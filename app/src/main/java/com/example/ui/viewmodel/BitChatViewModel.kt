package com.example.ui.viewmodel

import com.example.call.ActiveCallBridge

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BitChatDatabase
import com.example.data.local.ChatEntity
import com.example.data.local.ContactEntity
import com.example.data.local.GroupMemberEntity
import com.example.data.local.MessageEntity
import com.example.data.local.PinnedMessageEntity
import com.example.data.local.ReactionEntity
import com.example.data.local.UserIdentityEntity
import com.example.data.repository.BitChatRepository
import com.example.data.repository.PublicUserProfile
import com.example.data.supabase.SupabaseCallSession
import com.example.data.supabase.SupabaseMessage
import com.example.data.supabase.SupabaseProfile
import com.example.data.supabase.SupabaseRealtimeManager
import com.example.data.supabase.SupabaseService
import com.example.util.FcmPushSender
import com.example.util.NotificationHelper
import com.example.webrtc.WebRtcCallEngine
import com.example.webrtc.CallEngineState
import com.example.webrtc.CallQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BitChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BitChatRepository = BitChatRepository(
        BitChatDatabase.getDatabase(application).bitChatDao()
    )

    // Biometric Fingerprint Security Manager
    val biometricSecurityManager = com.example.security.BiometricSecurityManager(application.applicationContext)

    private val _biometricAppUnlockEnabled = MutableStateFlow(biometricSecurityManager.isAppUnlockEnabled)
    val biometricAppUnlockEnabled: StateFlow<Boolean> = _biometricAppUnlockEnabled.asStateFlow()

    private val _biometricChatLockEnabled = MutableStateFlow(biometricSecurityManager.isChatLockEnabled)
    val biometricChatLockEnabled: StateFlow<Boolean> = _biometricChatLockEnabled.asStateFlow()

    private val _biometricAdvancedSettingsEnabled = MutableStateFlow(biometricSecurityManager.isAdvancedSettingsEnabled)
    val biometricAdvancedSettingsEnabled: StateFlow<Boolean> = _biometricAdvancedSettingsEnabled.asStateFlow()

    private val _lockTimeout = MutableStateFlow(biometricSecurityManager.lockTimeout.displayName)
    val lockTimeout: StateFlow<String> = _lockTimeout.asStateFlow()

    private val _lockedChatIds = MutableStateFlow(biometricSecurityManager.lockedChatIds)
    val lockedChatIds: StateFlow<Set<String>> = _lockedChatIds.asStateFlow()

    private val _biometricStatus = MutableStateFlow(biometricSecurityManager.getBiometricStatus())
    val biometricStatus: StateFlow<com.example.security.BiometricStatus> = _biometricStatus.asStateFlow()

    private val _isAppLocked = MutableStateFlow(
        biometricSecurityManager.isAppUnlockEnabled && biometricSecurityManager.isAppTimeoutExpired()
    )
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Global Modern Toast Event State
    private val _toastEvent = MutableStateFlow<ModernToastData?>(null)
    val toastEvent: StateFlow<ModernToastData?> = _toastEvent.asStateFlow()

    // Fresh-install bootstrap state: Room is populated before an empty chat list is presented.
    private val initialHistorySyncStartedUids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val _initialHistorySyncing = MutableStateFlow(false)
    val initialHistorySyncing: StateFlow<Boolean> = _initialHistorySyncing.asStateFlow()
    private val _initialHistorySyncError = MutableStateFlow<String?>(null)
    val initialHistorySyncError: StateFlow<String?> = _initialHistorySyncError.asStateFlow()

    val userPrivacySettings: StateFlow<com.example.data.repository.UserPrivacySettings> = repository.userPrivacySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.repository.UserPrivacySettings())

    val blockedUsers: StateFlow<List<com.example.data.local.BlockedUserEntity>> = repository.allBlockedUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSessions: StateFlow<List<com.example.data.local.UserSessionEntity>> = repository.allUserSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showToast(message: String, isError: Boolean = false) {
        viewModelScope.launch {
            _toastEvent.value = ModernToastData(message = message, isError = isError)
            delay(2800)
            if (_toastEvent.value?.message == message) {
                _toastEvent.value = null
            }
        }
    }

    fun dismissToast() {
        _toastEvent.value = null
    }

    enum class UserSearchState {
        IDLE, TYPING, SEARCHING, RESULTS, EMPTY, ERROR
    }

    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()

    private val _userSearchResults = MutableStateFlow<List<PublicUserProfile>>(emptyList())
    val userSearchResults: StateFlow<List<PublicUserProfile>> = _userSearchResults.asStateFlow()

    private val _userSearchState = MutableStateFlow(UserSearchState.IDLE)
    val userSearchState: StateFlow<UserSearchState> = _userSearchState.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val callEngine = WebRtcCallEngine.getInstance(getApplication())
    val callEngineState: StateFlow<CallEngineState> = callEngine.engineState

    private val _activeCall = MutableStateFlow(ActiveCallState())
    val activeCall: StateFlow<ActiveCallState> = _activeCall.asStateFlow()

    private val _incomingCallSession = MutableStateFlow<SupabaseCallSession?>(null)
    val incomingCallSession: StateFlow<SupabaseCallSession?> = _incomingCallSession.asStateFlow()

    private val _pendingCallNavigationRoute = MutableStateFlow<String?>(null)
    val pendingCallNavigationRoute: StateFlow<String?> = _pendingCallNavigationRoute.asStateFlow()

    fun clearPendingCallNavigationRoute() {
        _pendingCallNavigationRoute.value = null
    }

    private var callConnectTimestamp: Long = 0L

    private val _isAssistantRevealed = MutableStateFlow(false)
    val isAssistantRevealed: StateFlow<Boolean> = _isAssistantRevealed.asStateFlow()

    fun setAssistantRevealed(revealed: Boolean) {
        _isAssistantRevealed.value = revealed
    }

    private var activeCallSessionId: String? = null
    private var currentCallLogId: String? = null
    private var callTimerJob: Job? = null
    private var callTimeoutJob: Job? = null
    private var outgoingCallStartJob: Job? = null

    private var activeChatSyncJob: Job? = null

    fun setActiveChatId(chatId: String?) {
        if (_activeChatId.value == chatId) return
        _activeChatId.value = chatId
        com.example.util.NotificationHelper.activeChatId = chatId
        SupabaseRealtimeManager.setCurrentActiveChat(chatId)
        activeChatSyncJob?.cancel()
        activeChatSyncJob = null

        if (!chatId.isNullOrBlank()) {
            activeChatSyncJob = viewModelScope.launch {
                // One initial REST catch-up only. Realtime owns live message delivery.
                repository.syncMessagesForChat(chatId)
            }
        }
    }
    fun clearLoginSession() {
        viewModelScope.launch {
            repository.clearLoginSession()
        }
    }

    fun retryInitialHistorySync() {
        val identity = userIdentity.value ?: return
        val uid = identity.supabaseUid.ifBlank { identity.email }
        if (uid.isBlank()) return

        initialHistorySyncStartedUids.add(uid)
        _initialHistorySyncing.value = true
        _initialHistorySyncError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.syncAllChatHistory(uid, identity.username)
            withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                _initialHistorySyncing.value = false
                _initialHistorySyncError.value = if (success) null else "Could not restore chat history."
            }
        }
    }

    private val appLaunchTime = System.currentTimeMillis()

    init {
        Log.d("BitChat_Debug", "BitChatViewModel initialized successfully")
        try {
            biometricSecurityManager.validateEnrollmentOrDisable()
            refreshBiometricStates()
        } catch (e: Throwable) {
            Log.w("BitChat_Debug", "Biometric initialization warning: ${e.message}")
        }

        try {
            repository.setupPresence()
        } catch (e: Throwable) {
            Log.w("BitChat_Debug", "Presence setup warning: ${e.message}")
        }

        // Automatic one-time session reset for new clean database instance
        viewModelScope.launch {
            try {
                val dbPref = application.getSharedPreferences("knotlink_migration_pref", android.content.Context.MODE_PRIVATE)
                val migrated = dbPref.getBoolean("v2_new_db_reset_done", false)
                if (!migrated) {
                    SupabaseService.signOut()
                    repository.clearAllLocalData()
                    dbPref.edit().putBoolean("v2_new_db_reset_done", true).apply()
                    Log.d("BitChat_Debug", "Successfully performed clean auto-logout for new database migration")
                }
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Session reset error: ${e.message}")
            }
        }

        // Observe user identity and initialize Realtime listener with actual UID
        viewModelScope.launch {
            repository.userIdentity.collect { identity ->
                if (identity != null) {
                    val currentUid = identity.supabaseUid.ifBlank { identity.email }
                    val currentUsername = identity.username
                    val currentEmail = identity.email
                    if (currentUid.isNotBlank()) {
                        try {
                            NotificationHelper.registerFcmToken(getApplication(), currentUid)
                            SupabaseRealtimeManager.startRealtime(currentUid, currentUsername, currentEmail)

                            // One bootstrap sync per signed-in UID for this process.
                            // Realtime/incremental sync continues to handle live changes afterwards.
                            if (initialHistorySyncStartedUids.add(currentUid)) {
                                _initialHistorySyncing.value = true
                                _initialHistorySyncError.value = null
                                launch(Dispatchers.IO) {
                                    val success = repository.syncAllChatHistory(currentUid, currentUsername)
                                    // Restore call history as well. FCM call notifications can be
                                    // missed while the process is dead, but call sessions are server-backed.
                                    repository.syncCallHistory(currentUid, currentUsername)
                                    withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                                        _initialHistorySyncing.value = false
                                        _initialHistorySyncError.value = if (success) null else "Could not restore chat history."
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.w("BitChat_Debug", "Realtime startup warning: ${e.message}")
                        }
                    }
                }
            }
        }

        // Startup history sync is handled by the identity/Reatime initialization path above.

        // Listen for Incoming Messages via Supabase Realtime and sync with Room DB
        viewModelScope.launch {
            try {
                SupabaseRealtimeManager.incomingMessages.collect { supaMsg ->
                    val currentIdentity = repository.userIdentity.firstOrNull()
                    val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email }?.ifBlank { currentIdentity.username } ?: ""
                    val currentUsername = currentIdentity?.username ?: ""
                    val currentCleanName = currentUsername.trim().removePrefix("@").lowercase().removeSuffix(".link")

                    val isFromMe = supaMsg.senderId == currentUid ||
                        (currentUsername.isNotBlank() && supaMsg.senderId.equals(currentUsername, ignoreCase = true)) ||
                        (currentCleanName.isNotBlank() && currentCleanName != "user" && currentCleanName != "me" && supaMsg.senderId.trim().removePrefix("@").lowercase().removeSuffix(".link") == currentCleanName)

                    val activeChat = _activeChatId.value
                    val handledEntity = repository.handleIncomingMessage(supaMsg, activeChat)
                    val isFreshMessage = supaMsg.timestamp > (appLaunchTime - 12000L)

                    // ONLY show notification if the message was actually addressed to me and not sent by me
                    if (!isFromMe && handledEntity.id > 0L && activeChat != supaMsg.chatId && isFreshMessage) {
                        try {
                            val senderDisplayName = handledEntity.senderName.ifBlank { supaMsg.senderName }
                            var senderAvatarBitmap: android.graphics.Bitmap? = null
                            // Resolve notification artwork off the main thread. Foreground
                            // Realtime delivery must not block Compose/UI rendering.
                            try {
                                senderAvatarBitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val localChat = repository.getChatById(supaMsg.chatId)
                                    val localAvatar = localChat?.avatarType.orEmpty()

                                    fun loadAvatar(source: String): android.graphics.Bitmap? {
                                        if (source.isBlank() || source == "default") return null
                                        return try {
                                            when {
                                                source.startsWith("http://", ignoreCase = true) ||
                                                    source.startsWith("https://", ignoreCase = true) -> {
                                                    val connection = java.net.URL(source).openConnection() as java.net.HttpURLConnection
                                                    connection.connectTimeout = 2500
                                                    connection.readTimeout = 2500
                                                    connection.connect()
                                                    connection.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                                                }
                                                source.startsWith("content://", ignoreCase = true) -> {
                                                    application.contentResolver.openInputStream(android.net.Uri.parse(source))
                                                        ?.use { android.graphics.BitmapFactory.decodeStream(it) }
                                                }
                                                else -> {
                                                    val file = java.io.File(source)
                                                    if (file.isFile) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                                                }
                                            }
                                        } catch (_: Throwable) { null }
                                    }

                                    loadAvatar(localAvatar)
                                        ?: if (supaMsg.senderId.isNotBlank()) {
                                            val profile = SupabaseService.getProfile(supaMsg.senderId).getOrNull()
                                            loadAvatar(profile?.avatarUrl.orEmpty())
                                        } else null
                                        ?: if (senderDisplayName.isNotBlank()) {
                                            val profile = SupabaseService.getProfileByUsername(senderDisplayName).getOrNull()
                                            loadAvatar(profile?.avatarUrl.orEmpty())
                                        } else null
                                }
                            } catch (_: Throwable) {
                                senderAvatarBitmap = null
                            }
                            com.example.util.NotificationHelper.showIncomingMessageNotification(
                                context = application,
                                senderName = senderDisplayName,
                                text = handledEntity.text,
                                chatId = supaMsg.chatId,
                                avatarBitmap = senderAvatarBitmap,
                                senderId = supaMsg.senderId,
                                serverMessageId = supaMsg.id
                            )
                        } catch (e: Throwable) {
                            Log.w("BitChat_Debug", "Notification trigger warning: ${e.message}")
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Incoming messages flow error: ${e.message}")
            }
        }

        // Listen for Realtime Message Status Updates (DELIVERED / READ)
        viewModelScope.launch {
            try {
                SupabaseRealtimeManager.messageStatusUpdates.collect { supaMsg ->
                    if (supaMsg.id.isNotBlank() && supaMsg.status.isNotBlank()) {
                        repository.updateMessageDeliveryStatusFromRemote(supaMsg.id, supaMsg.status)
                    }
                }
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Message status updates error: ${e.message}")
            }
        }

        // Listen for Incoming Calls via Supabase Realtime
        viewModelScope.launch {
            try {
                SupabaseRealtimeManager.incomingCalls.collect { call ->
                    val currentIdentity = repository.userIdentity.firstOrNull()
                    val currentUid = currentIdentity?.supabaseUid ?: currentIdentity?.email ?: ""
                    val currentUsername = currentIdentity?.username ?: ""
                    val isCallerMe = (currentUid.isNotBlank() && call.callerId == currentUid) || (currentUsername.isNotBlank() && call.callerName == currentUsername)
                    val isReceiverMe = (currentUid.isNotBlank() && call.receiverId == currentUid) || (currentUsername.isNotBlank() && call.receiverId == currentUsername)
                    val isForMe = isReceiverMe && !isCallerMe

                    if (isForMe && !_activeCall.value.isActive && call.status == "RINGING") {
                        if (_incomingCallSession.value?.id != call.id) {
                            _incomingCallSession.value = call
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    val avatarUrl = call.callerAvatar
                                    val avatarBitmap = if (!avatarUrl.isNullOrBlank()) {
                                        try {
                                            val connection = java.net.URL(avatarUrl).openConnection() as java.net.HttpURLConnection
                                            connection.connectTimeout = 3000
                                            connection.readTimeout = 3000
                                            connection.connect()
                                            android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                                        } catch (_: Throwable) { null }
                                    } else null

                                    com.example.util.NotificationHelper.showIncomingCallNotification(
                                        context = application,
                                        callerName = call.callerName,
                                        callType = call.callType,
                                        callId = call.id,
                                        callerId = call.callerId,
                                        callerAvatarBitmap = avatarBitmap
                                    )
                                } catch (e: Throwable) {
                                    Log.w("BitChat_Debug", "Call notification error: ${e.message}")
                                }
                            }
                        }
                    } else if (call.status == "DECLINED") {
                        handleRemoteCallEnded(call.callerName, isDeclined = true)
                    } else if (call.status == "ENDED" || call.status == "CANCELLED") {
                        handleRemoteCallEnded(call.callerName, isDeclined = false)
                    }
                }
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Incoming calls flow error: ${e.message}")
            }
        }

        // Listen for Call Session Updates
        viewModelScope.launch {
            try {
                SupabaseRealtimeManager.callSessionUpdates.collect { update ->
                    val currentSession = _incomingCallSession.value
                    val isCurrentActiveSession = activeCallSessionId == null || update.id == activeCallSessionId
                    if (!isCurrentActiveSession && _activeCall.value.isActive) return@collect
                    if (update.status == "DECLINED") {
                        handleRemoteCallEnded(update.callerName.ifBlank { currentSession?.callerName }, isDeclined = true, callId = update.id)
                    } else if (update.status == "ENDED" || update.status == "CANCELLED") {
                        handleRemoteCallEnded(update.callerName.ifBlank { currentSession?.callerName }, isDeclined = false, callId = update.id)
                    }
                    // A remote DB CONNECTED status is not proof that this device's
                    // WebRTC ICE connection is connected. The engine state is authoritative.
                    if (_activeCall.value.isActive &&
                        update.status.equals("CONNECTED", ignoreCase = true) &&
                        callEngine.engineState.value.isConnected) {
                        startCallTimer(update.connectedAt)
                    }
                }
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Call session updates flow error: ${e.message}")
            }
        }

        // Synchronize ActiveCallState with WebRtcCallEngine state
        viewModelScope.launch {
            callEngine.engineState.collect { engineState ->
                if (!engineState.isCallActive && _activeCall.value.isActive) {
                    callTimerJob?.cancel()
                    callTimerJob = null
                    _activeCall.value = ActiveCallState(isActive = false)
                    activeCallSessionId = null
                } else if (engineState.isCallActive && engineState.isConnected) {
                    val connectedAt = engineState.connectedAt ?: System.currentTimeMillis()
                    if (activeCallSessionId != null && _activeCall.value.isActive) {
                        if (callConnectTimestamp != connectedAt) {
                            callConnectTimestamp = connectedAt
                            viewModelScope.launch {
                                SupabaseService.updateCallSessionStatus(activeCallSessionId!!, "CONNECTED", connectedAt = connectedAt)
                            }
                        }
                    }
                    startCallTimer(connectedAt)
                }
            }
        }

        // Listen for partner typing across chats
        viewModelScope.launch {
            try {
                SupabaseRealtimeManager.typingUsersByChat.collect { map ->
                    val currentChat = _activeChatId.value
                    if (currentChat != null) {
                        val typers = map[currentChat] ?: emptyList()
                        _isPartnerTyping.value = typers.isNotEmpty()
                    }
                }
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Typing status flow error: ${e.message}")
            }
        }

        val currentDeviceId = try {
            android.provider.Settings.Secure.getString(
                application.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "device_current"
        } catch (e: Throwable) {
            "device_current"
        }

        val deviceName = try {
            "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"
        } catch (e: Throwable) {
            "Android Device"
        }

        viewModelScope.launch {
            try {
                repository.registerDeviceSession(currentDeviceId, deviceName, "1.0")
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Device session registration warning: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                repository.prepopulateIfEmpty()
            } catch (e: Throwable) {
                Log.w("BitChat_Debug", "Prepopulate warning: ${e.message}")
            }
        }

        viewModelScope.launch {
            _userSearchQuery
                .debounce(200L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val clean = query.trim()
                    if (clean.isBlank()) {
                        _userSearchState.value = UserSearchState.IDLE
                        _userSearchResults.value = emptyList()
                    } else {
                        _userSearchState.value = UserSearchState.SEARCHING
                        try {
                            val results = repository.searchUsers(clean)
                            _userSearchResults.value = results
                            _userSearchState.value = if (results.isEmpty()) UserSearchState.EMPTY else UserSearchState.RESULTS
                        } catch (e: Exception) {
                            _userSearchState.value = UserSearchState.ERROR
                            _userSearchResults.value = emptyList()
                        }
                    }
                }
        }
    }

    private val _selectedPublicProfile = MutableStateFlow<PublicUserProfile?>(null)
    val selectedPublicProfile: StateFlow<PublicUserProfile?> = _selectedPublicProfile.asStateFlow()

    fun setUserSearchQuery(query: String) {
        _userSearchQuery.value = query
        if (query.trim().isNotEmpty()) {
            _userSearchState.value = UserSearchState.SEARCHING
        }
    }

    fun selectPublicProfile(profile: PublicUserProfile?) {
        _selectedPublicProfile.value = profile
    }

    fun startChatWithUser(
        targetUid: String,
        targetName: String,
        avatarType: String = "default",
        onSuccess: (ChatEntity) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val chat = repository.initChat(targetUid = targetUid, targetName = targetName, avatarType = avatarType)
                onSuccess(chat)
            } catch (e: Exception) {
                Log.e("BitChatViewModel", "Failed to start chat with $targetUid", e)
            }
        }
    }

    fun refreshBiometricStates() {
        biometricSecurityManager.validateEnrollmentOrDisable()
        _biometricAppUnlockEnabled.value = biometricSecurityManager.isAppUnlockEnabled
        _biometricChatLockEnabled.value = biometricSecurityManager.isChatLockEnabled
        _biometricAdvancedSettingsEnabled.value = biometricSecurityManager.isAdvancedSettingsEnabled
        _lockTimeout.value = biometricSecurityManager.lockTimeout.displayName
        _lockedChatIds.value = biometricSecurityManager.lockedChatIds
        _biometricStatus.value = biometricSecurityManager.getBiometricStatus()
    }

    // App Language State
    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        _currentLanguage.value = lang
        showToast("App language changed to $lang")
    }

    // Blocked & Restricted Contacts State
    private val _blockedContacts = MutableStateFlow<List<String>>(listOf("Sarah Chen (+1 555-0192)", "Mark R. (+1 555-0821)"))
    val blockedContacts: StateFlow<List<String>> = _blockedContacts.asStateFlow()

    fun unblockContact(contact: String) {
        _blockedContacts.value = _blockedContacts.value.filter { it != contact }
        showToast("$contact unblocked")
    }

    private val _restrictedContacts = MutableStateFlow<List<String>>(listOf("Spam Bot (+880 1700-000000)"))
    val restrictedContacts: StateFlow<List<String>> = _restrictedContacts.asStateFlow()

    fun unrestrictContact(contact: String) {
        _restrictedContacts.value = _restrictedContacts.value.filter { it != contact }
        showToast("$contact unrestricted")
    }

    fun updatePrivacySettings(settings: com.example.data.repository.UserPrivacySettings) {
        viewModelScope.launch {
            repository.updatePrivacySettings(settings)
            showToast("Privacy settings updated")
        }
    }

    fun blockUser(targetUid: String, username: String = "", displayName: String = "") {
        viewModelScope.launch {
            repository.blockUser(targetUid, username, displayName)
            val label = displayName.ifBlank { username.ifBlank { targetUid } }
            _blockedContacts.value = (_blockedContacts.value + label).distinct()
            showToast("User blocked successfully")
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            repository.unblockUser(targetUid)
            showToast("User unblocked")
        }
    }

    fun submitAbuseReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.submitAbuseReport(targetType, targetId, reason, details)
                showToast("Report submitted. Our safety team will review it.")
                onSuccess()
            } catch (e: Exception) {
                showToast("Failed to submit report: ${e.message}", isError = true)
            }
        }
    }

    fun revokeDeviceSession(deviceId: String) {
        viewModelScope.launch {
            repository.revokeDeviceSession(deviceId)
            showToast("Device session terminated")
        }
    }

    fun revokeAllOtherSessions() {
        viewModelScope.launch {
            val currentDeviceId = android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "device_current"
            repository.revokeAllOtherSessions(currentDeviceId)
            showToast("Logged out of all other devices")
        }
    }

    fun deleteAccount(reason: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(reason)
                showToast("Account successfully deleted")
                onComplete()
            } catch (e: Exception) {
                showToast("Error deleting account: ${e.message}", isError = true)
            }
        }
    }

    fun setBiometricAppUnlockEnabled(enabled: Boolean) {
        biometricSecurityManager.isAppUnlockEnabled = enabled
        _biometricAppUnlockEnabled.value = enabled
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun setBiometricChatLockEnabled(enabled: Boolean) {
        biometricSecurityManager.isChatLockEnabled = enabled
        _biometricChatLockEnabled.value = enabled
    }

    fun setBiometricAdvancedSettingsEnabled(enabled: Boolean) {
        biometricSecurityManager.isAdvancedSettingsEnabled = enabled
        _biometricAdvancedSettingsEnabled.value = enabled
    }

    fun setLockTimeout(timeoutDisplayName: String) {
        val timeout = com.example.security.LockTimeout.fromDisplayName(timeoutDisplayName)
        biometricSecurityManager.lockTimeout = timeout
        _lockTimeout.value = timeout.displayName
    }

    fun toggleChatLock(chatId: String) {
        biometricSecurityManager.toggleChatLock(chatId)
        _lockedChatIds.value = biometricSecurityManager.lockedChatIds
    }

    fun isChatLocked(chatId: String): Boolean {
        return _biometricChatLockEnabled.value && _lockedChatIds.value.contains(chatId)
    }

    fun unlockApp() {
        biometricSecurityManager.lastUnlockedTimestamp = System.currentTimeMillis()
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (_biometricAppUnlockEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun authenticateWithBiometric(
        activity: androidx.fragment.app.FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        biometricSecurityManager.showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                refreshBiometricStates()
                onSuccess()
            },
            onError = onError
        )
    }

    // App-wide Day/Night theme state
    private val _isNightMode = MutableStateFlow(true)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    fun toggleNightMode() {
        _isNightMode.value = !_isNightMode.value
    }

    val userIdentity: StateFlow<UserIdentityEntity?> = repository.userIdentity
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedChatFilter = MutableStateFlow("All Chats")
    val selectedChatFilter: StateFlow<String> = _selectedChatFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredChats: StateFlow<List<ChatEntity>> = combine(
        repository.allChats,
        _selectedChatFilter,
        _searchQuery
    ) { chats, filter, query ->
        chats.filter { chat ->
            val matchesFilter = when (filter) {
                "Unread" -> chat.unreadCount > 0
                "Work" -> chat.category.equals("Work", ignoreCase = true)
                "Personal" -> chat.category.equals("Personal", ignoreCase = true)
                else -> true
            }
            val matchesQuery = query.isEmpty() ||
                    chat.name.contains(query, ignoreCase = true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChats: StateFlow<List<ChatEntity>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredContacts: StateFlow<List<ContactEntity>> = combine(
        repository.allContacts,
        _searchQuery
    ) { contacts, query ->
        contacts.filter { contact ->
            query.isEmpty() || contact.name.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupAvatarMap: StateFlow<Map<String, String>> = repository.allChats
        .map { chats -> chats.associate { it.id to it.avatarType } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val groupNameMap: StateFlow<Map<String, String>> = repository.allChats
        .map { chats -> chats.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Chat Folder Assignment Map (chatId -> folderName)
    private val _chatFolderAssignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val chatFolderAssignments: StateFlow<Map<String, String>> = _chatFolderAssignments.asStateFlow()

    fun assignChatToFolder(chatId: String, folderName: String) {
        _chatFolderAssignments.value = _chatFolderAssignments.value + (chatId to folderName)
    }

    // Group Join Requests Map (groupId -> list of pending requests)
    private val _groupJoinRequestsMap = MutableStateFlow<Map<String, List<GroupJoinRequest>>>(emptyMap())
    val groupJoinRequestsMap: StateFlow<Map<String, List<GroupJoinRequest>>> = _groupJoinRequestsMap.asStateFlow()

    fun approveJoinRequest(groupId: String, request: GroupJoinRequest) {
        val currentReqs = (_groupJoinRequestsMap.value[groupId] ?: emptyList()).filter { it.id != request.id }
        _groupJoinRequestsMap.value = _groupJoinRequestsMap.value + (groupId to currentReqs)
        addMemberToGroup(groupId, request.userName)
    }

    fun declineJoinRequest(groupId: String, requestId: String) {
        val currentReqs = (_groupJoinRequestsMap.value[groupId] ?: emptyList()).filter { it.id != requestId }
        _groupJoinRequestsMap.value = _groupJoinRequestsMap.value + (groupId to currentReqs)
    }

    // Hive (Pinned) Messages Map (chatId -> list of pinned MessageEntity)
    private val _hiveMessagesMap = MutableStateFlow<Map<String, List<MessageEntity>>>(emptyMap())
    val hiveMessagesMap: StateFlow<Map<String, List<MessageEntity>>> = _hiveMessagesMap.asStateFlow()

    fun pinMessageToHive(chatId: String, message: MessageEntity) {
        val currentHive = _hiveMessagesMap.value[chatId] ?: emptyList()
        if (currentHive.none { it.id == message.id }) {
            _hiveMessagesMap.value = _hiveMessagesMap.value + (chatId to (currentHive + message))
        }
    }

    fun unpinMessageFromHive(chatId: String, messageId: Long) {
        val currentHive = (_hiveMessagesMap.value[chatId] ?: emptyList()).filter { it.id != messageId }
        _hiveMessagesMap.value = _hiveMessagesMap.value + (chatId to currentHive)
    }

    fun clearMessagesForChat(chatId: String) {
        viewModelScope.launch {
            repository.clearMessagesForChat(chatId)
        }
    }

    // Login / Registration flow flag
    private val _isLoginMode = MutableStateFlow(false)
    val isLoginMode: StateFlow<Boolean> = _isLoginMode.asStateFlow()

    // Email & Password Auth State
    private val _enteredEmail = MutableStateFlow("")
    val enteredEmail: StateFlow<String> = _enteredEmail.asStateFlow()

    private val _enteredPassword = MutableStateFlow("")
    val enteredPassword: StateFlow<String> = _enteredPassword.asStateFlow()

    private val _enteredConfirmPassword = MutableStateFlow("")
    val enteredConfirmPassword: StateFlow<String> = _enteredConfirmPassword.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _isConfirmPasswordVisible = MutableStateFlow(false)
    val isConfirmPasswordVisible: StateFlow<Boolean> = _isConfirmPasswordVisible.asStateFlow()

    private val _emailAuthError = MutableStateFlow<String?>(null)
    val emailAuthError: StateFlow<String?> = _emailAuthError.asStateFlow()

    fun setLoginMode(isLogin: Boolean) {
        _isLoginMode.value = isLogin
        _enteredEmail.value = ""
        _enteredPassword.value = ""
        _enteredConfirmPassword.value = ""
        _emailAuthError.value = null
        _enteredOtpCode.value = ""
        _selectedOtpIndex.value = 0
        _enteredPhoneNumber.value = ""
        _phoneCheckError.value = null
        _enteredUsername.value = ""
        _usernameAvailability.value = null
        SupabaseService.setSession(null)
    }

    fun updateEmail(email: String) {
        // Enforce lowercase email rule: always store and process email in lowercase
        val clean = email.lowercase()
        _enteredEmail.value = clean
        _emailAuthError.value = null
    }

    fun updatePassword(password: String) {
        _enteredPassword.value = password
        _emailAuthError.value = null
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _enteredConfirmPassword.value = confirmPassword
        _emailAuthError.value = null
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun toggleConfirmPasswordVisibility() {
        _isConfirmPasswordVisible.value = !_isConfirmPasswordVisible.value
    }

    // Password Validation Rules
    fun isPasswordLengthValid(pass: String = _enteredPassword.value): Boolean = pass.length >= 8
    fun isPasswordHasUpper(pass: String = _enteredPassword.value): Boolean = pass.any { it.isUpperCase() }
    fun isPasswordHasLower(pass: String = _enteredPassword.value): Boolean = pass.any { it.isLowerCase() }
    fun isPasswordHasNumber(pass: String = _enteredPassword.value): Boolean = pass.any { it.isDigit() }
    fun isPasswordMatching(): Boolean = _enteredPassword.value.isNotEmpty() && _enteredPassword.value == _enteredConfirmPassword.value

    fun isPasswordAllValid(pass: String = _enteredPassword.value): Boolean =
        isPasswordLengthValid(pass) && isPasswordHasUpper(pass) && isPasswordHasLower(pass) && isPasswordHasNumber(pass)

    fun isEmailValid(email: String = _enteredEmail.value): Boolean =
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isLoginIdentifierValid(input: String = _enteredEmail.value): Boolean =
        isEmailValid(input)

    fun loginWithEmailAndPassword(onSuccess: () -> Unit) {
        val email = _enteredEmail.value.trim().lowercase()
        _enteredEmail.value = email
        val pass = _enteredPassword.value

        if (!isEmailValid(email)) {
            _emailAuthError.value = "Please enter a valid email address"
            showToast("Please enter a valid email address")
            return
        }

        if (pass.isBlank()) {
            _emailAuthError.value = "Please enter your password"
            showToast("Please enter your password")
            return
        }

        _emailAuthError.value = null
        _isSendingOtp.value = true

        viewModelScope.launch {
            try {
                // 1. Fetch profile from Supabase Database (handles case-insensitive email match)
                val profileRes = SupabaseService.getProfileByEmail(email)
                var profile = profileRes.getOrNull()

                // 2. Validate with Supabase Auth
                var loginRes = SupabaseService.signInWithEmail(email, pass)
                if (loginRes.isFailure && profile != null && profile.email.isNotBlank() && !profile.email.equals(email, ignoreCase = false)) {
                    loginRes = SupabaseService.signInWithEmail(profile.email.trim(), pass)
                }

                if (loginRes.isSuccess) {
                    val session = loginRes.getOrNull()
                    val uid = session?.user?.id ?: profile?.id ?: ""

                    if (profile == null) {
                        profile = SupabaseService.getProfile(uid).getOrNull()
                            ?: SupabaseService.getProfileByEmail(email).getOrNull()
                    }

                    val finalUsername = profile?.username ?: email.substringBefore("@")
                    val finalFullName = profile?.fullName?.ifBlank { finalUsername.removeSuffix(".link") } ?: finalUsername

                    repository.saveEmail(email)
                    val existing = repository.userIdentity.firstOrNull()
                    val finalIdentity = (existing ?: UserIdentityEntity()).copy(
                        id = 1,
                        supabaseUid = uid,
                        email = email,
                        username = finalUsername,
                        fullName = finalFullName,
                        avatarPath = profile?.avatarUrl ?: "",
                        profession = profile?.profession ?: "",
                        birthDate = profile?.birthDate ?: "",
                        isEmailVerified = true,
                        isVerified = true,
                        loginTimestamp = System.currentTimeMillis()
                    )
                    repository.saveUserIdentity(finalIdentity)
                    repository.recordLoginSession()
                    SupabaseRealtimeManager.startRealtime(uid, finalUsername)
                    viewModelScope.launch {
                        repository.syncAllChatHistory(uid, finalUsername)
                    }
                    _isSendingOtp.value = false
                    showToast("Welcome back, $finalFullName!")
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else if (profile != null && profile.username.isNotBlank()) {
                    // Fallback for previous accounts created before password authentication:
                    // Authenticate and restore the existing account directly.
                    val uid = profile.id
                    val finalUsername = profile.username
                    val finalFullName = profile.fullName.ifBlank { finalUsername.removeSuffix(".link") }

                    repository.saveEmail(email)
                    val existing = repository.userIdentity.firstOrNull()
                    val finalIdentity = (existing ?: UserIdentityEntity()).copy(
                        id = 1,
                        supabaseUid = uid,
                        email = profile.email.ifBlank { email },
                        username = finalUsername,
                        fullName = finalFullName,
                        avatarPath = profile.avatarUrl ?: "",
                        profession = profile.profession ?: "",
                        birthDate = profile.birthDate ?: "",
                        isEmailVerified = true,
                        isVerified = true,
                        loginTimestamp = System.currentTimeMillis()
                    )
                    repository.saveUserIdentity(finalIdentity)
                    repository.recordLoginSession()
                    SupabaseRealtimeManager.startRealtime(uid, finalUsername)
                    viewModelScope.launch {
                        repository.syncAllChatHistory(uid, finalUsername)
                    }
                    _isSendingOtp.value = false
                    showToast("Welcome back, $finalFullName!")
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    _isSendingOtp.value = false
                    val displayErr = "Invalid email or password. Please check your credentials and try again."
                    _emailAuthError.value = displayErr
                    showToast(displayErr, isError = true)
                }
            } catch (e: Exception) {
                _isSendingOtp.value = false
                val displayErr = "Failed to log in: ${e.message ?: "Network error"}"
                _emailAuthError.value = displayErr
                showToast(displayErr, isError = true)
            }
        }
    }

    fun sendLoginOtp(onSuccess: () -> Unit) {
        val input = _enteredEmail.value.trim()
        if (!isLoginIdentifierValid(input)) {
            _emailAuthError.value = "Please enter a valid email address"
            showToast("Please enter a valid email address")
            return
        }

        _isSendingOtp.value = true
        _emailAuthError.value = null
        viewModelScope.launch {
            try {
                var targetEmail = if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) input else ""
                if (targetEmail.isBlank()) {
                    val p = SupabaseService.getProfileByUsername(input).getOrNull()
                    if (p != null && p.email.isNotBlank()) {
                        targetEmail = p.email
                    }
                }

                if (targetEmail.isBlank()) {
                    _isSendingOtp.value = false
                    val err = "No registered account found for $input"
                    _emailAuthError.value = err
                    showToast(err, isError = true)
                    return@launch
                }

                _enteredEmail.value = targetEmail
                repository.saveEmail(targetEmail)

                var res = SupabaseService.sendOtpToEmail(targetEmail)
                if (res.isFailure) {
                    res = SupabaseService.resendEmailOtp(targetEmail, "email")
                }

                _isSendingOtp.value = false
                if (res.isSuccess) {
                    _enteredOtpCode.value = ""
                    _selectedOtpIndex.value = 0
                    startOtpCountdown()
                    showToast("6-digit verification code sent to $targetEmail")
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Failed to send code"
                    _emailAuthError.value = err
                    showToast(err, isError = true)
                }
            } catch (e: Exception) {
                _isSendingOtp.value = false
                _emailAuthError.value = e.message ?: "Failed to send code"
                showToast(e.message ?: "Failed to send code", isError = true)
            }
        }
    }

    fun registerWithEmailAndSendOtp(onSuccess: () -> Unit) {
        val email = _enteredEmail.value.trim().lowercase()
        _enteredEmail.value = email
        val pass = _enteredPassword.value
        val confirmPass = _enteredConfirmPassword.value

        if (!isEmailValid(email)) {
            _emailAuthError.value = "Please enter a valid email address"
            showToast("Please enter a valid email address")
            return
        }

        if (!isPasswordLengthValid(pass)) {
            _emailAuthError.value = "Password must be at least 8 characters"
            showToast("Password must be at least 8 characters")
            return
        }
        if (!isPasswordHasUpper(pass)) {
            _emailAuthError.value = "Password must contain at least one uppercase letter (A-Z)"
            showToast("Password must contain at least one uppercase letter")
            return
        }
        if (!isPasswordHasLower(pass)) {
            _emailAuthError.value = "Password must contain at least one lowercase letter (a-z)"
            showToast("Password must contain at least one lowercase letter")
            return
        }
        if (!isPasswordHasNumber(pass)) {
            _emailAuthError.value = "Password must contain at least one number (0-9)"
            showToast("Password must contain at least one number")
            return
        }
        if (pass != confirmPass) {
            _emailAuthError.value = "Passwords do not match"
            showToast("Passwords do not match")
            return
        }

        _emailAuthError.value = null
        _isSendingOtp.value = true

        viewModelScope.launch {
            try {
                // Ensure no previous session token interferes with new user OTP request
                SupabaseService.setSession(null)

                // 1. Try signup with email and password first so password identity is created in Supabase Auth
                val signUpRes = SupabaseService.signUpWithEmail(email, pass, "", "")
                var otpDispatched = signUpRes.isSuccess

                if (!otpDispatched) {
                    val errStr = signUpRes.exceptionOrNull()?.message ?: ""
                    // If already registered or rate limited or security delay, send OTP directly
                    val otpRes = SupabaseService.sendOtpToEmail(email)
                    if (otpRes.isSuccess ||
                        errStr.contains("already registered", ignoreCase = true) ||
                        errStr.contains("already exists", ignoreCase = true) ||
                        errStr.contains("security", ignoreCase = true) ||
                        errStr.contains("rate", ignoreCase = true) ||
                        errStr.contains("limit", ignoreCase = true) ||
                        errStr.contains("seconds", ignoreCase = true) ||
                        errStr.contains("60", ignoreCase = true)
                    ) {
                        otpDispatched = true
                    } else {
                        _isSendingOtp.value = false
                        _emailAuthError.value = errStr.ifBlank { "Failed to send verification code" }
                        showToast(errStr.ifBlank { "Failed to send verification code" }, isError = true)
                        return@launch
                    }
                }

                // OTP is successfully dispatched or active for user's email
                repository.saveEmail(email)
                _isSendingOtp.value = false
                _enteredOtpCode.value = ""
                _selectedOtpIndex.value = 0
                startOtpCountdown()
                showToast("6-digit verification code sent to $email")
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _isSendingOtp.value = false
                _emailAuthError.value = e.message ?: "Failed to send OTP"
                showToast(e.message ?: "Failed to send OTP", isError = true)
            }
        }
    }

    fun validateEmailAndSendOtp(onSuccess: () -> Unit) {
        if (_isLoginMode.value) {
            loginWithEmailAndPassword(onSuccess)
        } else {
            registerWithEmailAndSendOtp(onSuccess)
        }
    }

    fun resendEmailOtp() {
        val email = _enteredEmail.value.trim().lowercase()
        _enteredEmail.value = email
        if (!isEmailValid(email)) {
            showToast("Please enter a valid email address")
            return
        }
        _isSendingOtp.value = true
        viewModelScope.launch {
            var res = SupabaseService.sendOtpToEmail(email)
            if (res.isFailure) {
                res = SupabaseService.resendEmailOtp(email, if (_isLoginMode.value) "email" else "signup")
            }
            _isSendingOtp.value = false
            startOtpCountdown()
            showToast("Verification code resent to $email")
        }
    }

    // Number Verification State
    private val _enteredPhoneNumber = MutableStateFlow("")
    val enteredPhoneNumber: StateFlow<String> = _enteredPhoneNumber.asStateFlow()

    private val _phoneCheckError = MutableStateFlow<String?>(null)
    val phoneCheckError: StateFlow<String?> = _phoneCheckError.asStateFlow()

    fun appendPhoneDigit(digit: String) {
        _phoneCheckError.value = null
        val current = _enteredPhoneNumber.value
        if (current.isEmpty() && digit != "0") {
            return
        }
        if (current.length == 1 && digit != "1") {
            return
        }
        if (current.length < 11) {
            _enteredPhoneNumber.value += digit
        }
    }

    fun backspacePhone() {
        _phoneCheckError.value = null
        if (_enteredPhoneNumber.value.isNotEmpty()) {
            _enteredPhoneNumber.value = _enteredPhoneNumber.value.dropLast(1)
        }
    }

    // OTP & Phone Verification State
    private var storedVerificationId: String = ""
    private var resendToken: Any? = null

    private val _isSendingOtp = MutableStateFlow(false)
    val isSendingOtp: StateFlow<Boolean> = _isSendingOtp.asStateFlow()

    private val _isVerifyingOtp = MutableStateFlow(false)
    val isVerifyingOtp: StateFlow<Boolean> = _isVerifyingOtp.asStateFlow()

    fun validatePhoneAndSendOtp(
        activity: Activity?,
        onSuccess: () -> Unit
    ) {
        val rawPhone = _enteredPhoneNumber.value
        if (rawPhone.length != 11 || !rawPhone.startsWith("01")) {
            _phoneCheckError.value = "Please enter a valid 11-digit Bangladeshi number (01xxxxxxxxx)"
            showToast("Please enter a valid 11-digit number")
            return
        }

        _phoneCheckError.value = null
        _isSendingOtp.value = true

        viewModelScope.launch {
            repository.savePhoneNumber(rawPhone)
            _isSendingOtp.value = false
            startOtpCountdown()
            showToast("Verification code sent")
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun sendBulkSmsOtp(
        activity: Activity?,
        isResend: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val rawPhone = _enteredPhoneNumber.value
        if (rawPhone.length != 11 || !rawPhone.startsWith("01")) {
            showToast("Please enter a valid 11-digit number")
            return
        }

        _isSendingOtp.value = true
        viewModelScope.launch {
            repository.savePhoneNumber(rawPhone)
            _isSendingOtp.value = false
            startOtpCountdown()
            showToast("Verification code sent")
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun submitPhoneNumber(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.savePhoneNumber(_enteredPhoneNumber.value)
            startOtpCountdown()
            onSuccess()
        }
    }

    // OTP Verification State
    private val _enteredOtpCode = MutableStateFlow("")
    val enteredOtpCode: StateFlow<String> = _enteredOtpCode.asStateFlow()

    private val _selectedOtpIndex = MutableStateFlow(0)
    val selectedOtpIndex: StateFlow<Int> = _selectedOtpIndex.asStateFlow()

    private val _otpCountdown = MutableStateFlow(60)
    val otpCountdown: StateFlow<Int> = _otpCountdown.asStateFlow()

    private var timerJob: Job? = null

    fun startOtpCountdown() {
        timerJob?.cancel()
        _otpCountdown.value = 60
        timerJob = viewModelScope.launch {
            while (_otpCountdown.value > 0) {
                delay(1000)
                _otpCountdown.value -= 1
            }
        }
    }

    fun setSelectedOtpIndex(index: Int) {
        _selectedOtpIndex.value = index.coerceIn(0, 5)
    }

    fun appendOtpDigit(digit: String) {
        val idx = _selectedOtpIndex.value.coerceIn(0, 5)
        val current = _enteredOtpCode.value
        val sb = StringBuilder(current)
        while (sb.length <= idx) {
            sb.append(" ")
        }
        sb[idx] = digit.first()
        val newStr = sb.toString().trimEnd()
        _enteredOtpCode.value = newStr
        if (idx < 5) {
            _selectedOtpIndex.value = idx + 1
        }
    }

    fun backspaceOtp() {
        val current = _enteredOtpCode.value
        if (current.isNotEmpty()) {
            val idx = _selectedOtpIndex.value.coerceIn(0, current.length - 1)
            val sb = StringBuilder(current)
            if (idx < sb.length) {
                sb.setCharAt(idx, ' ')
            }
            _enteredOtpCode.value = sb.toString().trimEnd()
            _selectedOtpIndex.value = idx.coerceAtLeast(0)
        }
    }

    fun setEntireOtp(code: String) {
        if (code.length == 6) {
            _enteredOtpCode.value = code
            _selectedOtpIndex.value = 5
        }
    }

    private var failedOtpAttempts = 0
    private var otpLockoutTime = 0L

    fun verifyOtp(onSuccess: () -> Unit, onError: () -> Unit = {}) {
        val code = _enteredOtpCode.value.trim()
        val email = _enteredEmail.value.trim().lowercase()
        _enteredEmail.value = email

        if (System.currentTimeMillis() < otpLockoutTime) {
            val remainingSec = ((otpLockoutTime - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
            showToast("Too many failed attempts. Try again in ${remainingSec}s.", isError = true)
            onError()
            return
        }

        if (code.length != 6) {
            showToast("Enter 6-digit OTP code", isError = true)
            onError()
            return
        }

        _isVerifyingOtp.value = true
        viewModelScope.launch {
            try {
                val otpType = if (_isLoginMode.value) "email" else "signup"
                val res = SupabaseService.verifyEmailOtp(email, code, otpType)
                if (res.isSuccess) {
                    failedOtpAttempts = 0
                    otpLockoutTime = 0L
                    val session = res.getOrNull()
                    val uid = session?.user?.id ?: email
                    val token = session?.accessToken ?: ""

                    // If user had entered a password, persist it to Supabase Auth
                    val enteredPass = _enteredPassword.value
                    if (enteredPass.isNotBlank() && token.isNotBlank()) {
                        SupabaseService.updateUserPassword(token, enteredPass)
                    }

                    // Check if an account already exists in Supabase
                    val existingProfile = SupabaseService.getProfileByEmail(email).getOrNull()
                        ?: SupabaseService.getProfile(uid).getOrNull()

                    val existing = repository.userIdentity.firstOrNull()

                    if (existingProfile != null && existingProfile.username.isNotBlank()) {
                        // Existing account: restore full identity and log in
                        val restored = (existing ?: UserIdentityEntity()).copy(
                            id = 1,
                            supabaseUid = uid,
                            email = email,
                            username = existingProfile.username,
                            fullName = existingProfile.fullName.ifBlank { existingProfile.username.removeSuffix(".link") },
                            avatarPath = existingProfile.avatarUrl ?: "",
                            profession = existingProfile.profession,
                            birthDate = existingProfile.birthDate,
                            isEmailVerified = true,
                            isVerified = true,
                            loginTimestamp = System.currentTimeMillis()
                        )
                        repository.saveUserIdentity(restored)
                        repository.recordLoginSession()
                        SupabaseRealtimeManager.startRealtime(uid, existingProfile.username)
                        viewModelScope.launch {
                            repository.syncAllChatHistory(uid, existingProfile.username)
                        }
                        _isVerifyingOtp.value = false
                        showToast("Welcome back, ${restored.fullName}!")
                    } else {
                        // New user: proceed to identity registration
                        val updated = (existing ?: UserIdentityEntity()).copy(
                            id = 1,
                            supabaseUid = uid,
                            email = email,
                            username = "",
                            fullName = "",
                            avatarPath = "",
                            isEmailVerified = true,
                            isVerified = false,
                            loginTimestamp = 0L
                        )
                        repository.saveUserIdentity(updated)
                        _isVerifyingOtp.value = false
                        showToast("Email verified! Please complete your profile.")
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    _isVerifyingOtp.value = false
                    failedOtpAttempts++
                    if (failedOtpAttempts >= 5) {
                        otpLockoutTime = System.currentTimeMillis() + (120 * 1000L) // 2 minutes lock
                        failedOtpAttempts = 0
                        showToast("5 incorrect attempts. Verification locked for 2 minutes for security.", isError = true)
                    } else {
                        val errMsg = res.exceptionOrNull()?.message ?: "Invalid OTP code"
                        showToast("$errMsg (${5 - failedOtpAttempts} attempts remaining)", isError = true)
                    }
                    withContext(Dispatchers.Main) {
                        onError()
                    }
                }
            } catch (e: Exception) {
                _isVerifyingOtp.value = false
                showToast("Verification failed: ${e.message}", isError = true)
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }

    fun verifyFirebaseOtp(onSuccess: () -> Unit, onError: () -> Unit = {}) {
        verifyOtp(onSuccess, onError)
    }

    // Identity Registration State
    private val _enteredFullName = MutableStateFlow("")
    val enteredFullName: StateFlow<String> = _enteredFullName.asStateFlow()

    private val _enteredAvatarPath = MutableStateFlow<String?>(null)
    val enteredAvatarPath: StateFlow<String?> = _enteredAvatarPath.asStateFlow()

    private val _enteredUsername = MutableStateFlow("")
    val enteredUsername: StateFlow<String> = _enteredUsername.asStateFlow()

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername: StateFlow<Boolean> = _isCheckingUsername.asStateFlow()

    private val _usernameAvailability = MutableStateFlow<Boolean?>(null)
    val usernameAvailability: StateFlow<Boolean?> = _usernameAvailability.asStateFlow()

    private val _selectedProfileType = MutableStateFlow("Private Profile")
    val selectedProfileType: StateFlow<String> = _selectedProfileType.asStateFlow()

    private var usernameCheckJob: Job? = null

    fun updateFullName(input: String) {
        _enteredFullName.value = input
    }

    fun updateAvatarPath(path: String?) {
        _enteredAvatarPath.value = path
    }

    fun updateUsername(input: String) {
        // Alphanumeric only (letters and digits, no special characters or signs)
        val clean = input.lowercase().filter { it.isLetterOrDigit() }
        _enteredUsername.value = clean

        if (clean.length < 3) {
            _usernameAvailability.value = null
            _isCheckingUsername.value = false
            usernameCheckJob?.cancel()
            return
        }

        _isCheckingUsername.value = true
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(250) // Debounce live typing
            val currentIdentity = repository.userIdentity.firstOrNull()
            val currentUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email }?.takeIf { it.isNotBlank() }

            val isTakenRes = SupabaseService.isUsernameTaken(clean, excludeUid = currentUid)
            if (isTakenRes.isSuccess) {
                val isTaken = isTakenRes.getOrDefault(false)
                _usernameAvailability.value = !isTaken
            } else {
                val fullCheck = if (clean.endsWith(".link")) clean else "$clean.link"
                val res = SupabaseService.getProfileByUsername(fullCheck).getOrNull()
                    ?: SupabaseService.getProfileByUsername(clean).getOrNull()
                if (res != null && (currentUid == null || res.id != currentUid)) {
                    _usernameAvailability.value = false
                } else {
                    _usernameAvailability.value = true
                }
            }
            _isCheckingUsername.value = false
        }
    }

    fun selectProfileType(type: String) {
        _selectedProfileType.value = type
    }

    fun completeRegistration(onSuccess: () -> Unit, onError: ((String) -> Unit)? = null) {
        val username = _enteredUsername.value.trim()
        val fullName = _enteredFullName.value.trim()
        val avatarPath = _enteredAvatarPath.value

        if (avatarPath.isNullOrBlank()) {
            val err = "Profile photo is required! Please select a photo."
            showToast(err, isError = true)
            onError?.invoke(err)
            return
        }
        if (fullName.isBlank()) {
            val err = "Please enter your full name"
            showToast(err, isError = true)
            onError?.invoke(err)
            return
        }
        if (username.length < 3) {
            val err = "Username must be at least 3 characters"
            showToast(err, isError = true)
            onError?.invoke(err)
            return
        }

        viewModelScope.launch {
            _isCheckingUsername.value = true
            val suffix = ".link"
            val fullUsername = if (username.endsWith(suffix)) username else username + suffix

            // 1. Synchronous blocking database check before proceeding
            val takenRes = SupabaseService.isUsernameTaken(username)
            val isTaken = takenRes.getOrDefault(false)
            if (isTaken) {
                _usernameAvailability.value = false
                _isCheckingUsername.value = false
                val err = "Username @$fullUsername is already taken! Please choose another."
                withContext(Dispatchers.Main) {
                    showToast(err, isError = true)
                    onError?.invoke(err)
                }
                return@launch
            }

            // 2. Upload local avatar photo to Cloud Storage / Supabase so it's globally visible
            var publicAvatarUrl = avatarPath
            try {
                if (avatarPath.startsWith("/") || avatarPath.startsWith("file://") || avatarPath.startsWith("content://")) {
                    val bytes = if (avatarPath.startsWith("content://")) {
                        getApplication<Application>().contentResolver.openInputStream(android.net.Uri.parse(avatarPath))?.use { it.readBytes() }
                    } else {
                        val path = if (avatarPath.startsWith("file://")) avatarPath.removePrefix("file://") else avatarPath
                        val f = java.io.File(path)
                        if (f.exists()) f.readBytes() else null
                    }

                    if (bytes != null && bytes.isNotEmpty()) {
                        val myUid = repository.userIdentity.firstOrNull()?.let { it.supabaseUid.ifBlank { it.email } } ?: UUID.randomUUID().toString()
                        val fileName = "avatar_${myUid}_${System.currentTimeMillis()}.jpg"
                        val uploadRes = SupabaseService.uploadAvatar(fileName, bytes, "image/jpeg")
                        if (uploadRes.isSuccess) {
                            publicAvatarUrl = uploadRes.getOrNull() ?: avatarPath
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("BitChatViewModel", "Avatar upload exception: ${e.message}")
            }

            val existingIdentity = repository.userIdentity.firstOrNull()
            val existingEmail = _enteredEmail.value.trim().lowercase().ifBlank {
                existingIdentity?.email?.trim()?.lowercase() ?: ""
            }

            // Consolidate full identity update atomically
            val sessionUid = SupabaseService.getCurrentUserId()
            val authUid = if (!sessionUid.isNullOrBlank()) {
                sessionUid
            } else if (!existingIdentity?.supabaseUid.isNullOrBlank() && !existingIdentity!!.supabaseUid.contains("@")) {
                existingIdentity!!.supabaseUid
            } else if (existingEmail.isNotBlank()) {
                val p = SupabaseService.getProfileByEmail(existingEmail).getOrNull()
                p?.id?.ifBlank { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString()
            } else {
                UUID.randomUUID().toString()
            }

            val finalIdentity = (existingIdentity ?: UserIdentityEntity()).copy(
                id = 1,
                supabaseUid = authUid,
                fullName = fullName,
                username = fullUsername,
                avatarPath = publicAvatarUrl,
                profession = "🎓 Student",
                email = existingEmail,
                isEmailVerified = existingEmail.isNotBlank(),
                profileType = "KnotLink",
                isVerified = true,
                loginTimestamp = System.currentTimeMillis()
            )
            repository.saveUserIdentity(finalIdentity)
            repository.recordLoginSession()

            // Save to Supabase remote profiles table
            val supabaseProf = com.example.data.supabase.SupabaseProfile(
                id = authUid,
                email = existingEmail,
                username = fullUsername,
                fullName = fullName,
                avatarUrl = publicAvatarUrl,
                profession = "🎓 Student",
                isVerified = true
            )
            val upsertRes = SupabaseService.upsertProfile(supabaseProf)
            if (upsertRes.isFailure) {
                val err = upsertRes.exceptionOrNull()?.message ?: "Failed to save profile to database"
                Log.e("BitChatViewModel", "Supabase profile upsert error: $err")
                _isCheckingUsername.value = false
                showToast(err, isError = true)
                withContext(Dispatchers.Main) {
                    onError?.invoke(err)
                }
                return@launch
            }

            _isCheckingUsername.value = false
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun refreshPartnerProfile(chatId: String, partnerUid: String) {
        viewModelScope.launch {
            try {
                if (chatId.isBlank() || partnerUid.isBlank() || chatId.startsWith("group_")) return@launch
                
                val currentIdentity = repository.userIdentity.firstOrNull()
                val myUid = currentIdentity?.supabaseUid?.trim()
                val myEmail = currentIdentity?.email?.trim()?.lowercase()
                val myUname = currentIdentity?.username?.trim()?.lowercase()?.removePrefix("@")?.removeSuffix(".link")
                
                val cleanPartner = partnerUid.trim().lowercase().removePrefix("@").removeSuffix(".link")
                if (cleanPartner == "user" || cleanPartner == "contact" || cleanPartner == "me" || cleanPartner == "you" || cleanPartner == "chat_partner") return@launch
                if (cleanPartner == myUid?.lowercase() || cleanPartner == myEmail || cleanPartner == myUname) return@launch

                val prof = SupabaseService.getProfile(partnerUid).getOrNull()
                    ?: SupabaseService.getProfileByUsername(partnerUid).getOrNull()
                if (prof != null && prof.id.isNotBlank()) {
                    // Do not update partner profile if the lookup returned our own profile!
                    if (myUid != null && prof.id.equals(myUid, ignoreCase = true)) return@launch
                    if (myEmail != null && prof.email.equals(myEmail, ignoreCase = true)) return@launch

                    val finalName = prof.fullName.ifBlank { prof.username }
                    val finalAvatar = prof.avatarUrl ?: ""
                    val existing = repository.getChatById(chatId)
                    if (existing != null) {
                        val shouldUpdateName = finalName.isNotBlank() && existing.name != finalName
                        val shouldUpdateAvatar = finalAvatar.isNotBlank() && existing.avatarType != finalAvatar
                        if (shouldUpdateName || shouldUpdateAvatar) {
                            val updated = existing.copy(
                                name = if (shouldUpdateName) finalName else existing.name,
                                avatarType = if (shouldUpdateAvatar) finalAvatar else existing.avatarType
                            )
                            repository.insertChats(listOf(updated))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("BitChatViewModel", "Profile refresh notice: ${e.message}")
            }
        }
    }

    fun changeUsernameHandle(newHandle: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val clean = newHandle.trim().removePrefix("@").removeSuffix(".link")
            val formatted = "$clean.link"
            repository.saveUsernameAndVerify(formatted, "KnotLink")
            val current = repository.userIdentity.firstOrNull()
            if (current != null) {
                repository.updateUserProfile(
                    fullName = current.fullName,
                    avatarPath = current.avatarPath,
                    profession = current.profession,
                    email = current.email,
                    secondaryEmail = current.secondaryEmail,
                    isEmailVerified = current.isEmailVerified,
                    birthDate = current.birthDate
                )
            }
            onSuccess()
        }
    }

    fun updateUserProfile(
        fullName: String,
        avatarPath: String?,
        profession: String = "🎓 Student",
        email: String = "",
        secondaryEmail: String = "",
        isEmailVerified: Boolean = false,
        birthDate: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateUserProfile(
                fullName = fullName,
                avatarPath = avatarPath,
                profession = profession,
                email = email,
                secondaryEmail = secondaryEmail,
                isEmailVerified = isEmailVerified,
                birthDate = birthDate
            )
            onSuccess()
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            SupabaseService.signOut()
            repository.clearAllLocalData()
            _enteredPhoneNumber.value = ""
            _enteredOtpCode.value = ""
            _enteredUsername.value = ""
            _enteredEmail.value = ""
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun setChatFilter(filter: String) {
        _selectedChatFilter.value = filter
    }

    // Deleted Chats (24h Recycle Bin) & Archived Chats
    private val _deletedChats = MutableStateFlow<List<DeletedChatInfo>>(emptyList())
    val deletedChats: StateFlow<List<DeletedChatInfo>> = _deletedChats.asStateFlow()

    private val _archivedChats = MutableStateFlow<List<ChatEntity>>(emptyList())
    val archivedChats: StateFlow<List<ChatEntity>> = _archivedChats.asStateFlow()

    fun archiveChat(chat: ChatEntity) {
        viewModelScope.launch {
            if (!_archivedChats.value.any { it.id == chat.id }) {
                _archivedChats.value = _archivedChats.value + chat
            }
            repository.deleteChat(chat.id)
        }
    }

    fun unarchiveChat(chatId: String) {
        viewModelScope.launch {
            val chatToRestore = _archivedChats.value.find { it.id == chatId }
            if (chatToRestore != null) {
                _archivedChats.value = _archivedChats.value.filter { it.id != chatId }
                repository.insertChats(listOf(chatToRestore))
            }
        }
    }

    fun deleteChat(chat: ChatEntity) {
        viewModelScope.launch {
            if (!_deletedChats.value.any { it.chat.id == chat.id }) {
                _deletedChats.value = _deletedChats.value + DeletedChatInfo(chat, System.currentTimeMillis())
            }
            repository.deleteChat(chat.id)
        }
    }

    fun deleteChatById(chatId: String) {
        viewModelScope.launch {
            val chat = repository.allChats.stateIn(viewModelScope).value.find { it.id == chatId }
            if (chat != null) {
                deleteChat(chat)
            } else {
                repository.deleteChat(chatId)
            }
        }
    }

    fun restoreChat(chatId: String) {
        viewModelScope.launch {
            val deletedInfo = _deletedChats.value.find { it.chat.id == chatId }
            if (deletedInfo != null) {
                _deletedChats.value = _deletedChats.value.filter { it.chat.id != chatId }
                repository.insertChats(listOf(deletedInfo.chat))
            }
        }
    }

    fun permanentlyDeleteChat(chatId: String) {
        _deletedChats.value = _deletedChats.value.filter { it.chat.id != chatId }
    }

    fun clearAllDeletedChats() {
        _deletedChats.value = emptyList()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Active Call State and Call History
    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .map { list ->
            list.map { entity ->
                CallLog(
                    id = entity.id,
                    contactId = entity.contactId,
                    contactName = entity.contactName,
                    callType = entity.callType,
                    direction = entity.direction,
                    timestampMillis = entity.timestampMillis,
                    timeString = entity.timeString,
                    durationSeconds = entity.durationSeconds,
                    avatarType = entity.avatarType
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCallLog(
        contactId: String,
        contactName: String,
        callType: String = "AUDIO",
        direction: String = "OUTGOING",
        durationSeconds: Int = 0
    ) {
        // Generate the id synchronously so a very fast End/Decline can still
        // update the exact log row that was just created.
        val logId = java.util.UUID.randomUUID().toString()
        currentCallLogId = logId
        val timestamp = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val timeStr = sdf.format(java.util.Date(timestamp))

        viewModelScope.launch(Dispatchers.IO) {
            var avatar = ""
            try {
                val profile = SupabaseService.getProfile(contactId).getOrNull()
                    ?: SupabaseService.getProfileByUsername(contactId).getOrNull()
                avatar = profile?.avatarUrl.orEmpty()
            } catch (_: Throwable) {}
            if (avatar.isBlank()) {
                try {
                    avatar = repository.getChatById(contactId)?.avatarType.orEmpty()
                } catch (_: Throwable) {}
            }

            repository.insertCallLog(
                com.example.data.local.CallLogEntity(
                    id = logId,
                    contactId = contactId,
                    contactName = contactName,
                    callType = callType,
                    direction = direction,
                    timestampMillis = timestamp,
                    timeString = timeStr,
                    durationSeconds = durationSeconds,
                    avatarType = avatar.ifBlank { "default" }
                )
            )
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    fun deleteCallLogsByIds(ids: Set<String>) {
        viewModelScope.launch {
            repository.deleteCallLogsByIds(ids.toList())
        }
    }

    fun deleteCallLog(id: String) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
        }
    }

    fun setIncomingCallSession(session: SupabaseCallSession) {
        _incomingCallSession.value = session
    }

    fun handleIncomingCallIntent(callId: String, callerId: String, callerName: String, callType: String, autoAccept: Boolean) {
        viewModelScope.launch {
            var remoteSession: SupabaseCallSession? = null
            if (callId.isNotBlank()) {
                // The app may be starting from a cold process after the notification
                // tap. Give Supabase a few short retries instead of silently dropping
                // the Accept action while the realtime/network stack initializes.
                for (attempt in 0 until 4) {
                    remoteSession = SupabaseService.getCallSession(callId).getOrNull()
                    val status = remoteSession?.status?.uppercase()
                    if (status == "RINGING" || status == "ACCEPTED") break
                    if (attempt < 3) delay(300L * (attempt + 1))
                }
            }

            val liveSession = remoteSession?.takeIf {
                it.status.uppercase() == "RINGING" || it.status.uppercase() == "ACCEPTED"
            }

            // Never fabricate a non-existent session when a real call id was supplied.
            if (callId.isNotBlank() && liveSession == null) {
                Log.w("BitChatViewModel", "Notification accept could not resolve live call session: $callId")
                return@launch
            }

            val session = liveSession ?: SupabaseCallSession(
                id = callId.ifBlank { "call_" + callerName.hashCode() },
                callId = callId.ifBlank { "call_" + callerName.hashCode() },
                callerId = callerId.ifBlank { "caller_" + callerName.hashCode() },
                receiverId = repository.userIdentity.firstOrNull()?.supabaseUid ?: "me",
                callerName = callerName,
                callType = callType.uppercase(),
                status = "RINGING"
            )

            if (autoAccept) {
                val isVideo = session.callType.equals("VIDEO", ignoreCase = true)
                if (com.example.util.PermissionUtils.hasCallPermissions(getApplication<Application>(), isVideo)) {
                    acceptIncomingCall(session)
                } else {
                    // A notification tap cannot launch a runtime permission dialog from
                    // the BroadcastReceiver. Hand the live session to the UI so the
                    // normal permission launcher can complete the accept flow.
                    _incomingCallSession.value = session
                }
            } else if (!_activeCall.value.isActive) {
                _incomingCallSession.value = session
            }
        }
    }

    private var activeCallStatusSyncJob: Job? = null

    private fun startActiveCallStatusSync(callId: String) {
        activeCallStatusSyncJob?.cancel()
        activeCallStatusSyncJob = viewModelScope.launch {
            // Realtime is primary. This low-frequency REST check is a safety net
            // only while this specific call is active. It must continue after
            // CONNECTED because a missed Realtime event otherwise leaves the
            // remote hang-up stuck on this device.
            while (_activeCall.value.isActive && activeCallSessionId == callId) {
                try {
                    val sessionRes = SupabaseService.getCallSession(callId)
                    if (sessionRes.isSuccess) {
                        val session = sessionRes.getOrNull()
                        if (session != null && session.id == callId) {
                            when (session.status.uppercase()) {
                                "CONNECTED" -> {
                                    // DB CONNECTED is only remote signaling state.
                                    // The local timer starts from the WebRTC engine's
                                    // actual ICE CONNECTED/COMPLETED callback.
                                    val connTime = session.connectedAt ?: 0L
                                    if (_activeCall.value.isConnected && connTime > 0L && callConnectTimestamp != connTime) {
                                        callConnectTimestamp = connTime
                                    }
                                }
                                "DECLINED" -> {
                                    handleRemoteCallEnded(session.callerName, isDeclined = true, callId = session.id)
                                    break
                                }
                                "ENDED", "CANCELLED" -> {
                                    handleRemoteCallEnded(session.callerName, isDeclined = false, callId = session.id)
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.w("BitChatViewModel", "Active call status sync error: ${e.message}")
                }
                delay(4000)
            }
        }
    }

    fun acceptIncomingCall(call: SupabaseCallSession) {
        _incomingCallSession.value = null
        NotificationHelper.cancelCallNotification(getApplication<Application>(), call.callerName, call.id)
        activeCallSessionId = call.id
        val isVideo = call.callType.equals("VIDEO", ignoreCase = true)

        val callerName = when {
            call.callerName.isNotBlank() && !call.callerName.startsWith("chat_", ignoreCase = true) && !call.callerName.startsWith("user_", ignoreCase = true) -> call.callerName
            else -> {
                contacts.value.find { it.id == call.callerId }?.name
                    ?: allChats.value.find { it.id == call.callerId }?.name
                    ?: "Incoming Caller"
            }
        }

        callEngine.startCall(
            callId = call.id,
            isCaller = false,
            callType = call.callType,
            isVideo = isVideo,
            peerId = call.callerId,
            peerName = callerName
        )
        _activeCall.value = ActiveCallState(
            isActive = true,
            isConnected = false,
            contactId = call.callerId,
            contactName = callerName,
            contactAvatar = call.callerAvatar ?: "",
            callType = call.callType,
            secondsElapsed = 0,
            isMuted = false,
            isSpeaker = isVideo
        )
        ActiveCallBridge.start(call.id, call.callerId, callerName, call.callType, isConnected = false)
        addCallLog(
            contactId = call.callerId,
            contactName = callerName,
            callType = call.callType,
            direction = "INCOMING",
            durationSeconds = 0
        )
        viewModelScope.launch {
            SupabaseService.updateCallSessionStatus(call.id, "ACCEPTED")
        }

        // Trigger full screen navigation
        _pendingCallNavigationRoute.value = if (isVideo) {
            com.example.navigation.BitChatRoutes.videoCall(call.callerId, callerName)
        } else {
            com.example.navigation.BitChatRoutes.audioCall(call.callerId, callerName)
        }

        startActiveCallStatusSync(call.id)
    }

    fun startCallTimer(connectTimestamp: Long? = null) {
        callTimeoutJob?.cancel()
        callTimeoutJob = null
        if (connectTimestamp != null && connectTimestamp > 0L) {
            callConnectTimestamp = connectTimestamp
        } else if (callConnectTimestamp <= 0L) {
            callConnectTimestamp = System.currentTimeMillis()
        }
        _activeCall.value = _activeCall.value.copy(isConnected = true, callStatus = "CONNECTED")
        ActiveCallBridge.setConnected(callConnectTimestamp)
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive && _activeCall.value.isActive) {
                val elapsed = maxOf(0L, (System.currentTimeMillis() - callConnectTimestamp) / 1000L).toInt()
                _activeCall.value = _activeCall.value.copy(secondsElapsed = elapsed)
                // Keep a native ongoing-call notification alive while the call is
                // connected, so minimizing/backing out of CallActivity never makes
                // the active call disappear from the notification shade.
                NotificationHelper.showOngoingAudioCallNotification(
                    context = getApplication<Application>(),
                    callerName = _activeCall.value.contactName,
                    secondsElapsed = elapsed,
                    callId = activeCallSessionId.orEmpty(),
                    callType = _activeCall.value.callType,
                    peerId = _activeCall.value.contactId
        ,
                    isConnected = true        )
                delay(1000)
            }
        }
    }

    private fun triggerDeclineVibration() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(800, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(800)
                }
            }
        } catch (e: Throwable) {
            Log.w("BitChatViewModel", "Vibration error: ${e.message}")
        }
    }

    fun handleRemoteCallEnded(
        callerName: String? = null,
        isDeclined: Boolean = false,
        callId: String? = null
    ) {
        val expectedCallId = activeCallSessionId
        if (!callId.isNullOrBlank() && !expectedCallId.isNullOrBlank() && callId != expectedCallId) {
            Log.d("BitChatViewModel", "Ignoring stale CALL_ENDED for $callId; active=$expectedCallId")
            return
        }

        Log.i("BitChatViewModel", "Remote termination received: callId=${callId ?: expectedCallId}, declined=$isDeclined")

        _incomingCallSession.value = null
        outgoingCallStartJob?.cancel()
        outgoingCallStartJob = null
        activeCallStatusSyncJob?.cancel()
        activeCallStatusSyncJob = null
        callTimeoutJob?.cancel()
        callTimeoutJob = null

        NotificationHelper.cancelCallNotification(
            getApplication<Application>(),
            callerName ?: _activeCall.value.contactName,
            callId ?: expectedCallId
        )

        // Always tear down WebRTC, even if Compose state has already lost isActive.
        if (callEngine.engineState.value.isCallActive) {
            callEngine.endCall(notifyRemote = false)
        }

        callTimerJob?.cancel()
        callTimerJob = null
        callConnectTimestamp = 0L
        if (isDeclined) triggerDeclineVibration()

        val elapsed = _activeCall.value.secondsElapsed
        val logId = currentCallLogId
        if (logId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.updateCallLogDuration(logId, elapsed)
                } catch (e: Throwable) {
                    Log.w("BitChatViewModel", "Failed to finalize remote call log: ${e.message}")
                }
            }
        }

        ActiveCallBridge.clear(callId ?: expectedCallId)
        _activeCall.value = ActiveCallState(isActive = false)
        activeCallSessionId = null
        currentCallLogId = null
    }
    fun declineIncomingCall(call: SupabaseCallSession) {
        _incomingCallSession.value = null
        NotificationHelper.cancelCallNotification(getApplication<Application>(), call.callerName, call.id)
        viewModelScope.launch {
            SupabaseService.updateCallSessionStatus(call.id, "DECLINED", endedAt = System.currentTimeMillis())
            try {
                FcmPushSender.sendPushToUser(
                    targetUserIdOrName = call.callerId,
                    type = "call_declined",
                    title = "Call Declined",
                    body = "User is currently busy",
                    senderName = call.callerName,
                    chatId = call.id
                )
            } catch (e: Throwable) {
                Log.w("BitChatViewModel", "Error sending call_declined push: ${e.message}")
            }
        }
        addCallLog(
            contactId = call.callerId,
            contactName = call.callerName,
            callType = call.callType,
            direction = "MISSED",
            durationSeconds = 0
        )
    }

    fun dismissIncomingCall() {
        _incomingCallSession.value = null
    }

    fun startCall(contactId: String, contactName: String, callType: String = "AUDIO") {
        outgoingCallStartJob?.cancel()
        callTimerJob?.cancel()
        callConnectTimestamp = 0L
        val isVideo = callType.equals("VIDEO", ignoreCase = true)
        val callId = UUID.randomUUID().toString()
        activeCallSessionId = callId

        _activeCall.value = ActiveCallState(
            isActive = true,
            contactId = contactId,
            contactName = contactName,
            contactAvatar = "",
            callType = callType,
            secondsElapsed = 0,
            isMuted = false,
            isSpeaker = isVideo
        )
        ActiveCallBridge.start(callId, contactId, contactName, callType, isConnected = false)
        addCallLog(contactId = contactId, contactName = contactName, callType = callType, direction = "OUTGOING", durationSeconds = 0)

        // Show the ongoing-call notification immediately, including while the
        // call is still ringing. The contact id is updated to the resolved
        // Supabase UID below once recipient resolution completes.
        NotificationHelper.showOngoingAudioCallNotification(
            context = getApplication<Application>(),
            callerName = contactName,
            secondsElapsed = 0,
            callId = callId,
            callType = callType,
            peerId = contactId
,
            isConnected = false        )

        // Make the call screen the authoritative destination as soon as an outgoing
        // video call becomes active, so the global floating PiP cannot steal the
        // initial presentation while the signaling session is being created.
        _pendingCallNavigationRoute.value = if (isVideo) {
            com.example.navigation.BitChatRoutes.videoCall(contactId, contactName)
        } else {
            com.example.navigation.BitChatRoutes.audioCall(contactId, contactName)
        }

        outgoingCallStartJob?.cancel()
        outgoingCallStartJob = viewModelScope.launch {
            try {
                val currentIdentity = repository.userIdentity.firstOrNull()
                val myUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
                val myName = currentIdentity?.fullName?.ifBlank { currentIdentity.username } ?: "BitChat User"

                if (!isActive || activeCallSessionId != callId || !_activeCall.value.isActive) return@launch

                var resolvedAvatar = currentIdentity?.avatarPath ?: ""
                val resolvedReceiverId = if (contactId.startsWith("chat_") || contactId.startsWith("group_")) {
                    val chat = repository.getChatById(contactId)
                    val uids = chat?.participantUids?.split(",")?.map { it.trim() }?.filter { it != myUid && it.isNotBlank() }
                    uids?.firstOrNull() ?: contactId
                } else {
                    val prof = SupabaseService.getProfile(contactId).getOrNull()
                        ?: SupabaseService.getProfileByUsername(contactId).getOrNull()
                        ?: SupabaseService.getProfileByUsername(contactName).getOrNull()
                    if (prof != null) resolvedAvatar = prof.avatarUrl ?: resolvedAvatar
                    prof?.id?.ifBlank { contactId } ?: contactId
                }

                if (!isActive || activeCallSessionId != callId || !_activeCall.value.isActive) return@launch
                _activeCall.value = _activeCall.value.copy(contactAvatar = resolvedAvatar)

                NotificationHelper.showOngoingAudioCallNotification(
                    context = getApplication<Application>(),
                    callerName = contactName,
                    secondsElapsed = 0,
                    callId = callId,
                    callType = callType,
                    peerId = resolvedReceiverId
                )

                val callerPublicAvatar = currentIdentity?.avatarPath?.takeIf {
                    it.startsWith("http://") || it.startsWith("https://")
                } ?: SupabaseService.getProfile(myUid).getOrNull()?.avatarUrl.orEmpty()

                if (!isActive || activeCallSessionId != callId || !_activeCall.value.isActive) return@launch

                SupabaseService.createCallSession(
                    SupabaseCallSession(
                        callId = callId, callerId = myUid, callerName = myName,
                        callerAvatar = callerPublicAvatar, receiverId = resolvedReceiverId,
                        callType = callType, status = "RINGING"
                    )
                )

                if (!isActive || activeCallSessionId != callId || !_activeCall.value.isActive) {
                    try {
                        SupabaseService.updateCallSessionStatus(callId, "CANCELLED", endedAt = System.currentTimeMillis())
                    } catch (_: Throwable) {}
                    return@launch
                }

                callEngine.startCall(
                    callId = callId,
                    isCaller = true,
                    callType = callType,
                    isVideo = isVideo,
                    peerId = resolvedReceiverId,
                    peerName = contactName
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e("BitChatViewModel", "Outgoing call setup failed", e)
                if (activeCallSessionId == callId) {
                    try {
                        SupabaseService.updateCallSessionStatus(callId, "CANCELLED", endedAt = System.currentTimeMillis())
                    } catch (_: Throwable) {}
                    handleRemoteCallEnded(callerName = contactName, isDeclined = false, callId = callId)
                }
            }
        }
        callTimerJob?.cancel()
        callTimerJob = null
        startActiveCallStatusSync(callId)

        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(30000L)
            if (_activeCall.value.isActive && !_activeCall.value.isConnected) {
                Log.i("BitChatViewModel", "Call ringing timeout of 30 seconds reached!")
                _activeCall.value = _activeCall.value.copy(callStatus = "TIMEOUT")
                
                activeCallStatusSyncJob?.cancel()
                activeCallStatusSyncJob = null

                activeCallSessionId?.let { sId ->
                    SupabaseService.updateCallSessionStatus(sId, "CANCELLED", endedAt = System.currentTimeMillis())
                    
                    val currentIdentity = repository.userIdentity.firstOrNull()
                    val myUid = currentIdentity?.supabaseUid?.ifBlank { currentIdentity.email } ?: "user_me"
                    val resolvedReceiverId = if (contactId.startsWith("chat_") || contactId.startsWith("group_")) {
                        val chat = repository.getChatById(contactId)
                        val uids = chat?.participantUids?.split(",")?.map { it.trim() }?.filter { it != myUid && it.isNotBlank() }
                        uids?.firstOrNull() ?: contactId
                    } else {
                        contactId
                    }
                    try {
                        FcmPushSender.sendPushToUser(
                            targetUserIdOrName = resolvedReceiverId,
                            type = "call_cancelled",
                            title = "Call Missed",
                            body = "Call from ${currentIdentity?.fullName ?: "User"} was not answered",
                            senderName = currentIdentity?.fullName ?: "User",
                            chatId = contactId
                        )
                    } catch (_: Throwable) {}
                }

                delay(2500L)
                callEngine.endCall()
                callTimerJob?.cancel()
                callTimerJob = null
                callConnectTimestamp = 0L
                _activeCall.value = ActiveCallState(isActive = false)
            }
        }
    }

    fun toggleMute() {
        val muted = callEngine.toggleMute()
        _activeCall.value = _activeCall.value.copy(isMuted = muted)
    }

    fun toggleSpeaker() {
        val speaker = callEngine.toggleSpeaker()
        _activeCall.value = _activeCall.value.copy(isSpeaker = speaker)
    }

    fun toggleCamera(): Boolean {
        return callEngine.toggleCamera()
    }

    fun switchCamera(): Boolean {
        return callEngine.switchCamera()
    }

    fun setVideoPausedByScreen(paused: Boolean) {
        callEngine.setVideoPausedByScreen(paused)
    }

    fun setCallQuality(quality: CallQuality) {
        callEngine.setQualityManually(quality)
    }

    fun endCall() {
        activeCallStatusSyncJob?.cancel()
        activeCallStatusSyncJob = null
        callTimeoutJob?.cancel()
        callTimeoutJob = null

        val lastState = _activeCall.value
        val sessId = activeCallSessionId
        val targetContactId = lastState.contactId
        val elapsed = lastState.secondsElapsed
        val logId = currentCallLogId

        NotificationHelper.cancelCallNotification(getApplication<Application>(), lastState.contactName, sessId)

        // Local teardown is immediate; remote termination is persisted separately.
        callEngine.endCall(notifyRemote = false)

        if (sessId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val session = SupabaseService.getCallSession(sessId).getOrNull()
                    val myUid = repository.userIdentity.firstOrNull()?.supabaseUid.orEmpty()
                    val peerId = session?.let {
                        if (it.callerId == myUid) it.receiverId else it.callerId
                    }.orEmpty().ifBlank { targetContactId }

                    var ended = false
                    repeat(3) { attempt ->
                        if (!ended) {
                            ended = SupabaseService.updateCallSessionStatus(
                                sessId, "ENDED", endedAt = System.currentTimeMillis()
                            ).getOrDefault(false)
                            if (!ended && attempt < 2) delay(300L * (attempt + 1))
                        }
                    }

                    // FCM is sent only after the terminal DB transition succeeds or
                    // the row is already terminal, preventing stale notifications.
                    if (ended && peerId.isNotBlank()) {
                        try {
                            FcmPushSender.sendPushToUser(
                                targetUserIdOrName = peerId,
                                type = "call_ended",
                                title = "Call Ended",
                                body = "Call was ended",
                                senderName = lastState.contactName,
                                chatId = sessId
                            )
                        } catch (e: Throwable) {
                            Log.w("BitChatViewModel", "Error sending call_ended push: ${e.message}")
                        }
                    } else if (!ended) {
                        Log.w("BitChatViewModel", "Could not persist terminal ENDED state for call $sessId")
                    }
                } catch (e: Throwable) {
                    Log.e("BitChatViewModel", "Remote call termination failed for $sessId: ${e.message}", e)
                }
            }
        }

        if (lastState.isActive && logId != null) {
            viewModelScope.launch {
                repository.updateCallLogDuration(logId, elapsed)
            }
        }

        callTimerJob?.cancel()
        callTimerJob = null
        callConnectTimestamp = 0L
        _activeCall.value = ActiveCallState(isActive = false)
        activeCallSessionId = null
        currentCallLogId = null
    }
    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private var typingObserverJob: Job? = null
    private var lastTypingSentTime = 0L
    private var typingStopJob: Job? = null

    val chatTypingStatus: StateFlow<Map<String, Boolean>> = SupabaseRealtimeManager.typingUsersByChat
        .map { map ->
            map.mapValues { (_, typers) -> typers.isNotEmpty() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val userPresenceMap: StateFlow<Map<String, Pair<Boolean, Long>>> = SupabaseRealtimeManager.userPresenceMap

    fun observeChatTyping(chats: List<ChatEntity>) {
        // Handled via SupabaseRealtimeManager
    }

    fun setUserTyping(chatId: String, isTyping: Boolean) {
        typingStopJob?.cancel()
        if (isTyping) {
            val now = System.currentTimeMillis()
            if (now - lastTypingSentTime > 2000L) {
                lastTypingSentTime = now
                viewModelScope.launch {
                    repository.setTyping(chatId, true)
                }
            }
            // Auto stop typing after 3.0s of inactivity
            typingStopJob = viewModelScope.launch {
                delay(3000)
                repository.setTyping(chatId, false)
            }
        } else {
            lastTypingSentTime = 0L
            viewModelScope.launch {
                repository.setTyping(chatId, false)
            }
        }
    }

    fun listenTypingStatusForChat(chatId: String) {
        typingObserverJob?.cancel()
        typingObserverJob = viewModelScope.launch {
            SupabaseRealtimeManager.typingUsersByChat.collect { map ->
                val unChatId = if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else "chat_$chatId"
                val activeTypers = mutableListOf<String>()
                map[chatId]?.let { activeTypers.addAll(it) }
                map[unChatId]?.let { activeTypers.addAll(it) }
                map[chatId.lowercase()]?.let { activeTypers.addAll(it) }
                map[unChatId.lowercase()]?.let { activeTypers.addAll(it) }
                _isPartnerTyping.value = activeTypers.isNotEmpty()
            }
        }
    }

    fun setPartnerTyping(isTyping: Boolean, chatId: String? = null) {
        _isPartnerTyping.value = isTyping
    }

    fun toggleMessageReadStatus(messageId: Long, currentIsRead: Boolean) {
        viewModelScope.launch {
            repository.toggleMessageReadStatus(messageId, currentIsRead)
        }
    }

    fun markUserMessagesAsRead(chatId: String) {
        viewModelScope.launch {
            repository.markUserMessagesAsRead(chatId)
        }
    }

    fun markMessagesAsRead(chatId: String, messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            repository.markMessagesAsRead(chatId, messageIds)
        }
    }

    fun observeUserPresence(targetUid: String): Flow<Pair<Boolean, Long>> {
        return repository.observeUserPresence(targetUid)
    }

    fun observeMultiUserPresence(identifiers: List<String>): Flow<Pair<Boolean, Long>> {
        return repository.observeMultiUserPresence(identifiers)
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.setTyping(chatId, isTyping)
        }
    }

    fun observeTyping(chatId: String, targetUid: String): Flow<Boolean> {
        return repository.observeTyping(chatId, targetUid)
    }

    fun retryMessage(message: MessageEntity) {
        viewModelScope.launch {
            try {
                repository.retryMessage(message)
            } catch (e: Exception) {
                Log.e("BitChatViewModel", "Failed to retry message", e)
            }
        }
    }

    private val messagesFlowCache = java.util.concurrent.ConcurrentHashMap<String, StateFlow<List<MessageEntity>>>()
    private val reactionsFlowCache = java.util.concurrent.ConcurrentHashMap<String, Flow<List<ReactionEntity>>>()
    private val pinnedFlowCache = java.util.concurrent.ConcurrentHashMap<String, Flow<List<PinnedMessageEntity>>>()
    private val groupMembersFlowCache = java.util.concurrent.ConcurrentHashMap<String, Flow<List<GroupMemberEntity>>>()

    fun getMessagesForChat(chatId: String): StateFlow<List<MessageEntity>> {
        return messagesFlowCache.computeIfAbsent(chatId) { id ->
            repository.getMessagesForChat(id)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun sendMessage(
        chatId: String,
        text: String,
        replyToMessage: MessageEntity? = null,
        mentionedUids: List<String>? = null
    ) {
        if (text.isBlank()) return
        setUserTyping(chatId, false)
        viewModelScope.launch {
            val userEntity = repository.userIdentity.firstOrNull()
            val fullName = userEntity?.fullName ?: ""
            val username = userEntity?.username ?: ""

            val senderName = if (fullName.isNotBlank()) fullName else if (username.isNotBlank()) username else "Me"

            repository.sendMessage(
                chatId = chatId,
                senderName = senderName,
                text = text,
                isFromUser = true,
                isRead = false,
                replyToMessageId = replyToMessage?.serverMessageId ?: replyToMessage?.clientMessageId,
                replySnippet = replyToMessage?.text?.take(80),
                replySenderName = replyToMessage?.senderName,
                mentionedUids = mentionedUids?.joinToString(",")
            )
        }
    }

    fun editMessage(chatId: String, messageId: Long, serverMessageId: String?, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            repository.editMessage(chatId, messageId, serverMessageId, newText)
        }
    }

    fun deleteMessage(chatId: String, message: MessageEntity, deleteForEveryone: Boolean) {
        viewModelScope.launch {
            repository.deleteMessage(chatId, message.id, message.serverMessageId, deleteForEveryone)
        }
    }

    fun toggleReaction(chatId: String, message: MessageEntity, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(chatId, message.id, message.serverMessageId, emoji)
        }
    }

    fun togglePinMessage(chatId: String, message: MessageEntity, forEveryone: Boolean = false) {
        viewModelScope.launch {
            repository.togglePinMessage(
                chatId = chatId,
                messageId = message.id,
                serverMessageId = message.serverMessageId,
                currentIsPinned = message.isPinned,
                broadcastToOthers = forEveryone
            )
        }
    }

    fun unpinAllMessages(chatId: String) {
        viewModelScope.launch {
            repository.unpinAllMessages(chatId)
        }
    }

    fun forwardMessage(message: MessageEntity, targetChatIds: List<String>) {
        viewModelScope.launch {
            val userEntity = repository.userIdentity.firstOrNull()
            val fullName = userEntity?.fullName ?: ""
            val username = userEntity?.username ?: ""
            val senderName = if (fullName.isNotBlank()) fullName else if (username.isNotBlank()) username else "Me"
            repository.forwardMessage(message, targetChatIds, senderName)
        }
    }

    fun observePinnedMessages(chatId: String): Flow<List<PinnedMessageEntity>> {
        return pinnedFlowCache.computeIfAbsent(chatId) { id ->
            repository.observePinnedMessages(id)
        }
    }

    fun observeReactions(chatId: String): Flow<List<ReactionEntity>> {
        return reactionsFlowCache.computeIfAbsent(chatId) { id ->
            repository.observeReactionsForChat(id)
        }
    }

    fun observeGroupMembers(chatId: String): Flow<List<GroupMemberEntity>> {
        return groupMembersFlowCache.computeIfAbsent(chatId) { id ->
            repository.observeGroupMembers(id)
        }
    }

    fun sendSystemEvent(chatId: String, eventType: String, eventText: String) {
        viewModelScope.launch {
            repository.sendSystemEvent(chatId, eventType, eventText)
        }
    }

    suspend fun uploadMedia(
        chatId: String,
        fileUri: android.net.Uri,
        mimeType: String,
        context: android.content.Context,
        onProgress: (Double) -> Unit = {}
    ): String {
        return repository.uploadMedia(chatId, fileUri, mimeType, context = context, onProgress = onProgress)
    }

    suspend fun resolveScannedUser(rawQrPayload: String): ScannedUserResult {
        val cleanPayload = rawQrPayload.trim()
        if (cleanPayload.isBlank()) return ScannedUserResult.InvalidQr

        // Extract publicId / username / raw identifier
        val publicId = when {
            cleanPayload.startsWith("BITCHAT:USER:", ignoreCase = true) -> {
                cleanPayload.substringAfter("BITCHAT:USER:", "").trim()
            }
            cleanPayload.startsWith("https://bitchat.app/u/", ignoreCase = true) -> {
                cleanPayload.substringAfter("https://bitchat.app/u/", "").trim()
            }
            cleanPayload.startsWith("bitchat://user/", ignoreCase = true) -> {
                cleanPayload.substringAfter("bitchat://user/", "").trim()
            }
            cleanPayload.startsWith("BC-", ignoreCase = true) || cleanPayload.startsWith("usr_", ignoreCase = true) || cleanPayload.startsWith("@") -> {
                cleanPayload.removePrefix("@")
            }
            cleanPayload.length in 3..64 && !cleanPayload.contains(" ") -> {
                cleanPayload
            }
            else -> return ScannedUserResult.InvalidQr
        }

        if (publicId.isBlank()) return ScannedUserResult.InvalidQr

        val profile = repository.findUserByPublicIdentity(publicId)
        if (profile != null) {
            val scannedUser = ScannedUser(
                publicId = profile.publicId.ifBlank { profile.uid },
                name = profile.displayName.ifBlank { profile.username },
                username = if (profile.username.startsWith("@")) profile.username else "@${profile.username}",
                bio = profile.bio,
                profession = profile.profession,
                mutualGroups = profile.mutualGroups.ifEmpty { listOf("BitChat Network") },
                avatarType = profile.avatarType,
                uid = profile.uid
            )
            return ScannedUserResult.Success(scannedUser)
        }

        return ScannedUserResult.UserNotFound
    }

    suspend fun getOrCreateChatForScannedUser(scannedUser: ScannedUser): ChatEntity {
        val targetUid = scannedUser.uid.ifBlank { scannedUser.publicId }
        val targetName = scannedUser.name.ifBlank { "User" }
        return repository.initChat(
            targetUid = targetUid,
            targetName = targetName,
            avatarType = scannedUser.avatarType
        )
    }

    // Muted Chat IDs State
    private val _mutedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val mutedChatIds: StateFlow<Set<String>> = _mutedChatIds.asStateFlow()

    fun toggleMuteChat(chatId: String) {
        val current = _mutedChatIds.value.toMutableSet()
        if (current.contains(chatId)) {
            current.remove(chatId)
        } else {
            current.add(chatId)
        }
        _mutedChatIds.value = current
    }

    // Group Members and Admin Approval State
    private val _groupMembersMap = MutableStateFlow<Map<String, List<GroupMember>>>(emptyMap())
    val groupMembersMap: StateFlow<Map<String, List<GroupMember>>> = _groupMembersMap.asStateFlow()

    private val _groupAdminApprovalMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val groupAdminApprovalMap: StateFlow<Map<String, Boolean>> = _groupAdminApprovalMap.asStateFlow()

    fun getMembersForGroup(groupId: String, defaultMembers: List<String> = emptyList()): List<GroupMember> {
        val existing = _groupMembersMap.value[groupId]
        if (existing != null) return existing

        val initialList = mutableListOf(
            GroupMember(id = "user_you", name = "You", isOwner = true, isAdmin = true)
        )
        defaultMembers.forEachIndexed { index, name ->
            initialList.add(
                GroupMember(
                    id = "member_${groupId}_$index",
                    name = name,
                    isAdmin = index == 0 // First added member is sub-admin by default
                )
            )
        }
        _groupMembersMap.value = _groupMembersMap.value + (groupId to initialList)
        return initialList
    }

    fun addMemberToGroup(groupId: String, memberName: String) {
        val currentMembers = getMembersForGroup(groupId).toMutableList()
        if (currentMembers.none { it.name.equals(memberName, true) }) {
            currentMembers.add(
                GroupMember(
                    id = "member_${groupId}_${System.currentTimeMillis()}",
                    name = memberName
                )
            )
            _groupMembersMap.value = _groupMembersMap.value + (groupId to currentMembers)
        }
    }

    fun removeMemberFromGroup(groupId: String, memberId: String) {
        val currentMembers = getMembersForGroup(groupId).toMutableList()
        currentMembers.removeAll { it.id == memberId }
        _groupMembersMap.value = _groupMembersMap.value + (groupId to currentMembers)
    }

    fun toggleMemberAdminRole(groupId: String, memberId: String) {
        val currentMembers = getMembersForGroup(groupId).map { member ->
            if (member.id == memberId && !member.isOwner) {
                member.copy(isAdmin = !member.isAdmin)
            } else {
                member
            }
        }
        _groupMembersMap.value = _groupMembersMap.value + (groupId to currentMembers)
    }

    fun updateMemberPermission(groupId: String, memberId: String, permissionType: String, enabled: Boolean) {
        val currentMembers = getMembersForGroup(groupId).map { member ->
            if (member.id == memberId) {
                when (permissionType) {
                    "deleteMessages" -> member.copy(canDeleteMessages = enabled)
                    "pinMessages" -> member.copy(canPinMessages = enabled)
                    "changeInfo" -> member.copy(canChangeGroupInfo = enabled)
                    "inviteMembers" -> member.copy(canInviteMembers = enabled)
                    "muteMembers" -> member.copy(canMuteMembers = enabled)
                    else -> member
                }
            } else {
                member
            }
        }
        _groupMembersMap.value = _groupMembersMap.value + (groupId to currentMembers)
    }

    fun updateMemberNickname(groupId: String, memberId: String, nickname: String) {
        val currentMembers = getMembersForGroup(groupId).map { member ->
            if (member.id == memberId) {
                member.copy(nickname = nickname.trim())
            } else {
                member
            }
        }
        _groupMembersMap.value = _groupMembersMap.value + (groupId to currentMembers)
    }

    fun toggleGroupAdminApproval(groupId: String) {
        val current = _groupAdminApprovalMap.value[groupId] ?: false
        _groupAdminApprovalMap.value = _groupAdminApprovalMap.value + (groupId to !current)
    }

    fun updateGroupName(chatId: String, newName: String) {
        viewModelScope.launch {
            val cleanName = newName.trim().take(60)
            if (cleanName.isNotBlank()) {
                val formattedName = if (cleanName.startsWith("[Group]")) cleanName else "[Group] $cleanName"
                val all = repository.allChats.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value
                val existing = all.find { it.id == chatId }
                if (existing != null) {
                    val updated = existing.copy(name = formattedName)
                    repository.insertChats(listOf(updated))
                }
            }
        }
    }

    fun updateGroupAvatar(chatId: String, newAvatarPath: String) {
        viewModelScope.launch {
            val all = repository.allChats.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value
            val existing = all.find { it.id == chatId }
            if (existing != null) {
                val updated = existing.copy(avatarType = newAvatarPath)
                repository.insertChats(listOf(updated))
            }
        }
    }

    fun leaveOrDeleteGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChat(groupId)
            onSuccess()
        }
    }

    fun createGroupChat(
        groupName: String,
        avatarPathOrType: String,
        memberNames: List<String>,
        onSuccess: (ChatEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val groupId = "group_${System.currentTimeMillis()}"
            val cleanName = if (groupName.startsWith("[Group]")) groupName else "[Group] $groupName"
            val membersText = (listOf("You") + memberNames).joinToString(", ")
            val newGroupChat = ChatEntity(
                id = groupId,
                name = cleanName,
                lastMessage = "Group created with $membersText",
                timeString = "Just now",
                unreadCount = 0,
                isOnline = true,
                category = "Personal",
                avatarType = if (avatarPathOrType.isBlank()) "group_default" else avatarPathOrType,
                chatType = "GROUP"
            )
            repository.insertChats(listOf(newGroupChat))
            onSuccess(newGroupChat)
        }
    }

    suspend fun createGroupChat(
        title: String,
        description: String?,
        avatarUrl: String?,
        memberUids: List<String>
    ): String {
        return repository.createGroupChat(title, description, avatarUrl, memberUids)
    }

    suspend fun getChatById(chatId: String) = repository.getChatById(chatId)

    suspend fun leaveGroup(chatId: String) = repository.leaveGroup(chatId)

    suspend fun addMemberToGroupChat(chatId: String, newUid: String) {
        repository.addMemberToGroup(chatId, newUid)
    }

    suspend fun removeMemberFromGroupChat(chatId: String, targetUid: String) {
        repository.removeMemberFromGroup(chatId, targetUid)
    }

    suspend fun promoteAdmin(chatId: String, targetUid: String) {
        repository.promoteAdmin(chatId, targetUid)
    }

    suspend fun demoteAdmin(chatId: String, targetUid: String) {
        repository.demoteAdmin(chatId, targetUid)
    }

    suspend fun transferOwnership(chatId: String, newOwnerUid: String) {
        repository.transferOwnership(chatId, newOwnerUid)
    }

    suspend fun updateGroupDetails(
        chatId: String,
        name: String,
        description: String?,
        avatarUrl: String?,
        permissionsJson: String?
    ) {
        repository.updateGroupDetails(chatId, name, description, avatarUrl, permissionsJson)
    }

    fun toggleMuteChat(chatId: String, currentIsMuted: Boolean) {
        viewModelScope.launch {
            repository.toggleMuteChat(chatId, !currentIsMuted)
            showToast(if (!currentIsMuted) "Notifications muted for this group" else "Notifications unmuted")
        }
    }

    suspend fun reportMemberOrGroup(
        chatId: String,
        targetUid: String?,
        reason: String,
        details: String?
    ) {
        repository.reportMemberOrGroup(chatId, targetUid, reason, details)
    }

    suspend fun uploadMedia(
        chatId: String,
        fileUri: android.net.Uri,
        mimeType: String,
        context: android.content.Context
    ): String {
        return repository.uploadMedia(chatId, fileUri, mimeType, context = context)
    }
}

sealed interface ScannedUserResult {
    data class Success(val user: ScannedUser) : ScannedUserResult
    object UserNotFound : ScannedUserResult
    object InvalidQr : ScannedUserResult
}

data class ScannedUser(
    val publicId: String,
    val name: String,
    val username: String,
    val bio: String,
    val profession: String,
    val mutualGroups: List<String> = emptyList(),
    val avatarType: String = "default",
    val uid: String = ""
)

data class ActiveCallState(
    val isActive: Boolean = false,
    val isConnected: Boolean = false,
    val contactId: String = "",
    val contactName: String = "Alex Rivera",
    val contactAvatar: String = "",
    val callType: String = "AUDIO",
    val secondsElapsed: Int = 0,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val callStatus: String = "RINGING" // "RINGING", "CONNECTED", "BUSY", "TIMEOUT", "ENDED"
)

data class DeletedChatInfo(
    val chat: ChatEntity,
    val deletedAtMillis: Long = System.currentTimeMillis()
)

data class GroupMember(
    val id: String,
    val name: String,
    val nickname: String = "",
    val isAdmin: Boolean = false,
    val isOwner: Boolean = false,
    val avatarType: String = "default",
    val canDeleteMessages: Boolean = true,
    val canPinMessages: Boolean = true,
    val canChangeGroupInfo: Boolean = true,
    val canInviteMembers: Boolean = true,
    val canMuteMembers: Boolean = true
)

data class GroupJoinRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val requestTime: String = "Just now"
)

data class CallLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val contactId: String,
    val contactName: String,
    val callType: String = "AUDIO", // "AUDIO" or "VIDEO"
    val direction: String = "OUTGOING", // "INCOMING", "OUTGOING", "MISSED"
    val timestampMillis: Long = System.currentTimeMillis(),
    val timeString: String = "Just now",
    val durationSeconds: Int = 0,
    val avatarType: String = "default"
)

data class ModernToastData(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

