package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun getBdCurrentJoinedDate(): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        sdf.format(Date())
    } catch (e: Exception) {
        "29 Jul 2026"
    }
}

@Entity(tableName = "user_identity")
data class UserIdentityEntity(
    @PrimaryKey val id: Int = 1,
    val phoneNumber: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarPath: String? = null,
    val profileType: String = "Private Profile",
    val isVerified: Boolean = false,
    val profession: String = "🎓 Student",
    val email: String = "",
    val isEmailVerified: Boolean = false,
    val secondaryEmail: String = "",
    val isSecondaryEmailVerified: Boolean = false,
    val supabaseUid: String = "",
    val joinedDate: String = getBdCurrentJoinedDate(),
    val birthDate: String = "",
    val loginTimestamp: Long = 0L,
    val privacySettingsJson: String? = null
) {
    val publicId: String
        get() = if (username.isNotBlank()) username.removePrefix("@") else "usr_8921"

    val qrIdentifier: String
        get() = supabaseUid.trim().takeIf { it.isNotBlank() && !it.equals("user_me", ignoreCase = true) }
            ?: username.trim().removePrefix("@")
}

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val statusText: String,
    val isOnline: Boolean,
    val isFavorite: Boolean,
    val categoryLetter: String,
    val avatarType: String = "default"
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastMessage: String,
    val timeString: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val category: String, // All, Unread, Work, Personal
    val avatarType: String = "default",
    val lastUpdated: Long = System.currentTimeMillis(),
    val participantUids: String = "",
    val chatType: String = "DIRECT", // DIRECT, GROUP
    val ownerUid: String? = null,
    val description: String? = null,
    val permissions: String? = null,
    val isMuted: Boolean = false
)

@Entity(tableName = "group_members", primaryKeys = ["chatId", "uid"])
data class GroupMemberEntity(
    val chatId: String,
    val uid: String,
    val role: String, // OWNER, ADMIN, MEMBER
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val senderName: String,
    val text: String,
    val timestampString: String,
    val isFromUser: Boolean,
    val isRead: Boolean = false,
    val senderUid: String = "",
    val receiverUid: String = "",
    val deliveryState: String = "SENT", // LOCAL_PENDING, SENDING, SENT, DELIVERED, READ, FAILED
    val timestamp: Long = System.currentTimeMillis(),
    val clientMessageId: String = java.util.UUID.randomUUID().toString(),
    val serverMessageId: String? = null,
    val serverTimestamp: Long? = null,
    val syncStatus: String = "SYNCED", // PENDING, SYNCED, FAILED
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val replyToMessageId: String? = null,
    val replySnippet: String? = null,
    val replySenderName: String? = null,
    val isForwarded: Boolean = false,
    val forwardedFromMessageId: String? = null,
    val deletedAt: Long? = null,
    val isDeletedForEveryone: Boolean = false,
    val isDeletedForMe: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val messageType: String = "TEXT", // TEXT, MEDIA, SYSTEM_EVENT
    val systemEventType: String? = null,
    val mentionedUids: String? = null,
    val reactionsJson: String? = null
)

@Entity(tableName = "message_reactions", primaryKeys = ["messageId", "uid", "emoji"])
data class ReactionEntity(
    val messageId: String,
    val chatId: String,
    val uid: String,
    val emoji: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pinned_messages", primaryKeys = ["chatId", "messageId"])
data class PinnedMessageEntity(
    val chatId: String,
    val messageId: String,
    val pinnedByUid: String,
    val pinnedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users", primaryKeys = ["targetUid"])
data class BlockedUserEntity(
    val targetUid: String,
    val username: String = "",
    val displayName: String = "",
    val blockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_profiles")
data class CachedProfileEntity(
    @PrimaryKey val uid: String,
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val profession: String = "",
    val email: String = "",
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "user_sessions", primaryKeys = ["deviceId"])
data class UserSessionEntity(
    val deviceId: String,
    val deviceName: String = "Android Device",
    val platform: String = "Android",
    val appVersion: String = "1.0",
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isCurrentDevice: Boolean = false,
    val isActive: Boolean = true
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val callType: String = "AUDIO",
    val direction: String = "OUTGOING",
    val timestampMillis: Long = System.currentTimeMillis(),
    val timeString: String = "Just now",
    val durationSeconds: Int = 0,
    val avatarType: String = "default"
)

