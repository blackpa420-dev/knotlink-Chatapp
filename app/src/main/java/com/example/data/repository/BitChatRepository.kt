package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.BitChatDao
import com.example.data.local.BlockedUserEntity
import com.example.data.local.CachedProfileEntity
import com.example.data.local.SyncStateEntity
import com.example.data.local.ChatEntity
import com.example.data.local.ContactEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.MessageEntity
import com.example.data.local.PinnedMessageEntity
import com.example.data.local.ReactionEntity
import com.example.data.local.UserIdentityEntity
import com.example.data.local.UserSessionEntity
import com.example.data.supabase.SupabaseCallSession
import com.example.data.supabase.SupabaseMessage
import com.example.data.supabase.SupabaseProfile
import com.example.data.supabase.SupabaseRealtimeManager
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue

data class UserPrivacySettings(
    val profilePhotoVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val onlineStatusVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val lastSeenVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val whoCanMessageMe: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val readReceiptsEnabled: Boolean = true,
    val typingIndicatorEnabled: Boolean = true
) {
    fun toJson(): String {
        return org.json.JSONObject().apply {
            put("profilePhotoVisibility", profilePhotoVisibility)
            put("onlineStatusVisibility", onlineStatusVisibility)
            put("lastSeenVisibility", lastSeenVisibility)
            put("whoCanMessageMe", whoCanMessageMe)
            put("readReceiptsEnabled", readReceiptsEnabled)
            put("typingIndicatorEnabled", typingIndicatorEnabled)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): UserPrivacySettings {
            if (jsonStr.isNullOrBlank()) return UserPrivacySettings()
            return try {
                val obj = org.json.JSONObject(jsonStr)
                UserPrivacySettings(
                    profilePhotoVisibility = obj.optString("profilePhotoVisibility", "EVERYONE"),
                    onlineStatusVisibility = obj.optString("onlineStatusVisibility", "EVERYONE"),
                    lastSeenVisibility = obj.optString("lastSeenVisibility", "EVERYONE"),
                    whoCanMessageMe = obj.optString("whoCanMessageMe", "EVERYONE"),
                    readReceiptsEnabled = obj.optBoolean("readReceiptsEnabled", true),
                    typingIndicatorEnabled = obj.optBoolean("typingIndicatorEnabled", true)
                )
            } catch (e: Exception) {
                UserPrivacySettings()
            }
        }
    }
}

data class AbuseReport(
    val reportId: String,
    val reporterUid: String,
    val targetType: String, // USER, MESSAGE, GROUP
    val targetId: String,
    val reason: String,
    val details: String?,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

data class PublicUserProfile(
    val uid: String = "",
    val publicId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val profession: String = "🎓 Student",
    val mutualGroups: List<String> = emptyList(),
    val avatarType: String = "default"
)

class BitChatRepository(val dao: BitChatDao) {
    private val messagePushScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)


    private val historySyncMutex = kotlinx.coroutines.sync.Mutex()
    private val lastHistorySyncAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val HISTORY_SYNC_TTL_MS = 30_000L

    val userIdentity: Flow<UserIdentityEntity?> = dao.getUserIdentity()
    val allChats: Flow<List<ChatEntity>> = dao.getAllChats()
    val allContacts: Flow<List<ContactEntity>> = dao.getAllContacts()
    val allBlockedUsers: Flow<List<BlockedUserEntity>> = dao.getAllBlockedUsers()
    val allUserSessions: Flow<List<UserSessionEntity>> = dao.getAllUserSessions()
    val allCallLogs: Flow<List<com.example.data.local.CallLogEntity>> = dao.getAllCallLogs()

    suspend fun insertCallLog(callLog: com.example.data.local.CallLogEntity) {
        dao.insertCallLog(callLog)
    }

    suspend fun deleteCallLog(id: String) {
        dao.deleteCallLog(id)
    }

    suspend fun deleteCallLogsByIds(ids: List<String>) {
        dao.deleteCallLogsByIds(ids)
    }

    suspend fun clearCallLogs() {
        dao.clearCallLogs()
    }

    suspend fun updateCallLogDuration(id: String, duration: Int) {
        dao.updateCallLogDuration(id, duration)
    }

    val userPrivacySettings: Flow<UserPrivacySettings> = dao.getUserIdentity().map { identity ->
        UserPrivacySettings.fromJson(identity?.privacySettingsJson)
    }

    suspend fun updatePrivacySettings(settings: UserPrivacySettings) {
        val json = settings.toJson()
        dao.updatePrivacySettings(json)
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val uid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: currentIdentity?.email
        if (currentIdentity != null && !uid.isNullOrBlank() && currentIdentity.username.isNotBlank() && currentIdentity.fullName.isNotBlank()) {
            val prof = SupabaseProfile(
                id = uid,
                email = currentIdentity.email,
                username = currentIdentity.username,
                fullName = currentIdentity.fullName,
                privacySettings = json
            )
            SupabaseService.upsertProfile(prof)
        }
    }

    suspend fun blockUser(targetUid: String, username: String = "", displayName: String = "") {
        val entity = BlockedUserEntity(
            targetUid = targetUid,
            username = username,
            displayName = displayName,
            blockedAt = System.currentTimeMillis()
        )
        dao.insertBlockedUser(entity)
    }

    suspend fun unblockUser(targetUid: String) {
        dao.deleteBlockedUser(targetUid)
    }

    suspend fun isUserBlocked(targetUid: String): Boolean {
        val blocked = dao.getAllBlockedUsers().firstOrNull() ?: emptyList()
        return blocked.any { it.targetUid == targetUid }
    }

    suspend fun submitAbuseReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String?
    ): String {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val reporterUid = currentIdentity?.supabaseUid ?: currentIdentity?.email ?: "anonymous"
        val reportId = "rep_" + UUID.randomUUID().toString().take(8)
        Log.d("BitChatRepo", "Abuse report $reportId submitted against $targetId for $reason by $reporterUid")
        return reportId
    }

    suspend fun registerDeviceSession(
        deviceId: String,
        deviceName: String = "Android Device",
        appVersion: String = "1.0",
        fcmToken: String? = null
    ) {
        val session = UserSessionEntity(
            deviceId = deviceId,
            deviceName = deviceName,
            platform = "Android",
            appVersion = appVersion,
            lastActiveTimestamp = System.currentTimeMillis(),
            isCurrentDevice = true,
            isActive = true
        )
        dao.insertUserSessions(listOf(session))
    }

    suspend fun revokeDeviceSession(deviceId: String) {
        dao.deleteUserSession(deviceId)
    }

    suspend fun revokeAllOtherSessions(currentDeviceId: String) {
        val all = dao.getAllUserSessions().firstOrNull() ?: emptyList()
        for (session in all) {
            if (session.deviceId != currentDeviceId) {
                dao.deleteUserSession(session.deviceId)
            }
        }
    }

    suspend fun deleteAccount(reason: String?) {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        if (currentIdentity != null) {
            val uid = currentIdentity.supabaseUid.ifBlank { currentIdentity.email }
            if (uid.isNotBlank()) {
                SupabaseService.updateProfile(uid, mapOf("status" to "DELETED"))
            }
        }
        clearAllLocalData()
    }

    suspend fun deleteChat(chatId: String) {
        dao.deleteChatById(chatId)
    }

    suspend fun insertChats(chats: List<ChatEntity>) {
        dao.insertChats(chats)
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = dao.getMessagesForChat(chatId)

    suspend fun clearMessagesForChat(chatId: String) {
        dao.clearMessagesForChat(chatId)
    }

    private suspend fun getLocalProfile(identifier: String): SupabaseProfile? {
        val key = identifier.trim()
        if (key.isBlank()) return null
        val cached = when {
            key.matches(Regex("^[0-9a-fA-F-]{36}$")) -> dao.getCachedProfile(key)
            key.contains("@") -> dao.getCachedProfileByEmail(key.lowercase())
            else -> dao.getCachedProfileByUsername(key.removePrefix("@")) ?: dao.getCachedProfile(key)
        }
        return cached?.let {
            SupabaseProfile(
                id = it.uid,
                username = it.username,
                fullName = it.fullName,
                avatarUrl = it.avatarUrl,
                bio = it.bio,
                profession = it.profession,
                email = it.email,
                lastSeen = it.lastSeen,
                isOnline = it.isOnline
            )
        }
    }

    private suspend fun cacheProfileLocally(profile: SupabaseProfile?) {
        if (profile == null || profile.id.isBlank()) return
        dao.upsertCachedProfile(
            CachedProfileEntity(
                uid = profile.id,
                username = profile.username,
                fullName = profile.fullName,
                avatarUrl = profile.avatarUrl,
                bio = profile.bio,
                profession = profile.profession,
                email = profile.email,
                lastSeen = profile.lastSeen,
                isOnline = profile.isOnline,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveUserIdentity(identity: UserIdentityEntity) {
        val uid = identity.supabaseUid.trim()
        require(isValidUuid(uid)) {
            "A real authenticated Supabase user ID is required for local identity"
        }
        val cleanEmail = identity.email.trim().lowercase()
        val cleanSecEmail = identity.secondaryEmail.trim().lowercase()
        val safeIdentity = identity.copy(
            supabaseUid = uid,
            email = cleanEmail,
            secondaryEmail = cleanSecEmail
        )
        dao.saveUserIdentity(safeIdentity)

        // Only create/update profile in Supabase remote database if profile setup is complete (fullName and username entered)
        if (safeIdentity.username.isNotBlank() && safeIdentity.fullName.isNotBlank()) {
            val prof = SupabaseProfile(
                id = uid,
                email = safeIdentity.email,
                secondaryEmail = safeIdentity.secondaryEmail,
                username = safeIdentity.username,
                fullName = safeIdentity.fullName,
                avatarUrl = safeIdentity.avatarPath ?: "",
                profession = safeIdentity.profession,
                birthDate = safeIdentity.birthDate,
                isVerified = safeIdentity.isVerified,
                privacySettings = safeIdentity.privacySettingsJson
            )
            val res = SupabaseService.upsertProfile(prof)
            if (res.isSuccess) cacheProfileLocally(res.getOrNull() ?: prof)
            if (res.isFailure) {
                Log.e("BitChatRepository", "saveUserIdentity upsertProfile failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    private fun isValidUuid(str: String): Boolean {
        return try {
            UUID.fromString(str)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recordLoginSession() {
        val current = dao.getUserIdentity().firstOrNull()
        if (current != null) {
            val updated = current.copy(loginTimestamp = System.currentTimeMillis())
            dao.saveUserIdentity(updated)
        }
    }

    suspend fun clearLoginSession() {
        val current = dao.getUserIdentity().firstOrNull()
        if (current != null) {
            val updated = current.copy(loginTimestamp = 0L)
            dao.saveUserIdentity(updated)
        }
    }

    suspend fun clearAllLocalData() {
        dao.clearUserIdentity()
        dao.clearAllChats()
        dao.clearAllContacts()
        dao.clearAllMessages()
        dao.clearBlockedUsers()
        dao.clearUserSessions()
        dao.clearCachedProfiles()
        dao.clearSyncStates()
    }

    /**
     * Establishes a strict local-account boundary.
     *
     * The authenticated Supabase UID is authoritative. If a different account
     * was previously stored on this device, discard account-scoped local state
     * before importing the new account's server history.
     */
    suspend fun prepareForAuthenticatedUser(authUid: String): Boolean {
        val canonicalUid = authUid.trim()
        if (!isValidUuid(canonicalUid)) return false

        val sessionUid = SupabaseService.getAuthenticatedUserId() ?: return false
        if (!sessionUid.equals(canonicalUid, ignoreCase = true)) return false

        val existing = dao.getUserIdentity().firstOrNull()
        val existingUid = existing?.supabaseUid?.trim().orEmpty()
        if (existingUid.isNotBlank() && !existingUid.equals(canonicalUid, ignoreCase = true)) {
            clearAllLocalData()
        }
        return true
    }


    suspend fun savePhoneNumber(phoneNumber: String) {
        val current = dao.getUserIdentity().firstOrNull() ?: UserIdentityEntity(phoneNumber = phoneNumber)
        val updated = current.copy(phoneNumber = phoneNumber)
        dao.saveUserIdentity(updated)
    }

    suspend fun saveEmail(email: String) {
        val clean = email.trim().lowercase()
        val current = dao.getUserIdentity().firstOrNull() ?: UserIdentityEntity(email = clean)
        val updated = current.copy(email = clean, isEmailVerified = false)
        dao.saveUserIdentity(updated)
    }

    suspend fun saveUsernameAndVerify(username: String, profileType: String) {
        val current = dao.getUserIdentity().firstOrNull() ?: UserIdentityEntity()
        val uid = SupabaseService.getAuthenticatedUserId()
            ?: throw IllegalStateException("Cannot save username without an authenticated Supabase session")
        val updated = current.copy(
            supabaseUid = uid,
            username = username,
            profileType = profileType,
            isVerified = true,
            loginTimestamp = System.currentTimeMillis()
        )
        dao.saveUserIdentity(updated)

        val prof = SupabaseProfile(
            id = uid,
            email = updated.email,
            secondaryEmail = updated.secondaryEmail,
            username = username,
            fullName = updated.fullName,
            avatarUrl = updated.avatarPath ?: "",
            profession = updated.profession,
            isVerified = true
        )
        val res = SupabaseService.upsertProfile(prof)
        if (res.isSuccess) cacheProfileLocally(res.getOrNull() ?: prof)
        if (res.isFailure) {
            Log.e("BitChatRepository", "saveUsernameAndVerify upsertProfile failed: ${res.exceptionOrNull()?.message}")
        }
    }

    suspend fun updateUserProfile(
        fullName: String, avatarPath: String?, profession: String = "🎓 Student",
        email: String = "", secondaryEmail: String = "", isEmailVerified: Boolean = false,
        birthDate: String = ""
    ): Result<Unit> {
        return try {
            val current = dao.getUserIdentity().firstOrNull() ?: UserIdentityEntity()
            val uid = SupabaseService.getAuthenticatedUserId()
                ?: return Result.failure(IllegalStateException("Cannot update profile without an authenticated Supabase session"))
            val updated = current.copy(
                supabaseUid = uid, fullName = fullName, avatarPath = avatarPath, profession = profession,
                email = if (email.isNotBlank()) email else current.email,
                secondaryEmail = if (secondaryEmail.isNotBlank()) secondaryEmail else current.secondaryEmail,
                isEmailVerified = isEmailVerified || current.isEmailVerified, birthDate = birthDate
            )
            val prof = SupabaseProfile(
                id = uid, email = updated.email, secondaryEmail = updated.secondaryEmail,
                username = updated.username, fullName = fullName, avatarUrl = avatarPath ?: "",
                profession = profession, birthDate = birthDate, isVerified = updated.isVerified
            )
            val res = SupabaseService.upsertProfile(prof)
            if (res.isFailure) {
                val error = res.exceptionOrNull() ?: Exception("Failed to save profile")
                Log.e("BitChatRepository", "updateUserProfile upsertProfile failed: " + error.message, error)
                return Result.failure(error)
            }
            val savedProfile = res.getOrNull() ?: prof
            dao.saveUserIdentity(updated.copy(avatarPath = savedProfile.avatarUrl ?: avatarPath))
            cacheProfileLocally(savedProfile)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BitChatRepository", "updateUserProfile failed", e)
            Result.failure(e)
        }
    }

    suspend fun resolveReceiverSupabaseUuid(
        candidate: String,
        chatId: String,
        existingChat: ChatEntity?,
        currentUid: String,
        myUsername: String,
        myEmail: String
    ): String? {
        // Direct messaging is UUID-only. Display/search identifiers are never
        // promoted to a message recipient because doing so can route to the
        // wrong account when stale/local data is present.
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        val matchingContact = dao.getAllContactsList().firstOrNull { contact ->
            contact.id.equals(candidate, ignoreCase = true) ||
                contact.id.equals(chatId, ignoreCase = true)
        }
        val candidates = mutableListOf<String>()
        fun addUuid(value: String?) {
            val v = value?.trim().orEmpty()
            if (v.matches(uuidRegex) && !v.equals(currentUid.trim(), ignoreCase = true)) candidates.add(v)
        }
        addUuid(candidate)
        addUuid(chatId)
        existingChat?.participantUids
            ?.split(",")
            ?.forEach { addUuid(it) }
        matchingContact?.let { addUuid(it.id) }

        // Prefer a locally cached UUID, but the UUID itself remains authoritative.
        for (uid in candidates) {
            val cached = getLocalProfile(uid)
            if (cached?.id?.matches(uuidRegex) == true) return cached.id
            return uid
        }
        return null
    }

    suspend fun sendMessage(
        chatId: String,
        senderName: String,
        text: String,
        isFromUser: Boolean = true,
        isRead: Boolean = false,
        existingClientMessageId: String? = null,
        replyToMessageId: String? = null,
        replySnippet: String? = null,
        replySenderName: String? = null,
        isForwarded: Boolean = false,
        forwardedFromMessageId: String? = null,
        messageType: String = "TEXT",
        systemEventType: String? = null,
        mentionedUids: String? = null
    ): MessageEntity {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val clientMsgId = existingClientMessageId ?: UUID.randomUUID().toString()
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.trim().orEmpty()
        if (!isValidUuid(currentUid)) {
            val failed = MessageEntity(
                chatId = chatId,
                senderName = senderName,
                text = text,
                timestampString = currentTime,
                isFromUser = isFromUser,
                isRead = false,
                senderUid = "",
                receiverUid = "",
                clientMessageId = clientMsgId,
                syncStatus = "FAILED",
                deliveryState = "FAILED",
                timestamp = System.currentTimeMillis(),
                replyToMessageId = replyToMessageId,
                replySnippet = replySnippet,
                replySenderName = replySenderName,
                isForwarded = isForwarded,
                forwardedFromMessageId = forwardedFromMessageId,
                messageType = messageType,
                systemEventType = systemEventType,
                mentionedUids = mentionedUids
            )
            dao.insertMessage(failed)
            return failed
        }
        // Do not throw here. A stale local identity can exist after an account/database reset;
        // the remote write will be rejected safely and the message will be marked FAILED.
        val mySenderName = if (isFromUser) {
            currentIdentity?.fullName?.ifBlank { currentIdentity.username.ifBlank { senderName } } ?: senderName
        } else {
            senderName
        }

        var existingChat = dao.getChatById(chatId)
        if (existingChat == null) {
            existingChat = dao.getAllChatsList().find { 
                it.id.equals(chatId, ignoreCase = true) || 
                it.name.equals(chatId, ignoreCase = true) || 
                it.participantUids.split(",").any { uid -> uid.trim().equals(chatId, ignoreCase = true) } 
            }
        }
        val myUsername = currentIdentity?.username ?: ""
        val myEmail = currentIdentity?.email ?: ""
        val participants = existingChat?.participantUids?.split(",")?.map { it.trim() } ?: emptyList()
        var otherParticipant = participants.firstOrNull { 
            it.isNotBlank() && 
            !it.equals(currentUid, ignoreCase = true) && 
            !it.equals(myUsername, ignoreCase = true) && 
            !it.equals(myEmail, ignoreCase = true) 
        } ?: ""

        if (otherParticipant.isBlank()) {
            val cleanCandidate = chatId.removePrefix("chat_").removePrefix("user_").trim()
            if (!chatId.startsWith("group_") && cleanCandidate.isNotBlank() && cleanCandidate != "global" && cleanCandidate != "bitassistant") {
                val contact = dao.getAllContactsList().find { 
                    it.id.equals(cleanCandidate, ignoreCase = true) || 
                    it.id.equals(chatId, ignoreCase = true) || 
                    (existingChat != null && it.name.equals(existingChat.name, ignoreCase = true)) 
                }
                if (contact != null) {
                    otherParticipant = contact.id
                } else {
                    try {
                        val prof = SupabaseService.getProfileByUsername(cleanCandidate).getOrNull()
                            ?: SupabaseService.getProfile(cleanCandidate).getOrNull()
                        if (prof != null) {
                            otherParticipant = prof.id.ifBlank { prof.username }
                        }
                    } catch (_: Exception) {}
                    if (otherParticipant.isBlank()) {
                        otherParticipant = cleanCandidate
                    }
                }
            }
        }

        // Proactively resolve actual Supabase profile UUID for otherParticipant
        val resolvedReceiverUuid = resolveReceiverSupabaseUuid(
            candidate = otherParticipant,
            chatId = chatId,
            existingChat = existingChat,
            currentUid = currentUid,
            myUsername = myUsername,
            myEmail = myEmail
        )
        if (!resolvedReceiverUuid.isNullOrBlank()) {
            otherParticipant = resolvedReceiverUuid
        }

        if (isFromUser && existingChat != null && existingChat.chatType != "GROUP") {
            if (otherParticipant.isNotBlank() && isUserBlocked(otherParticipant)) {
                throw IllegalStateException("Cannot send message: This contact is blocked.")
            }
        }

        // For direct chats, the server-generated UUID chat is authoritative.
        // Never write a new message against an old hash/username-derived chat id.
        val canonicalDirectChatId = if (
            isFromUser &&
            !chatId.startsWith("group_") &&
            otherParticipant.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
        ) {
            SupabaseService.getOrCreateDirectChat(otherParticipant).getOrNull()
        } else null
        val targetChatId = canonicalDirectChatId ?: existingChat?.id ?: chatId

        val message = MessageEntity(
            chatId = targetChatId,
            senderName = mySenderName,
            text = text,
            timestampString = currentTime,
            isFromUser = isFromUser,
            isRead = isRead,
            senderUid = if (isFromUser || messageType == "SYSTEM_EVENT") currentUid else "",
            receiverUid = otherParticipant,
            clientMessageId = clientMsgId,
            syncStatus = "PENDING",
            deliveryState = "SENDING",
            timestamp = System.currentTimeMillis(),
            replyToMessageId = replyToMessageId,
            replySnippet = replySnippet,
            replySenderName = replySenderName,
            isForwarded = isForwarded,
            forwardedFromMessageId = forwardedFromMessageId,
            messageType = messageType,
            systemEventType = systemEventType,
            mentionedUids = mentionedUids
        )
        // Idempotency boundary: a retry with the same clientMessageId must reuse the
        // existing optimistic Room row instead of creating a second local message.
        val existingByClientId = dao.getMessageByServerOrClientId(null, clientMsgId)
        val localRowId = if (existingByClientId != null) {
            val merged = message.copy(
                id = existingByClientId.id,
                serverMessageId = existingByClientId.serverMessageId,
                serverTimestamp = existingByClientId.serverTimestamp,
                syncStatus = existingByClientId.syncStatus,
                deliveryState = existingByClientId.deliveryState
            )
            dao.insertMessage(merged)
            existingByClientId.id
        } else {
            dao.insertMessage(message)
        }

        val previewText = if (messageType == "SYSTEM_EVENT") text else if (isForwarded) "↪️ Forwarded: $text" else text
        if (existingChat != null) {
            val currentUids = existingChat.participantUids.split(",").map { it.trim() }.toMutableList()
            if (otherParticipant.isNotBlank() && !currentUids.contains(otherParticipant)) {
                currentUids.add(otherParticipant)
            }
            val updatedChat = existingChat.copy(
                lastMessage = previewText,
                timeString = currentTime,
                lastUpdated = System.currentTimeMillis(),
                participantUids = currentUids.filter { it.isNotBlank() }.distinct().joinToString(",")
            )
            dao.insertChats(listOf(updatedChat))
        } else {
            // Auto-create ChatEntity so new messages show up in the inbox & chat list
            val allContacts = dao.getAllContactsList()
            val contact = allContacts.find { 
                it.id.equals(chatId, ignoreCase = true) || 
                it.name.equals(chatId, ignoreCase = true) 
            }
            val formattedName = chatId.replace("chat_", "").replace("user_", "").replace("_", " ")
                .split(" ").joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
            val chatName = contact?.name ?: if (otherParticipant.isNotBlank() && !otherParticipant.startsWith("user_") && !otherParticipant.startsWith("chat_")) otherParticipant else if (formattedName.isNotBlank()) formattedName else "Chat"
            val avatar = contact?.avatarType ?: "default"
            val newChat = com.example.data.local.ChatEntity(
                id = targetChatId,
                name = chatName,
                lastMessage = previewText,
                timeString = currentTime,
                unreadCount = 0,
                isOnline = contact?.isOnline ?: false,
                category = "Personal",
                avatarType = avatar,
                lastUpdated = System.currentTimeMillis(),
                participantUids = if (otherParticipant.isNotBlank()) "$currentUid,$otherParticipant" else "$currentUid,$targetChatId",
                chatType = "DIRECT"
            )
            dao.insertChats(listOf(newChat))
        }

        try {
            val serverTs = System.currentTimeMillis()

            // Keep the optimistic row PENDING until the authoritative server write succeeds.
            // A network/RLS failure must never appear as a successfully sent message.
            val pendingMessage = message.copy(
                id = localRowId,
                serverMessageId = "",
                serverTimestamp = 0L,
                syncStatus = "PENDING",
                deliveryState = "SENDING"
            )
            dao.insertMessage(pendingMessage)

            // Send to Supabase RPC; the server creates the authoritative UUID message id.

            val remoteText = if (!replySnippet.isNullOrBlank() || !replySenderName.isNullOrBlank()) {
                val encSender = java.net.URLEncoder.encode(replySenderName ?: "User", "UTF-8")
                val encSnippet = java.net.URLEncoder.encode(replySnippet ?: "", "UTF-8")
                "[REPLY_QUOTE:$encSender|$encSnippet]$text"
            } else {
                text
            }

            val supaMsg = SupabaseMessage(
                id = "",
                chatId = targetChatId,
                senderId = currentUid,
                senderName = mySenderName,
                receiverId = otherParticipant,
                text = remoteText,
                timestamp = serverTs,
                isRead = false,
                replyToId = replyToMessageId,
                messageType = messageType,
                isForwarded = isForwarded,
                mediaUrl = if (messageType != "TEXT") text else null,
                clientMsgId = clientMsgId
            )

            // Persist remotely first. Realtime broadcast is only emitted after the
            // authoritative database write succeeds, preventing phantom delivery.
            val sendResult = SupabaseService.sendMessage(supaMsg).getOrNull()
            if (sendResult == null) {
                // Never throw from the message-send coroutine: a server/RLS/auth failure
                // must become a failed local message, not an application crash.
                val failedMessage = message.copy(
                    id = localRowId,
                    syncStatus = "FAILED",
                    deliveryState = "FAILED"
                )
                dao.insertMessage(failedMessage)
                return failedMessage
            }
            val finalServerId = sendResult.id.trim()
            if (finalServerId.isBlank()) {
                val failedMessage = message.copy(
                    id = localRowId,
                    syncStatus = "FAILED",
                    deliveryState = "FAILED"
                )
                dao.insertMessage(failedMessage)
                return failedMessage
            }

            val syncedMessage = message.copy(
                id = localRowId,
                serverMessageId = finalServerId,
                serverTimestamp = sendResult.timestamp,
                syncStatus = "SYNCED",
                deliveryState = "SENT"
            )
            dao.insertMessage(syncedMessage)

            // The database write is the authoritative event.
            // Supabase Realtime postgres_changes delivers it to the recipient.
            // Do not send a second custom broadcast: that would create duplicate messages.
            if (finalServerId.isNotBlank()) {
                dao.updateMessageServerId(localRowId, clientMsgId, finalServerId)
            }

            // Trigger FCM High Priority Push Notification to recipient
            if (isFromUser && (otherParticipant.isNotBlank() || chatId.isNotBlank())) {
                val fcmMsgType = when {
                    text.contains("[AUDIO_BASE64|") || text.contains("[AUDIO_FILE|") || text.contains("[VIEW_ONCE_AUDIO_") -> "AUDIO"
                    text.contains("[IMAGE_BASE64|") || text.contains("[IMAGE_URL|") || text.contains("[IMAGE_ATTACHMENT|") || text.contains("[IMAGE_ALBUM|") -> "IMAGE"
                    text.contains("[VIDEO_BASE64|") || text.contains("[VIDEO_FILE|") -> "VIDEO"
                    text.contains("[DOCUMENT_FILE|") -> "DOCUMENT"
                    else -> "TEXT"
                }

                val finalRecipient = if (otherParticipant.isNotBlank()) {
                    otherParticipant
                } else {
                    resolveReceiverSupabaseUuid(
                        candidate = otherParticipant,
                        chatId = chatId,
                        existingChat = existingChat,
                        currentUid = currentUid,
                        myUsername = myUsername,
                        myEmail = myEmail
                    ) ?: otherParticipant
                }

                if (finalRecipient.isNotBlank()) {
                    // Repository-owned scope keeps the FCM request alive instead of using GlobalScope.
                    messagePushScope.launch {
                        try {
                            Log.d("BitChatRepository", "Sending message FCM push to recipient: $finalRecipient (chatId: $targetChatId)")
                            com.example.util.FcmPushSender.sendPushToUser(
                                targetUserIdOrName = finalRecipient,
                                type = "message",
                                title = "New Message from $mySenderName",
                                body = com.example.util.NotificationHelper.formatCleanPreviewText(text),
                                senderName = mySenderName,
                                chatId = targetChatId,
                                senderAvatar = currentIdentity?.avatarPath
                                    ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                                    ?: dao.getCachedProfile(currentUid)?.avatarUrl,
                                senderId = currentUid,
                                serverMessageId = finalServerId,
                                messageType = fcmMsgType
                            )
                        } catch (e: Throwable) {
                            Log.w("BitChatRepository", "FCM push error: ${e.message}")
                        }
                    }
                } else {
                    Log.w("BitChatRepository", "FCM push skipped: Could not resolve valid recipient UUID for chatId '$chatId'")
                }
            }

            val finalSyncedMessage = syncedMessage.copy(
                serverMessageId = finalServerId
            )
            dao.insertMessage(finalSyncedMessage)

            return finalSyncedMessage
        } catch (e: Exception) {
            val failedMessage = message.copy(
                id = localRowId,
                syncStatus = "FAILED",
                deliveryState = "FAILED"
            )
            dao.insertMessage(failedMessage)
            throw e
        }
    }

    suspend fun markUserMessagesAsRead(chatId: String) {
        val unreadServerIds = try {
            dao.getMessagesForChat(chatId).firstOrNull().orEmpty()
                .asSequence()
                .filter { !it.isFromUser && it.deliveryState != "READ" && !it.serverMessageId.isNullOrBlank() }
                .mapNotNull { it.serverMessageId }
                .distinct()
                .toList()
        } catch (_: Throwable) {
            emptyList()
        }

        if (unreadServerIds.isNotEmpty()) {
            markMessagesAsRead(chatId, unreadServerIds)
        } else {
            dao.markUserMessagesAsRead(chatId)
        }
        dao.resetChatUnreadCount(chatId)
    }

    suspend fun markMessagesAsRead(chatId: String, messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        dao.markMessagesAsReadByServerIds(messageIds)
        val result = SupabaseService.markMessagesAsRead(chatId, messageIds)
        val reader = dao.getUserIdentity().firstOrNull()
        val readerUid = reader?.supabaseUid?.trim().orEmpty()
        if (result.isSuccess && readerUid.isNotBlank()) {
            messageIds.forEach { serverId ->
                val local = dao.findMessageByServerId(serverId)
                if (local != null && local.senderUid.isNotBlank() && local.senderUid != readerUid) {
                    SupabaseRealtimeManager.broadcastMessageMutation(
                        SupabaseMessage(
                            id = serverId,
                            chatId = chatId,
                            senderId = readerUid,
                            senderName = reader?.fullName?.ifBlank { reader.username }.orEmpty(),
                            receiverId = local.senderUid,
                            text = local.text,
                            timestamp = System.currentTimeMillis(),
                            status = "READ",
                            isRead = true,
                            messageType = local.messageType,
                            isPinned = local.isPinned,
                            clientMsgId = local.clientMessageId
                        ),
                        mutation = "READ"
                    )
                }
            }
        }
    }

    suspend fun retryMessage(message: MessageEntity): MessageEntity {
        return sendMessage(
            chatId = message.chatId,
            senderName = message.senderName,
            text = message.text,
            isFromUser = message.isFromUser,
            isRead = message.isRead,
            existingClientMessageId = message.clientMessageId
        )
    }

    suspend fun toggleMessageReadStatus(messageId: Long, currentIsRead: Boolean) {
        dao.updateMessageReadStatus(messageId, !currentIsRead)
    }

    suspend fun syncCallHistory(myUid: String, myUsername: String = "") {
        if (myUid.isBlank()) return
        try {
            val identity = dao.getUserIdentity().firstOrNull()
            val calls = SupabaseService.getUserCallHistory(myUid).getOrNull().orEmpty()

            for (call in calls) {
                val incoming = call.receiverId.equals(myUid, ignoreCase = true)

                val otherId = if (incoming) call.callerId else call.receiverId
                val existingChat = dao.getAllChatsList().firstOrNull { chat ->
                    chat.participantUids.split(",").map { it.trim() }.any { it.equals(otherId, ignoreCase = true) }
                }

                val contactName = if (incoming) {
                    call.callerName.ifBlank { existingChat?.name ?: "Caller" }
                } else {
                    existingChat?.name ?: "Call"
                }

                val ts = call.startedAt
                val duration = if (call.connectedAt != null && call.endedAt != null) {
                    ((call.endedAt - call.connectedAt).coerceAtLeast(0L) / 1000L).toInt()
                } else 0

                val logId = "remote_call_" + call.id
                dao.insertCallLog(
                    com.example.data.local.CallLogEntity(
                        id = logId,
                        contactId = otherId.ifBlank { call.id },
                        contactName = contactName,
                        callType = if (call.callType.equals("video", true)) "VIDEO" else "AUDIO",
                        direction = if (incoming) "INCOMING" else "OUTGOING",
                        timestampMillis = ts,
                        timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts)),
                        durationSeconds = duration,
                        avatarType = call.callerAvatar?.takeIf { it.isNotBlank() } ?: existingChat?.avatarType ?: "default"
                    )
                )
            }
        } catch (e: Throwable) {
            android.util.Log.w("BitChatRepo", "Call history sync error: " + e.message)
        }
    }

    suspend fun prepopulateIfEmpty() {
        val chats = dao.getAllChats().firstOrNull() ?: emptyList()
        val assistantChat = chats.find { it.id == "bitassistant" }
        val welcomeText = "Hi ! This is your personal safe assistant , you can share important data or store anything ."
        if (assistantChat == null) {
            val newAssistantChat = ChatEntity(
                id = "bitassistant",
                name = "KnotLink Assistant",
                lastMessage = welcomeText,
                timeString = "Just now",
                unreadCount = 0,
                isOnline = false,
                category = "Personal",
                avatarType = "assistant"
            )
            dao.insertChats(listOf(newAssistantChat))

            val welcomeMsg = MessageEntity(
                chatId = "bitassistant",
                senderName = "KnotLink Assistant",
                text = welcomeText,
                timestampString = "Just now",
                isFromUser = false
            )
            dao.insertMessage(welcomeMsg)
        } else if (assistantChat.name != "KnotLink Assistant" || assistantChat.lastMessage.contains("BitChat", ignoreCase = true) || assistantChat.isOnline) {
            val updatedAssistant = assistantChat.copy(
                name = "KnotLink Assistant",
                lastMessage = welcomeText,
                isOnline = false,
                avatarType = "assistant"
            )
            dao.insertChats(listOf(updatedAssistant))

            val msgs = dao.getMessagesForChat("bitassistant").firstOrNull() ?: emptyList()
            if (msgs.isEmpty() || msgs.any { it.text.contains("BitChat", ignoreCase = true) }) {
                dao.clearMessagesForChat("bitassistant")
                val welcomeMsg = MessageEntity(
                    chatId = "bitassistant",
                    senderName = "KnotLink Assistant",
                    text = welcomeText,
                    timestampString = "Just now",
                    isFromUser = false
                )
                dao.insertMessage(welcomeMsg)
            }
        }
    }

    /**
     * Canonical public-user search used by BOTH manual search and QR scanning.
     * QR passes exactMatch=true so the scanned identifier must resolve to the
     * same real profile that this search flow would return manually.
     */
    suspend fun searchUsers(query: String, exactMatch: Boolean = false): List<PublicUserProfile> {
        val cleanQuery = query.trim().lowercase().removePrefix("@")
        if (cleanQuery.isEmpty()) return emptyList()

        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: ""
        val blockedUids = (allBlockedUsers.firstOrNull() ?: emptyList()).map { it.targetUid }.toSet()

        // ONE canonical remote profile-search system for both manual search and QR.
        // There is intentionally no local-contact resolver here.
        val supaResults = try {
            val remote = SupabaseService.searchProfiles(cleanQuery).getOrNull().orEmpty()
            // QR payloads carry the canonical Auth UUID. Keep a direct profile
            // lookup as a deterministic fallback so a UUID QR never depends on
            // the broader search query parser.
            if (remote.isEmpty() && cleanQuery.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
                SupabaseService.getProfile(cleanQuery).getOrNull()?.let { listOf(it) } ?: emptyList()
            } else {
                remote
            }
        } catch (e: Exception) {
            Log.d("BitChatRepo", "Canonical user search error: ${e.message}")
            emptyList()
        }

        val results = supaResults
            .asSequence()
            .filter { it.id.isNotBlank() }
            .filterNot { it.id.equals(currentUid, ignoreCase = true) || blockedUids.contains(it.id) }
            .map { p ->
                PublicUserProfile(
                    uid = p.id,
                    publicId = p.publicId.ifBlank { p.username.ifBlank { p.id } },
                    username = p.username,
                    displayName = p.fullName.ifBlank { p.username },
                    avatarUrl = p.avatarUrl,
                    bio = p.bio.ifBlank { "Verified KnotLink User" },
                    profession = p.profession.ifBlank { "✨ KnotLink Member" },
                    mutualGroups = listOf("KnotLink Network"),
                    avatarType = p.avatarUrl?.ifBlank { "default" } ?: "default"
                )
            }
            .distinctBy { it.uid }
            .toList()

        if (!exactMatch) return results

        val normalized = cleanQuery.removePrefix("@").lowercase()
        val normalizedBase = normalized.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")

        return results.filter { profile ->
            val username = profile.username.trim().lowercase().removePrefix("@")
            val usernameBase = username.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")
            val publicId = profile.publicId.trim().lowercase().removePrefix("@")
            val publicIdBase = publicId.removeSuffix(".link").removeSuffix(".bit").removeSuffix(".chat")

            profile.uid.equals(normalized, ignoreCase = true) ||
                publicId == normalized ||
                publicIdBase == normalizedBase ||
                username == normalized ||
                usernameBase == normalizedBase
        }
    }

    suspend fun findOrCreateCanonicalChat(
        chatId: String,
        opponentUid: String,
        opponentName: String,
        opponentAvatar: String
    ): ChatEntity {
        val allChats = dao.getAllChatsList()
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val myUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"

        val cleanOpponentUid = opponentUid.trim()
        val cleanOpponentName = opponentName.trim()

        // V2 direct chats are server-generated UUIDs. Resolve the canonical chat once
        // when opening the conversation; never derive chat identity from username/hash.
        val resolvedCanonicalChatId = if (
            cleanOpponentUid.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
        ) {
            SupabaseService.getOrCreateDirectChat(cleanOpponentUid).getOrNull() ?: chatId
        } else {
            chatId
        }

        // 1. Direct ID match
        val byId = allChats.find { it.id == resolvedCanonicalChatId }
        if (byId != null) {
            val isCurrentNameUgly = byId.name.isBlank() || 
                byId.name == "Contact" || 
                byId.name == "User" || 
                byId.name == byId.id || 
                byId.name == cleanOpponentUid || 
                byId.name.contains("-") || 
                (byId.name.contains("_") && !byId.name.contains(" "))
            val hasBetterName = cleanOpponentName.isNotBlank() && cleanOpponentName != cleanOpponentUid && cleanOpponentName != "Contact" && cleanOpponentName != "User"
            val hasBetterAvatar = opponentAvatar.isNotBlank() && opponentAvatar != "default" && (byId.avatarType.isBlank() || byId.avatarType == "default")

            if ((isCurrentNameUgly && hasBetterName) || hasBetterAvatar) {
                val updated = byId.copy(
                    name = if (hasBetterName) cleanOpponentName else byId.name,
                    avatarType = if (hasBetterAvatar) opponentAvatar else byId.avatarType
                )
                dao.insertChats(listOf(updated))
                return updated
            }
            return byId
        }

        // 2. Direct participant or name match for 1-on-1 chats
        if (!chatId.startsWith("group_")) {
            val byParticipant = allChats.find { chat ->
                chat.chatType != "GROUP" && chat.category != "Group" && (
                    (cleanOpponentUid.isNotBlank() && chat.participantUids.split(",").map { it.trim().lowercase() }.contains(cleanOpponentUid.lowercase())) ||
                    (cleanOpponentName.isNotBlank() && cleanOpponentName != "User" && cleanOpponentName != "Contact" && chat.name.equals(cleanOpponentName, ignoreCase = true))
                )
            }
            if (byParticipant != null) {
                val isCurrentNameUgly = byParticipant.name.isBlank() || 
                    byParticipant.name == "Contact" || 
                    byParticipant.name == "User" || 
                    byParticipant.name.contains("-") || 
                    byParticipant.name == cleanOpponentUid
                val hasBetterName = cleanOpponentName.isNotBlank() && cleanOpponentName != cleanOpponentUid && cleanOpponentName != "Contact" && cleanOpponentName != "User"
                val hasBetterAvatar = opponentAvatar.isNotBlank() && opponentAvatar != "default" && (byParticipant.avatarType.isBlank() || byParticipant.avatarType == "default")
                if ((isCurrentNameUgly && hasBetterName) || hasBetterAvatar) {
                    val updated = byParticipant.copy(
                        name = if (hasBetterName) cleanOpponentName else byParticipant.name,
                        avatarType = if (hasBetterAvatar) opponentAvatar else byParticipant.avatarType
                    )
                    dao.insertChats(listOf(updated))
                    return updated
                }
                return byParticipant
            }
        }

        // 3. Create new canonical ChatEntity
        val sortedUids = listOf(myUid, cleanOpponentUid).filter { it.isNotBlank() }.distinct().sorted()
        val newChat = ChatEntity(
            id = resolvedCanonicalChatId,
            name = opponentName.ifBlank { "Contact" },
            lastMessage = "",
            timeString = "Just now",
            unreadCount = 0,
            isOnline = true,
            category = "Personal",
            avatarType = opponentAvatar,
            lastUpdated = System.currentTimeMillis(),
            participantUids = sortedUids.joinToString(",")
        )
        dao.insertChats(listOf(newChat))
        return newChat
    }

    suspend fun deduplicateCopyChats() = withContext(Dispatchers.IO) {
        try {
            val allChats = dao.getAllChatsList()
            if (allChats.size <= 1) return@withContext

            val currentIdentity = dao.getUserIdentity().firstOrNull()
            val myUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
            val myUsername = currentIdentity?.username ?: ""

            val directChats = allChats.filter { it.chatType != "GROUP" && it.category != "Group" }

            val groupedByOpponent = directChats.groupBy { chat ->
                val parts = chat.participantUids.split(",").map { it.trim().lowercase() }.filter {
                    it.isNotBlank() && it != myUid.lowercase() && it != myUsername.lowercase() && it != "user_me"
                }
                val keyUid = parts.firstOrNull()
                if (!keyUid.isNullOrBlank()) keyUid else chat.name.trim().lowercase()
            }

            for ((opponentKey, copies) in groupedByOpponent) {
                if (opponentKey.isBlank() || copies.size <= 1) continue

                val sortedCopies = copies.sortedByDescending { it.lastUpdated }
                val keeper = sortedCopies.first()
                val duplicates = sortedCopies.drop(1)

                for (dup in duplicates) {
                    Log.i("BitChatRepo", "Merging duplicate copy chat ${dup.id} into canonical chat ${keeper.id} for opponent: $opponentKey")
                    dao.updateMessageChatId(dup.id, keeper.id)
                    dao.deleteChatById(dup.id)
                }
            }
        } catch (e: Exception) {
            Log.w("BitChatRepo", "deduplicateCopyChats error: ${e.message}")
        }
    }

    suspend fun handleIncomingMessage(supaMsg: SupabaseMessage, currentActiveChatId: String? = null): MessageEntity {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val myUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        val myUsername = currentIdentity?.username ?: ""
        val myEmail = currentIdentity?.email ?: ""
        val myCleanName = myUsername.trim().removePrefix("@").lowercase().removeSuffix(".link")

        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date(if (supaMsg.timestamp > 0L) supaMsg.timestamp else System.currentTimeMillis()))

        var msgText = supaMsg.text

        // Instant parsing for real-time and historical reaction updates
        if (msgText.startsWith("[REACTION:")) {
            try {
                val payload = msgText.substringAfter("[REACTION:").substringBefore("]")
                val parts = payload.split("|")
                val msgKey = parts.getOrNull(0) ?: ""
                val emoji = parts.getOrNull(1) ?: ""
                val act = parts.getOrNull(2) ?: "ADD"
                if (msgKey.isNotBlank() && emoji.isNotBlank()) {
                    if (act == "REMOVE") {
                        dao.deleteReaction(msgKey, supaMsg.senderId, emoji)
                    } else {
                        dao.insertReaction(
                            ReactionEntity(
                                messageId = msgKey,
                                chatId = supaMsg.chatId,
                                uid = supaMsg.senderId,
                                emoji = emoji,
                                createdAt = supaMsg.timestamp
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
            return MessageEntity(
                id = 0L,
                chatId = supaMsg.chatId,
                senderName = supaMsg.senderName,
                text = msgText,
                timestampString = "",
                isFromUser = false
            )
        }
        var replySnippet: String? = null
        var replySenderName: String? = null
        val replyToId: String? = supaMsg.replyToId?.ifBlank { supaMsg.replyToMessageId }

        if (msgText.startsWith("[REPLY_QUOTE:")) {
            try {
                val meta = msgText.substringAfter("[REPLY_QUOTE:").substringBefore("]")
                val encSender = meta.substringBefore("|")
                val encSnippet = meta.substringAfter("|")
                replySenderName = java.net.URLDecoder.decode(encSender, "UTF-8")
                replySnippet = java.net.URLDecoder.decode(encSnippet, "UTF-8")
                msgText = msgText.substringAfter("]")
            } catch (_: Exception) {}
        }

        // 1. Check if message was sent BY ME
        // Sender identity is canonical Auth UUID only.
        val isFromMe = supaMsg.senderId.equals(myUid, ignoreCase = true)

        if (isFromMe) {
            val clientKey = supaMsg.clientMsgId ?: ""
            // Realtime/history reconciliation is strictly ID-based. Never match a
            // message by text/time: two legitimate identical messages are allowed.
            val existingLocal = dao.findMessageByServerId(supaMsg.id)
                ?: if (clientKey.isNotBlank()) dao.getMessageByServerOrClientId(supaMsg.id, clientKey) else null

            val updatedEntity = if (existingLocal != null) {
                existingLocal.copy(
                    serverMessageId = supaMsg.id,
                    syncStatus = "SYNCED",
                    deliveryState = when {
                        supaMsg.isRead || supaMsg.status.equals("READ", ignoreCase = true) -> "READ"
                        supaMsg.status.equals("DELIVERED", ignoreCase = true) -> "DELIVERED"
                        else -> "SENT"
                    },
                    isFromUser = true,
                    text = msgText,
                    isEdited = supaMsg.isEdited,
                    isDeletedForEveryone = supaMsg.isDeletedForEveryone,
                    isPinned = supaMsg.isPinned,
                    editedAt = if (supaMsg.isEdited) supaMsg.timestamp else existingLocal.editedAt,
                    pinnedAt = if (supaMsg.isPinned) supaMsg.timestamp else existingLocal.pinnedAt
                )
            } else {
                MessageEntity(
                        chatId = supaMsg.chatId,
                        senderName = "You",
                        text = msgText,
                        timestampString = timeStr,
                        isFromUser = true,
                        isRead = true,
                        senderUid = myUid,
                        receiverUid = supaMsg.receiverId,
                        serverMessageId = supaMsg.id,
                        clientMessageId = clientKey,
                        syncStatus = "SYNCED",
                        deliveryState = "SENT",
                        timestamp = supaMsg.timestamp,
                        messageType = supaMsg.messageType,
                        replyToMessageId = replyToId,
                        replySnippet = replySnippet,
                        replySenderName = replySenderName
                    )
                }
            dao.insertMessage(updatedEntity)

            var resolvedOpponentName = ""
            var resolvedOpponentAvatar = "default"
            try {
                val existingTarget = dao.getChatById(supaMsg.chatId)
                if (existingTarget != null && existingTarget.name.isNotBlank() && existingTarget.name != "Contact" && existingTarget.name != "User" && !existingTarget.name.contains("-")) {
                    resolvedOpponentName = existingTarget.name
                    resolvedOpponentAvatar = existingTarget.avatarType
                } else {
                    val p = getLocalProfile(supaMsg.receiverId)
                        ?: SupabaseService.getProfile(supaMsg.receiverId).getOrNull()
                        ?: SupabaseService.getProfileByUsername(supaMsg.receiverId).getOrNull()
                    if (p != null) {
                        cacheProfileLocally(p)
                        resolvedOpponentName = p.fullName.ifBlank { p.username }
                        if (!p.avatarUrl.isNullOrBlank()) resolvedOpponentAvatar = p.avatarUrl!!
                    }
                }
            } catch (_: Exception) {}

            val chat = findOrCreateCanonicalChat(
                chatId = supaMsg.chatId,
                opponentUid = supaMsg.receiverId,
                opponentName = resolvedOpponentName.ifBlank { supaMsg.receiverId },
                opponentAvatar = resolvedOpponentAvatar
            )
            val updatedChat = chat.copy(
                lastMessage = msgText,
                timeString = timeStr,
                lastUpdated = supaMsg.timestamp
            )
            dao.insertChats(listOf(updatedChat))
            return updatedEntity
        }

        // 2. Message sent by someone else -> STRICT filtering to prevent 3rd party chat leaks
        val isGroupOrBroadcast = supaMsg.receiverId.equals("all", ignoreCase = true) ||
            supaMsg.receiverId.equals("group", ignoreCase = true) ||
            supaMsg.chatId == "global" ||
            supaMsg.chatId.startsWith("group_") ||
            supaMsg.messageType == "SYSTEM_EVENT"

        val myFullName = currentIdentity?.fullName?.trim()?.lowercase() ?: ""

        // Direct message routing is UUID-only. Blank/username/email/name recipients are
        // legacy data and must never be treated as a valid recipient.
        val isDirectRecipient = supaMsg.receiverId.isNotBlank() &&
            supaMsg.receiverId.equals(myUid, ignoreCase = true)

        if (!isGroupOrBroadcast && !isDirectRecipient) {
            Log.d("BitChatRepo", "Ignoring 3rd party message not addressed to me: receiver=${supaMsg.receiverId}, sender=${supaMsg.senderId}")
            return MessageEntity(
                id = 0L,
                chatId = supaMsg.chatId,
                senderName = supaMsg.senderName,
                text = supaMsg.text,
                timestampString = "",
                isFromUser = false
            )
        }

        // Existing message check (to prevent duplicate insertions)
        val clientKey = supaMsg.clientMsgId ?: ""
        // Strict server/client ID idempotency. Content/time matching can collapse
        // two different messages with identical text and is therefore forbidden.
        val existingIncoming = dao.findMessageByServerId(supaMsg.id)
            ?: if (clientKey.isNotBlank()) dao.getMessageByServerOrClientId(supaMsg.id, clientKey) else null

        if (existingIncoming != null) {
            val remoteState = when {
                supaMsg.isRead || supaMsg.status.equals("READ", ignoreCase = true) -> "READ"
                supaMsg.status.equals("DELIVERED", ignoreCase = true) -> "DELIVERED"
                else -> "SENT"
            }
            val localState = existingIncoming.deliveryState.ifBlank { "SENT" }
            val monotonicState = when {
                localState == "READ" || remoteState == "READ" -> "READ"
                localState == "DELIVERED" || remoteState == "DELIVERED" -> "DELIVERED"
                else -> "SENT"
            }
            val updated = existingIncoming.copy(
                serverMessageId = supaMsg.id,
                syncStatus = "SYNCED",
                text = msgText,
                isRead = existingIncoming.isRead || supaMsg.isRead || monotonicState == "READ",
                deliveryState = monotonicState,
                isEdited = supaMsg.isEdited,
                isDeletedForEveryone = supaMsg.isDeletedForEveryone,
                isPinned = supaMsg.isPinned,
                editedAt = if (supaMsg.isEdited) supaMsg.timestamp else existingIncoming.editedAt,
                pinnedAt = if (supaMsg.isPinned) supaMsg.timestamp else existingIncoming.pinnedAt
            )
            dao.insertMessage(updated)

            // Even duplicate delivery events must be acknowledged server-side.
            // This makes delivery idempotent and allows recovery if the first
            // acknowledgement was lost during reconnect.
            if (supaMsg.id.isNotBlank() && !supaMsg.isRead) {
                try { SupabaseService.markMessageDelivered(supaMsg.id) } catch (_: Exception) {}
            }

            // Update local pinned_messages table
            if (supaMsg.isPinned) {
                dao.insertPinnedMessage(
                    PinnedMessageEntity(
                        chatId = supaMsg.chatId,
                        messageId = supaMsg.id,
                        pinnedByUid = supaMsg.senderId,
                        pinnedAt = supaMsg.timestamp
                    )
                )
            } else {
                dao.deletePinnedMessage(supaMsg.chatId, supaMsg.id)
            }

            return updated
        }

        // Fetch sender's latest profile name and avatar from Supabase
        var resolvedSenderName = supaMsg.senderName
        var resolvedSenderAvatar = "default"
        try {
            val prof = getLocalProfile(supaMsg.senderId)
                ?: getLocalProfile(supaMsg.senderName)
                ?: SupabaseService.getProfile(supaMsg.senderId).getOrNull()
                ?: SupabaseService.getProfileByUsername(supaMsg.senderName).getOrNull()
            if (prof != null) {
                cacheProfileLocally(prof)
                if (prof.fullName.isNotBlank()) resolvedSenderName = prof.fullName
                else if (prof.username.isNotBlank()) resolvedSenderName = prof.username
                val av = prof.avatarUrl
                if (!av.isNullOrBlank()) resolvedSenderAvatar = av
            }
        } catch (e: Exception) {
            Log.w("BitChatRepo", "Error fetching sender profile: ${e.message}")
        }

        val isChatOpen = currentActiveChatId == supaMsg.chatId

        // Reuse canonical chat or create new one safely
        val targetChat = findOrCreateCanonicalChat(
            chatId = supaMsg.chatId,
            opponentUid = supaMsg.senderId,
            opponentName = resolvedSenderName.ifBlank { supaMsg.senderName },
            opponentAvatar = resolvedSenderAvatar
        )

        val entity = MessageEntity(
            chatId = targetChat.id,
            senderName = resolvedSenderName.ifBlank { supaMsg.senderName },
            text = msgText,
            timestampString = timeStr,
            isFromUser = false,
            isRead = supaMsg.isRead,
            senderUid = supaMsg.senderId,
            receiverUid = myUid,
            serverMessageId = supaMsg.id,
            syncStatus = "SYNCED",
            deliveryState = if (supaMsg.isRead) "READ" else "DELIVERED",
            timestamp = supaMsg.timestamp,
            messageType = supaMsg.messageType,
            replyToMessageId = replyToId,
            replySnippet = replySnippet,
            replySenderName = replySenderName,
            isEdited = supaMsg.isEdited,
            isDeletedForEveryone = supaMsg.isDeletedForEveryone,
            isPinned = supaMsg.isPinned,
            editedAt = if (supaMsg.isEdited) supaMsg.timestamp else null,
            pinnedAt = if (supaMsg.isPinned) supaMsg.timestamp else null
        )
        val insertedRowId = dao.insertMessage(entity)
        val savedEntity = entity.copy(id = insertedRowId)

        // Sync local pinned_messages table for new message
        if (supaMsg.isPinned) {
            dao.insertPinnedMessage(
                PinnedMessageEntity(
                    chatId = supaMsg.chatId,
                    messageId = supaMsg.id,
                    pinnedByUid = supaMsg.senderId,
                    pinnedAt = supaMsg.timestamp
                )
            )
        }

        if (isChatOpen && supaMsg.id.isNotBlank() && !supaMsg.isRead) {
            try {
                markMessagesAsRead(supaMsg.chatId, listOf(supaMsg.id))
            } catch (_: Throwable) {
                // Near-bottom observer will retry if the realtime/server update fails.
            }
        }

        // Mark as delivered on server
        if (supaMsg.id.isNotBlank()) {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseService.markMessageDelivered(supaMsg.id)
                }
            } catch (_: Exception) {}
        }

        val updatedChat = targetChat.copy(
            name = if (resolvedSenderName.isNotBlank() && resolvedSenderName != "Contact" && resolvedSenderName != "User") resolvedSenderName else targetChat.name,
            avatarType = if (resolvedSenderAvatar.isNotBlank() && resolvedSenderAvatar != "default") resolvedSenderAvatar else targetChat.avatarType,
            lastMessage = msgText,
            timeString = timeStr,
            unreadCount = if (isChatOpen) 0 else targetChat.unreadCount + 1,
            lastUpdated = supaMsg.timestamp
        )
        dao.insertChats(listOf(updatedChat))

        return savedEntity
    }

    suspend fun syncAllChatHistory(requestedUid: String, myUsername: String = ""): Boolean = withContext(Dispatchers.IO) {
        var myUid: String? = null
        repeat(5) { attempt ->
            myUid = SupabaseService.getAuthenticatedUserId()
            if (!myUid.isNullOrBlank()) return@repeat
            if (attempt < 4) kotlinx.coroutines.delay(500L * (attempt + 1))
        }
        val authenticatedUid = myUid ?: return@withContext false
        if (!requestedUid.isBlank() && !requestedUid.equals(authenticatedUid, ignoreCase = true)) {
            Log.w("BitChatRepo", "Ignoring non-canonical history UID; using authenticated UID")
        }

        val now = System.currentTimeMillis()
        val last = lastHistorySyncAt[authenticatedUid] ?: 0L
        if (now - last < HISTORY_SYNC_TTL_MS) return@withContext true
        historySyncMutex.withLock {
            val lockedNow = System.currentTimeMillis()
            val lockedLast = lastHistorySyncAt[authenticatedUid] ?: 0L
            if (lockedNow - lockedLast < HISTORY_SYNC_TTL_MS) return@withLock true
            lastHistorySyncAt[authenticatedUid] = lockedNow
            try {
            // First run deduplication on existing copy chats
            deduplicateCopyChats()

            val currentIdentity = dao.getUserIdentity().firstOrNull()
            val myEmail = currentIdentity?.email?.trim()?.lowercase() ?: ""
            val myCleanName = myUsername.trim().removePrefix("@").lowercase().removeSuffix(".link")
            val fetchedMessages = mutableListOf<SupabaseMessage>()
            val syncKey = "user_history:$authenticatedUid"
            val previousSync = dao.getSyncState(syncKey)?.lastSyncedAt ?: 0L

            val historyUsername = myUsername.takeIf { it.isNotBlank() && it != myUid }
            val historyEmail = myEmail.takeIf { it.isNotBlank() && it != myUid && it != myUsername }

            // Bootstrap once, then recover only the incremental window.
            // Re-fetching the complete account history on every 30-second sync
            // defeats Room caching and unnecessarily consumes Supabase egress.
            val pageSize = 500
            if (previousSync <= 0L) {
                var offset = 0
                while (true) {
                    var pageRes = SupabaseService.fetchUserMessages(
                        userId = authenticatedUid,
                        username = historyUsername,
                        email = historyEmail,
                        limit = pageSize,
                        offset = offset
                    )
                    if (pageRes.isFailure) {
                        kotlinx.coroutines.delay(700L)
                        pageRes = SupabaseService.fetchUserMessages(
                            userId = authenticatedUid,
                            username = historyUsername,
                            email = historyEmail,
                            limit = pageSize,
                            offset = offset
                        )
                    }
                    if (pageRes.isFailure) {
                        Log.w("BitChatRepo", "Initial history page failed at offset $offset after retry: ${pageRes.exceptionOrNull()?.message}")
                        return@withLock false
                    }
                    val page = pageRes.getOrNull().orEmpty()
                    fetchedMessages.addAll(page)
                    if (page.size < pageSize) break
                    offset += pageSize
                }
            } else {
                var offset = 0
                while (true) {
                    var pageRes = SupabaseService.fetchUserMessagesSince(
                        userId = authenticatedUid,
                        username = historyUsername,
                        email = historyEmail,
                        sinceTimestamp = (previousSync - 120_000L).coerceAtLeast(0L),
                        limit = pageSize,
                        offset = offset
                    )
                    if (pageRes.isFailure) {
                        kotlinx.coroutines.delay(700L)
                        pageRes = SupabaseService.fetchUserMessagesSince(
                            userId = authenticatedUid,
                            username = historyUsername,
                            email = historyEmail,
                            sinceTimestamp = (previousSync - 120_000L).coerceAtLeast(0L),
                            limit = pageSize,
                            offset = offset
                        )
                    }
                    if (pageRes.isFailure) {
                        Log.w("BitChatRepo", "Incremental history page failed at offset $offset after retry: ${pageRes.exceptionOrNull()?.message}")
                        return@withLock false
                    }
                    val page = pageRes.getOrNull().orEmpty()
                    fetchedMessages.addAll(page)
                    if (page.size < pageSize) break
                    offset += pageSize
                }
            }

            val uniqueMessages = fetchedMessages.distinctBy { it.id }.sortedBy { it.timestamp }
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

            // Build the profile map from Room first. Remote lookup is only used for cache misses.
            val profilesMap = mutableMapOf<String, SupabaseProfile>()
            dao.getAllCachedProfiles().forEach { cached ->
                val p = SupabaseProfile(
                    id = cached.uid,
                    username = cached.username,
                    fullName = cached.fullName,
                    avatarUrl = cached.avatarUrl,
                    bio = cached.bio,
                    profession = cached.profession,
                    email = cached.email,
                    lastSeen = cached.lastSeen,
                    isOnline = cached.isOnline
                )
                profilesMap[cached.uid.lowercase()] = p
                if (cached.username.isNotBlank()) {
                    profilesMap[cached.username.lowercase()] = p
                    profilesMap[cached.username.lowercase().removePrefix("@").removeSuffix(".link")] = p
                }
                if (cached.email.isNotBlank()) profilesMap[cached.email.lowercase()] = p
            }

            val localContacts = dao.getAllContactsList()
            val chatsToUpdate = mutableMapOf<String, ChatEntity>()
            val existingChats = dao.getAllChatsList()
            val existingChatMap = existingChats.associateBy { it.id }.toMutableMap()

            for (supaMsg in uniqueMessages) {
                // Direct message ownership is UUID-only. Username/email/name and
                // an existing local chat are never valid routing fallbacks.
                val isSentByMe = supaMsg.senderId.equals(myUid, ignoreCase = true)
                val isReceivedByMe = supaMsg.receiverId.equals(myUid, ignoreCase = true)

                if (!isSentByMe && !isReceivedByMe) continue

                val existing = dao.findMessageByServerId(supaMsg.id)
                val timeStr = sdf.format(Date(supaMsg.timestamp))

                var msgText = supaMsg.text
                var replySnippet: String? = null
                var replySenderName: String? = null
                val replyToId: String? = supaMsg.replyToId?.ifBlank { supaMsg.replyToMessageId }

                if (msgText.startsWith("[REPLY_QUOTE:")) {
                    try {
                        val meta = msgText.substringAfter("[REPLY_QUOTE:").substringBefore("]")
                        val encSender = meta.substringBefore("|")
                        val encSnippet = meta.substringAfter("|")
                        replySenderName = java.net.URLDecoder.decode(encSender, "UTF-8")
                        replySnippet = java.net.URLDecoder.decode(encSnippet, "UTF-8")
                        msgText = msgText.substringAfter("]")
                    } catch (_: Exception) {}
                }

                if (msgText.startsWith("[REACTION:")) {
                    try {
                        val payload = msgText.substringAfter("[REACTION:").substringBefore("]")
                        val parts = payload.split("|")
                        val msgKey = parts.getOrNull(0) ?: ""
                        val emoji = parts.getOrNull(1) ?: ""
                        val act = parts.getOrNull(2) ?: "ADD"
                        if (msgKey.isNotBlank() && emoji.isNotBlank()) {
                            if (act == "REMOVE") {
                                dao.deleteReaction(msgKey, supaMsg.senderId, emoji)
                            } else {
                                dao.insertReaction(
                                    ReactionEntity(
                                        messageId = msgKey,
                                        chatId = supaMsg.chatId,
                                        uid = supaMsg.senderId,
                                        emoji = emoji,
                                        createdAt = supaMsg.timestamp
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (existing == null) {
                    val entity = MessageEntity(
                        chatId = supaMsg.chatId,
                        senderName = if (isSentByMe) "You" else supaMsg.senderName,
                        text = msgText,
                        timestampString = timeStr,
                        isFromUser = isSentByMe,
                        isRead = supaMsg.isRead,
                        senderUid = supaMsg.senderId,
                        receiverUid = supaMsg.receiverId,
                        serverMessageId = supaMsg.id,
                        syncStatus = "SYNCED",
                        deliveryState = if (supaMsg.isRead) "READ" else "DELIVERED",
                        timestamp = supaMsg.timestamp,
                        messageType = supaMsg.messageType,
                        replyToMessageId = replyToId,
                        replySnippet = replySnippet,
                        replySenderName = replySenderName,
                        isEdited = supaMsg.isEdited,
                        isDeletedForEveryone = supaMsg.isDeletedForEveryone,
                        isPinned = supaMsg.isPinned
                    )
                    dao.insertMessage(entity)
                    if (supaMsg.isPinned) {
                        dao.insertPinnedMessage(
                            PinnedMessageEntity(
                                chatId = supaMsg.chatId,
                                messageId = supaMsg.id,
                                pinnedByUid = supaMsg.senderId,
                                pinnedAt = supaMsg.timestamp
                            )
                        )
                    }
                } else {
                    // Reconcile server delivery/read state without ever regressing a
                    // stronger local state (READ > DELIVERED > SENT).
                    val remoteState = when {
                        supaMsg.isRead || supaMsg.status.equals("READ", ignoreCase = true) -> "READ"
                        supaMsg.status.equals("DELIVERED", ignoreCase = true) -> "DELIVERED"
                        else -> "SENT"
                    }
                    if (supaMsg.id.isNotBlank()) {
                        dao.updateMessageDeliveryStateByServerId(supaMsg.id, remoteState)
                    }

                    // Real-time update for edits
                    if (supaMsg.text != existing.text || supaMsg.isEdited != existing.isEdited) {
                        dao.updateMessageTextWithRowId(existing.id, supaMsg.id, msgText, System.currentTimeMillis())
                    }
                    // Real-time update for pinned messages
                    if (supaMsg.isPinned != existing.isPinned) {
                        dao.updateMessagePinnedStatus(supaMsg.id, supaMsg.isPinned, if (supaMsg.isPinned) System.currentTimeMillis() else null)
                        if (supaMsg.isPinned) {
                            dao.insertPinnedMessage(
                                PinnedMessageEntity(
                                    chatId = supaMsg.chatId,
                                    messageId = supaMsg.id,
                                    pinnedByUid = supaMsg.senderId,
                                    pinnedAt = System.currentTimeMillis()
                                )
                            )
                        } else {
                            dao.deletePinnedMessage(supaMsg.chatId, supaMsg.id)
                        }
                    }
                    // Real-time update for deletes
                    if (supaMsg.isDeletedForEveryone && !existing.isDeletedForEveryone) {
                        dao.updateMessageDeletedForEveryone(supaMsg.id, System.currentTimeMillis())
                    }
                }

                // Resolve opponent identity and profile
                val opponentUid = if (isSentByMe) supaMsg.receiverId else supaMsg.senderId
                val cleanOpponentUid = opponentUid.trim().lowercase()

                val matchedProf = profilesMap[cleanOpponentUid]
                    ?: profilesMap[cleanOpponentUid.removePrefix("@").removeSuffix(".link")]
                    ?: if (!isSentByMe && supaMsg.senderName.isNotBlank()) {
                        profilesMap[supaMsg.senderName.trim().lowercase()]
                            ?: profilesMap[supaMsg.senderName.trim().lowercase().removePrefix("@").removeSuffix(".link")]
                    } else null

                val matchedContact = localContacts.find { it.id == opponentUid || it.name.equals(opponentUid, ignoreCase = true) }

                var resolvedOpponentName = when {
                    matchedProf != null && matchedProf.fullName.isNotBlank() -> matchedProf.fullName
                    matchedProf != null && matchedProf.username.isNotBlank() -> matchedProf.username
                    matchedContact != null && matchedContact.name.isNotBlank() -> matchedContact.name
                    !isSentByMe && supaMsg.senderName.isNotBlank() && !supaMsg.senderName.contains("-") && !supaMsg.senderName.startsWith("user_") && supaMsg.senderName != "Contact" -> supaMsg.senderName
                    else -> ""
                }

                var resolvedOpponentAvatar = when {
                    matchedProf != null && !matchedProf.avatarUrl.isNullOrBlank() -> matchedProf.avatarUrl!!
                    matchedContact != null && matchedContact.avatarType.isNotBlank() && matchedContact.avatarType != "default" -> matchedContact.avatarType
                    else -> "default"
                }

                // Fallback single profile lookup if unresolved and opponentUid looks like a valid user ID
                if (resolvedOpponentName.isBlank() && cleanOpponentUid.isNotBlank() && cleanOpponentUid != "all" && cleanOpponentUid != "group" && !cleanOpponentUid.startsWith("group_")) {
                    try {
                        val singleProf = SupabaseService.getProfile(opponentUid).getOrNull()
                            ?: SupabaseService.getProfileByUsername(opponentUid).getOrNull()
                        if (singleProf != null) {
                            cacheProfileLocally(singleProf)
                            profilesMap[singleProf.id.lowercase()] = singleProf
                            if (singleProf.username.isNotBlank()) profilesMap[singleProf.username.lowercase()] = singleProf
                            if (singleProf.fullName.isNotBlank()) resolvedOpponentName = singleProf.fullName
                            else if (singleProf.username.isNotBlank()) resolvedOpponentName = singleProf.username
                            if (!singleProf.avatarUrl.isNullOrBlank()) resolvedOpponentAvatar = singleProf.avatarUrl!!
                        }
                    } catch (_: Exception) {}
                }

                if (resolvedOpponentName.isBlank()) {
                    resolvedOpponentName = when {
                        opponentUid.contains("@") -> opponentUid.substringBefore("@")
                        cleanOpponentUid.endsWith(".link") -> cleanOpponentUid.removeSuffix(".link")
                        opponentUid.isNotBlank() && !opponentUid.contains("-") && !opponentUid.contains("_") -> opponentUid
                        else -> "Contact"
                    }
                }

                val targetChat = findOrCreateCanonicalChat(
                    chatId = supaMsg.chatId,
                    opponentUid = opponentUid,
                    opponentName = resolvedOpponentName,
                    opponentAvatar = resolvedOpponentAvatar
                )

                val shouldUpdateName = resolvedOpponentName.isNotBlank() && resolvedOpponentName != "Contact" && (
                    targetChat.name.isBlank() ||
                    targetChat.name == "Contact" ||
                    targetChat.name == "User" ||
                    targetChat.name == targetChat.id ||
                    targetChat.name == opponentUid ||
                    targetChat.name.contains("-") ||
                    (targetChat.name.contains("_") && !targetChat.name.contains(" "))
                )
                val shouldUpdateAvatar = resolvedOpponentAvatar.isNotBlank() && resolvedOpponentAvatar != "default" && (targetChat.avatarType.isBlank() || targetChat.avatarType == "default")

                val updated = targetChat.copy(
                    name = if (shouldUpdateName) resolvedOpponentName else targetChat.name,
                    avatarType = if (shouldUpdateAvatar) resolvedOpponentAvatar else targetChat.avatarType,
                    lastMessage = msgText,
                    timeString = timeStr,
                    lastUpdated = maxOf(targetChat.lastUpdated, supaMsg.timestamp)
                )
                existingChatMap[targetChat.id] = updated
                chatsToUpdate[targetChat.id] = updated
            }

            // Sweep existingChatMap to resolve any chats that still have placeholder/raw ID names
            for ((cId, chat) in existingChatMap) {
                if (chat.chatType == "GROUP" || chat.category == "Group" || chat.id.startsWith("group_")) continue
                val isUgly = chat.name.isBlank() || chat.name == "Contact" || chat.name == "User" || chat.name == chat.id || chat.name.contains("-") || (chat.name.contains("_") && !chat.name.contains(" "))
                val isNoAvatar = chat.avatarType.isBlank() || chat.avatarType == "default"
                if (isUgly || isNoAvatar) {
                    val parts = chat.participantUids.split(",").map { it.trim() }.filter {
                        it.isNotBlank() && !it.equals(myUid, ignoreCase = true) && !it.equals(myUsername, ignoreCase = true) && !it.equals(myEmail, ignoreCase = true) && it != "user_me"
                    }
                    val pUid = parts.firstOrNull() ?: if (isUgly) chat.name else ""
                    if (pUid.isNotBlank()) {
                        val prof = profilesMap[pUid.lowercase()]
                            ?: profilesMap[pUid.lowercase().removePrefix("@").removeSuffix(".link")]
                        if (prof != null) {
                            val realName = prof.fullName.ifBlank { prof.username }
                            val realAvatar = prof.avatarUrl ?: ""
                            val fixed = chat.copy(
                                name = if (realName.isNotBlank() && isUgly) realName else chat.name,
                                avatarType = if (realAvatar.isNotBlank() && isNoAvatar) realAvatar else chat.avatarType
                            )
                            chatsToUpdate[cId] = fixed
                        }
                    }
                }
            }

            if (chatsToUpdate.isNotEmpty()) {
                dao.insertChats(chatsToUpdate.values.toList())
            }

            val latestRemoteTimestamp = uniqueMessages.maxOfOrNull { it.updatedAt } ?: previousSync
            if (latestRemoteTimestamp > previousSync) {
                dao.upsertSyncState(SyncStateEntity(syncKey, latestRemoteTimestamp))
            }

            // Run deduplication again after history sync
            deduplicateCopyChats()
            true
            } catch (e: Exception) {
                Log.w("BitChatRepo", "syncAllChatHistory error: " + e.message, e)
                false
            }
        }
    }

    suspend fun syncMessagesForChat(chatId: String) = withContext(Dispatchers.IO) {
        if (chatId.isBlank()) return@withContext
        try {
            dao.getUserIdentity().firstOrNull() ?: return@withContext
            val latestLocal = dao.getLatestLocalMessageTimestamp(chatId) ?: 0L

            // Room is the first source. Network only fetches messages newer than the local cache.
            val res = if (latestLocal > 0L) {
                SupabaseService.fetchMessagesSince(chatId, latestLocal, limit = 100)
            } else {
                SupabaseService.fetchMessages(chatId, limit = 100)
            }

            if (res.isSuccess) {
                val messages = res.getOrNull().orEmpty()
                for (supaMsg in messages) handleIncomingMessage(supaMsg, chatId)
                val newest = messages.maxOfOrNull { it.updatedAt } ?: latestLocal
                if (newest > latestLocal) {
                    dao.upsertSyncState(SyncStateEntity("chat:$chatId", newest))
                }
            }
        } catch (ex: Exception) {
            Log.w("BitChatRepo", "syncMessagesForChat error: " + ex.message)
        }
    }

    suspend fun initChat(
        targetUid: String,
        currentUid: String? = null,
        targetName: String = "User",
        avatarType: String = "default"
    ): ChatEntity {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val myUid = currentUid ?: (currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me")

        // First deduplicate existing copy chats
        deduplicateCopyChats()

        // Lookup profile to get exact Supabase UID, Full Name & Avatar URL
        var finalUid = targetUid
        var finalName = targetName
        var finalAvatar = avatarType
        try {
            val prof = getLocalProfile(targetUid)
                ?: getLocalProfile(targetName)
                ?: SupabaseService.getProfile(targetUid).getOrNull()
                ?: SupabaseService.getProfileByUsername(targetUid).getOrNull()
                ?: SupabaseService.getProfileByUsername(targetName).getOrNull()
            cacheProfileLocally(prof)
            if (prof != null && prof.id.isNotBlank()) {
                val myEmail = currentIdentity?.email?.lowercase()
                val isSelf = prof.id.equals(myUid, ignoreCase = true) || (myEmail != null && prof.email.equals(myEmail, ignoreCase = true))
                if (!isSelf) {
                    finalUid = prof.id
                    if (prof.fullName.isNotBlank()) finalName = prof.fullName
                    else if (prof.username.isNotBlank()) finalName = prof.username
                    val av = prof.avatarUrl
                    if (!av.isNullOrBlank()) finalAvatar = av
                }
            }
        } catch (e: Exception) {
            Log.w("BitChatRepo", "Error fetching target profile in initChat: ${e.message}")
        }

        val sortedUids = listOf(myUid, finalUid).filter { it.isNotBlank() }.distinct().sorted()
        val chatId = "chat_" + (sortedUids.getOrElse(0) { "a" } + "_" + sortedUids.getOrElse(1) { "b" }).hashCode().absoluteValue.toString(16)

        // Check if an existing direct chat with this participant already exists
        val allLocal = dao.getAllChatsList()
        val existingByParticipant = allLocal.find { chat ->
            chat.category != "Group" && chat.chatType != "GROUP" && (
                chat.id == chatId ||
                (chat.participantUids.isNotBlank() && chat.participantUids.split(",").map { it.trim().lowercase() }.contains(finalUid.lowercase())) ||
                (chat.participantUids.isNotBlank() && chat.participantUids.split(",").map { it.trim().lowercase() }.contains(targetUid.lowercase())) ||
                (targetName.isNotBlank() && targetName != "User" && targetName != "Contact" && chat.name.equals(targetName, ignoreCase = true)) ||
                (finalName.isNotBlank() && finalName != "User" && finalName != "Contact" && chat.name.equals(finalName, ignoreCase = true))
            )
        }

        if (existingByParticipant != null) {
            val shouldUpdateName = finalName.isNotBlank() && finalName != "User" && finalName != "Contact" && existingByParticipant.name != finalName
            val shouldUpdateAvatar = finalAvatar.isNotBlank() && finalAvatar != "default" && existingByParticipant.avatarType != finalAvatar
            val shouldUpdateParticipants = existingByParticipant.participantUids.isBlank() || !existingByParticipant.participantUids.contains(finalUid)
            if (shouldUpdateName || shouldUpdateAvatar || shouldUpdateParticipants) {
                val updated = existingByParticipant.copy(
                    name = if (shouldUpdateName) finalName else existingByParticipant.name,
                    avatarType = if (shouldUpdateAvatar) finalAvatar else existingByParticipant.avatarType,
                    participantUids = if (shouldUpdateParticipants) sortedUids.joinToString(",") else existingByParticipant.participantUids
                )
                dao.insertChats(listOf(updated))
                return updated
            }
            return existingByParticipant
        }

        val newChat = ChatEntity(
            id = chatId,
            name = finalName,
            lastMessage = "Started direct encrypted conversation",
            timeString = "Just now",
            unreadCount = 0,
            isOnline = true,
            category = "Personal",
            avatarType = finalAvatar,
            participantUids = sortedUids.joinToString(",")
        )
        dao.insertChats(listOf(newChat))
        return newChat
    }

    fun setupPresence() {
        // Presence is handled via SupabaseRealtimeManager
    }

    fun observeUserPresence(targetUid: String): Flow<Pair<Boolean, Long>> =
        SupabaseRealtimeManager.userPresenceMap.map { map ->
            if (targetUid.isBlank()) Pair(false, 0L)
            else {
                val clean = targetUid.trim().removePrefix("@").lowercase().removeSuffix(".link")
                map[targetUid]
                    ?: map[targetUid.lowercase()]
                    ?: map[clean]
                    ?: map["$clean.link"]
                    ?: map["@$clean"]
                    ?: map["@$clean.link"]
                    ?: map.entries.firstOrNull {
                        it.key.equals(targetUid, ignoreCase = true) ||
                        it.key.equals(clean, ignoreCase = true) ||
                        it.key.equals("$clean.link", ignoreCase = true)
                    }?.value
                    ?: Pair(false, 0L)
            }
        }

    fun observeMultiUserPresence(identifiers: List<String>): Flow<Pair<Boolean, Long>> =
        SupabaseRealtimeManager.userPresenceMap.map { map ->
            for (id in identifiers) {
                if (id.isBlank()) continue
                val clean = id.trim().removePrefix("@").lowercase().removeSuffix(".link")
                val found = map[id]
                    ?: map[id.lowercase()]
                    ?: map[clean]
                    ?: map["$clean.link"]
                    ?: map["@$clean"]
                    ?: map["@$clean.link"]
                    ?: map.entries.firstOrNull {
                        it.key.equals(id, ignoreCase = true) ||
                        it.key.equals(clean, ignoreCase = true) ||
                        it.key.equals("$clean.link", ignoreCase = true)
                    }?.value
                if (found != null && found.first) {
                    return@map found
                }
            }
            var maxLastSeen = 0L
            for (id in identifiers) {
                if (id.isBlank()) continue
                val clean = id.trim().removePrefix("@").lowercase().removeSuffix(".link")
                val found = map[id] ?: map[id.lowercase()] ?: map[clean] ?: map["$clean.link"]
                if (found != null && found.second > maxLastSeen) {
                    maxLastSeen = found.second
                }
            }
            Pair(false, maxLastSeen)
        }

    suspend fun setTyping(chatId: String, isTyping: Boolean) {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val myUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        val myName = currentIdentity?.fullName?.ifBlank { currentIdentity.username } ?: "Me"
        SupabaseRealtimeManager.sendTypingBroadcast(chatId, myUid, myName, isTyping)
    }

    fun observeTyping(chatId: String, targetUid: String): Flow<Boolean> =
        SupabaseRealtimeManager.typingUsersByChat.map { map ->
            val unChatId = if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else "chat_$chatId"
            val typers = map[chatId] ?: map[unChatId] ?: (if (targetUid.isNotBlank()) map[targetUid] ?: map[targetUid.lowercase()] else null) ?: emptyList()
            typers.isNotEmpty()
        }

    suspend fun uploadMedia(
        chatId: String,
        fileUri: Uri,
        mimeType: String,
        clientUploadId: String = UUID.randomUUID().toString(),
        context: Context,
        onProgress: (Double) -> Unit = {}
    ): String {
        val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot read file")
        val ext = when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            mimeType.contains("gif", ignoreCase = true) -> "gif"
            mimeType.contains("mp4", ignoreCase = true) -> "mp4"
            mimeType.contains("webm", ignoreCase = true) -> "webm"
            mimeType.contains("mpeg", ignoreCase = true) || mimeType.contains("mp3", ignoreCase = true) -> "mp3"
            mimeType.contains("ogg", ignoreCase = true) -> "ogg"
            mimeType.contains("wav", ignoreCase = true) -> "wav"
            mimeType.contains("pdf", ignoreCase = true) -> "pdf"
            else -> "bin"
        }
        val fileName = "chat_${chatId}_${UUID.randomUUID().toString().take(8)}.${ext}"
        return SupabaseService.uploadChatMedia(chatId, fileName, bytes, mimeType).getOrThrow()
    }

    suspend fun createGroupChat(
        title: String,
        description: String?,
        avatarUrl: String?,
        memberUids: List<String>
    ): String {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        val chatId = "grp_" + UUID.randomUUID().toString().take(8)

        val allParticipants = (memberUids + currentUid).distinct()
        val now = System.currentTimeMillis()

        val groupMemberEntities = mutableListOf<GroupMemberEntity>()
        for (uid in allParticipants) {
            val role = if (uid == currentUid) "OWNER" else "MEMBER"
            groupMemberEntities.add(GroupMemberEntity(chatId = chatId, uid = uid, role = role, joinedAt = now))
        }

        dao.insertGroupMembers(groupMemberEntities)

        val chatEntity = ChatEntity(
            id = chatId,
            name = title,
            lastMessage = "Group created",
            timeString = "Just now",
            unreadCount = 0,
            isOnline = true,
            category = "All",
            avatarType = if (avatarUrl.isNullOrBlank()) "default" else avatarUrl,
            lastUpdated = now,
            participantUids = allParticipants.joinToString(","),
            chatType = "GROUP",
            ownerUid = currentUid,
            description = description,
            permissions = null
        )
        dao.insertChats(listOf(chatEntity))

        return chatId
    }

    fun observeGroupMembers(chatId: String): Flow<List<GroupMemberEntity>> {
        return dao.getMembersForChat(chatId)
    }

    suspend fun getChatById(chatId: String): ChatEntity? {
        return dao.getChatById(chatId)
    }

    suspend fun leaveGroup(chatId: String) {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        dao.deleteGroupMember(chatId, currentUid)
        dao.deleteChatById(chatId)
        dao.clearMembersForChat(chatId)
        sendSystemEvent(chatId, "MEMBER_LEFT", "🚪 A member left the group")
    }

    suspend fun addMemberToGroup(chatId: String, newUid: String) {
        val now = System.currentTimeMillis()
        val newMemberEntity = GroupMemberEntity(
            chatId = chatId,
            uid = newUid,
            role = "MEMBER",
            joinedAt = now
        )
        dao.insertGroupMembers(listOf(newMemberEntity))
        sendSystemEvent(chatId, "MEMBER_ADDED", "👋 Added $newUid to the group")
    }

    suspend fun removeMemberFromGroup(chatId: String, targetUid: String) {
        dao.deleteGroupMember(chatId, targetUid)
        sendSystemEvent(chatId, "MEMBER_REMOVED", "🚫 Removed $targetUid from the group")
    }

    suspend fun promoteAdmin(chatId: String, targetUid: String) {
        dao.updateGroupMemberRole(chatId, targetUid, "ADMIN")
        sendSystemEvent(chatId, "ADMIN_PROMOTED", "⭐ Promoted $targetUid to Admin")
    }

    suspend fun demoteAdmin(chatId: String, targetUid: String) {
        dao.updateGroupMemberRole(chatId, targetUid, "MEMBER")
        sendSystemEvent(chatId, "ADMIN_DEMOTED", "⬇️ Demoted $targetUid to Member")
    }

    suspend fun transferOwnership(chatId: String, newOwnerUid: String) {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        dao.updateChatOwner(chatId, newOwnerUid)
        dao.updateGroupMemberRole(chatId, currentUid, "ADMIN")
        dao.updateGroupMemberRole(chatId, newOwnerUid, "OWNER")
        sendSystemEvent(chatId, "OWNER_TRANSFERRED", "👑 Transferred group ownership to $newOwnerUid")
    }

    suspend fun updateGroupDetails(
        chatId: String,
        name: String,
        description: String?,
        avatarUrl: String?,
        permissionsJson: String?
    ) {
        val now = System.currentTimeMillis()
        dao.updateGroupDetails(
            chatId = chatId,
            name = name,
            description = description,
            avatarType = if (avatarUrl.isNullOrBlank()) "default" else avatarUrl,
            permissions = permissionsJson,
            lastUpdated = now
        )
        sendSystemEvent(chatId, "GROUP_INFO_UPDATED", "✏️ Group settings & permissions were updated")
    }

    suspend fun toggleMuteChat(chatId: String, isMuted: Boolean) {
        dao.updateChatMuteStatus(chatId, isMuted)
    }

    suspend fun reportMemberOrGroup(
        chatId: String,
        targetUid: String?,
        reason: String,
        details: String?
    ) {
        submitAbuseReport("GROUP", chatId, reason, details)
    }


    private fun resolveMutationReceiver(local: MessageEntity, currentUid: String): String {
        return if (local.senderUid.equals(currentUid, ignoreCase = true)) {
            local.receiverUid
        } else {
            local.senderUid
        }
    }

    suspend fun editMessage(
        chatId: String,
        messageId: Long,
        serverMessageId: String?,
        newText: String
    ) {
        val now = System.currentTimeMillis()
        val targetId = serverMessageId ?: messageId.toString()
        val local = dao.getMessageById(messageId)
        dao.updateMessageTextWithRowId(messageId, targetId, newText, now)

        if (!serverMessageId.isNullOrBlank()) {
            val result = SupabaseService.editMessage(serverMessageId, newText)
            if (!result.isSuccess) {
                android.util.Log.w("BitChatRepo", "Remote edit persistence failed for $serverMessageId; broadcasting realtime mutation")
            }
            if (local != null) {
                val identity = dao.getUserIdentity().firstOrNull()
                SupabaseRealtimeManager.broadcastMessageMutation(
                    SupabaseMessage(
                        id = serverMessageId,
                        chatId = chatId,
                        senderId = identity?.supabaseUid ?: local.senderUid,
                        senderName = identity?.fullName?.ifBlank { identity.username } ?: local.senderName,
                        receiverId = if (chatId.startsWith("group_", ignoreCase = true)) "all" else resolveMutationReceiver(local, identity?.supabaseUid ?: local.senderUid),
                        text = newText,
                        timestamp = now,
                        status = local.deliveryState.ifBlank { "SENT" },
                        isRead = local.isRead,
                        messageType = local.messageType,
                        isEdited = true,
                        isPinned = local.isPinned,
                        clientMsgId = local.clientMessageId
                    ),
                    mutation = "EDIT"
                )
            }
        }
    }

    suspend fun deleteMessage(
        chatId: String,
        messageId: Long,
        serverMessageId: String?,
        deleteForEveryone: Boolean
    ) {
        val now = System.currentTimeMillis()
        val targetId = serverMessageId ?: messageId.toString()
        val local = dao.getMessageById(messageId)

        if (deleteForEveryone) {
            dao.updateMessageDeletedForEveryone(targetId, now)
            if (!serverMessageId.isNullOrBlank()) {
                val result = SupabaseService.deleteMessageForEveryone(serverMessageId)
                if (result.isSuccess && local != null) {
                    val identity = dao.getUserIdentity().firstOrNull()
                    SupabaseRealtimeManager.broadcastMessageMutation(
                        SupabaseMessage(
                            id = serverMessageId,
                            chatId = chatId,
                            senderId = identity?.supabaseUid ?: local.senderUid,
                            senderName = identity?.fullName?.ifBlank { identity.username } ?: local.senderName,
                            receiverId = resolveMutationReceiver(local, identity?.supabaseUid ?: local.senderUid),
                            text = "",
                            timestamp = now,
                            status = local.deliveryState.ifBlank { "SENT" },
                            isRead = local.isRead,
                            messageType = local.messageType,
                            isDeletedForEveryone = true,
                            isPinned = false,
                            clientMsgId = local.clientMessageId
                        ),
                        mutation = "DELETE"
                    )
                }
            }
        } else {
            dao.updateMessageDeletedForMe(messageId)
        }
    }

    suspend fun toggleReaction(

        chatId: String,
        messageId: Long,
        serverMessageId: String?,
        emoji: String
    ) {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        val msgKey = serverMessageId ?: messageId.toString()

        val existing = dao.getReactionsForMessage(msgKey).firstOrNull()?.firstOrNull { it.uid == currentUid }

        if (existing != null && existing.emoji == emoji) {
            dao.deleteReaction(msgKey, currentUid, emoji)
            sendSystemEvent(chatId, "REACTION_REMOVED", "[REACTION:$msgKey|$emoji|REMOVE]")
        } else {
            if (existing != null) {
                dao.deleteReaction(msgKey, currentUid, existing.emoji)
                sendSystemEvent(chatId, "REACTION_REMOVED", "[REACTION:$msgKey|${existing.emoji}|REMOVE]")
            }
            val newReaction = ReactionEntity(
                messageId = msgKey,
                chatId = chatId,
                uid = currentUid,
                emoji = emoji,
                createdAt = System.currentTimeMillis()
            )
            dao.insertReaction(newReaction)
            sendSystemEvent(chatId, "REACTION_ADDED", "[REACTION:$msgKey|$emoji|ADD]")
        }
    }

    suspend fun togglePinMessage(
        chatId: String,
        messageId: Long,
        serverMessageId: String?,
        currentIsPinned: Boolean,
        broadcastToOthers: Boolean = false
    ) {
        val currentIdentity = dao.getUserIdentity().firstOrNull()
        val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
        val targetId = serverMessageId ?: messageId.toString()
        val newPinned = !currentIsPinned
        val now = System.currentTimeMillis()
        val local = dao.getMessageById(messageId)
        val existingPin = dao.getPinnedMessagesForChat(chatId).firstOrNull()?.firstOrNull { it.messageId == targetId }
        val existingPinIsPrivate = existingPin?.pinnedByUid?.startsWith("private:", ignoreCase = true) == true

        // A private pin exists only in this device's local pinned_messages table.
        // A shared pin is persisted in the message row and Supabase so both peers see it.
        val syncForEveryone = broadcastToOthers || (!newPinned && local?.isPinned == true && !existingPinIsPrivate)

        if (syncForEveryone) {
            dao.updateMessagePinnedStatus(targetId, newPinned, if (newPinned) now else null)
        }

        if (newPinned) {
            dao.insertPinnedMessage(
                PinnedMessageEntity(
                    chatId = chatId,
                    messageId = targetId,
                    pinnedByUid = if (broadcastToOthers) currentUid else "private:$currentUid",
                    pinnedAt = now
                )
            )
        } else {
            dao.deletePinnedMessage(chatId, targetId)
        }

        if (syncForEveryone && !serverMessageId.isNullOrBlank()) {
            val result = SupabaseService.updateMessagePinnedStatus(serverMessageId, newPinned)
            if (!result.isSuccess) {
                android.util.Log.w("BitChatRepo", "Remote pin persistence failed for $serverMessageId")
            }
            if (local != null) {
                SupabaseRealtimeManager.broadcastMessageMutation(
                    SupabaseMessage(
                        id = serverMessageId,
                        chatId = chatId,
                        senderId = currentUid,
                        senderName = currentIdentity?.fullName?.ifBlank { currentIdentity.username } ?: local.senderName,
                        receiverId = if (chatId.startsWith("group_", ignoreCase = true)) "all" else resolveMutationReceiver(local, currentUid),
                        text = local.text,
                        timestamp = now,
                        status = local.deliveryState.ifBlank { "SENT" },
                        isRead = local.isRead,
                        messageType = local.messageType,
                        isPinned = newPinned,
                        isEdited = local.isEdited,
                        clientMsgId = local.clientMessageId
                    ),
                    mutation = if (newPinned) "PIN" else "UNPIN"
                )
            }
        }
    }

    suspend fun unpinAllMessages(chatId: String) {
        val pinned = dao.getPinnedMessagesForChat(chatId).firstOrNull() ?: emptyList()
        for (item in pinned) {
            dao.updateMessagePinnedStatus(item.messageId, false, null)
            if (!item.messageId.contains("-")) {
                SupabaseService.updateMessagePinnedStatus(item.messageId, false)
            }
        }
        dao.clearPinnedMessagesForChat(chatId)
        sendSystemEvent(chatId, "MESSAGE_UNPINNED_ALL", "📌 All pinned messages were unpinned")
    }

    suspend fun forwardMessage(
        originalMessage: MessageEntity,
        targetChatIds: List<String>,
        senderName: String
    ) {
        for (targetChatId in targetChatIds) {
            sendMessage(
                chatId = targetChatId,
                senderName = senderName,
                text = originalMessage.text,
                isFromUser = true,
                isRead = true,
                isForwarded = true,
                forwardedFromMessageId = originalMessage.serverMessageId ?: originalMessage.clientMessageId,
                messageType = originalMessage.messageType
            )
        }
    }

    suspend fun sendSystemEvent(chatId: String, eventType: String, eventText: String) {
        sendMessage(
            chatId = chatId,
            senderName = "System",
            text = eventText,
            isFromUser = false,
            isRead = true,
            messageType = "SYSTEM_EVENT",
            systemEventType = eventType
        )
    }

    fun observePinnedMessages(chatId: String): Flow<List<PinnedMessageEntity>> {
        return dao.getPinnedMessagesForChat(chatId)
    }

    fun observeReactionsForChat(chatId: String): Flow<List<ReactionEntity>> {
        return dao.getReactionsForChat(chatId)
    }

    suspend fun updateMessageDeliveryStatusFromRemote(serverMessageId: String, status: String) {
        if (serverMessageId.isNotBlank() && status.isNotBlank()) {
            dao.updateMessageDeliveryStateByServerId(serverMessageId, status)
        }
    }

    suspend fun resetAllDatabaseData() {
        dao.clearUserIdentity()
        dao.clearAllChats()
        dao.clearAllContacts()
        dao.clearAllMessages()
        prepopulateIfEmpty()
    }
}
