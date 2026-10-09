package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object SupabaseRealtimeManager {
    private const val TAG = "SupabaseRealtime"

    private val _incomingMessages = MutableSharedFlow<SupabaseMessage>(extraBufferCapacity = 128)
    val incomingMessages: SharedFlow<SupabaseMessage> = _incomingMessages.asSharedFlow()

    private val _messageStatusUpdates = MutableSharedFlow<SupabaseMessage>(extraBufferCapacity = 128)
    val messageStatusUpdates: SharedFlow<SupabaseMessage> = _messageStatusUpdates.asSharedFlow()

    /** Server message ids that were deleted for everyone (row is gone; no tombstone). */
    private val _deletedMessageIds = MutableSharedFlow<String>(extraBufferCapacity = 128)
    val deletedMessageIds: SharedFlow<String> = _deletedMessageIds.asSharedFlow()

    private val _incomingCalls = MutableSharedFlow<SupabaseCallSession>(extraBufferCapacity = 32)
    val incomingCalls: SharedFlow<SupabaseCallSession> = _incomingCalls.asSharedFlow()

    private val _callSessionUpdates = MutableSharedFlow<SupabaseCallSession>(extraBufferCapacity = 64)
    val callSessionUpdates: SharedFlow<SupabaseCallSession> = _callSessionUpdates.asSharedFlow()

    private val _typingUsersByChat = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val typingUsersByChat: StateFlow<Map<String, List<String>>> = _typingUsersByChat.asStateFlow()

    private val _userPresenceMap = MutableStateFlow<Map<String, Pair<Boolean, Long>>>(emptyMap())
    val userPresenceMap: StateFlow<Map<String, Pair<Boolean, Long>>> = _userPresenceMap.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null
    private var heartbeatJob: Job? = null
    private var currentChatId: String? = null
    private var currentUserId: String? = null
    private var currentUsername: String? = null
    private var currentUserEmail: String? = null

    private val typingExpiryJobs = ConcurrentHashMap<String, Job>()

    private var webSocket: WebSocket? = null
    private val wsClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    init {
        startSyncLoop()
    }

    fun start() {
        startSyncLoop()
        connectWebSocket()
    }

    fun getCurrentUserId(): String? = currentUserId

    fun broadcastMessageMutation(message: SupabaseMessage, mutation: String) {
        val socket = webSocket ?: return
        try {
            val inner = message.toJson().apply {
                put("event", "message_mutation")
                put("mutation", mutation)
            }
            val payload = JSONObject().apply {
                put("type", "broadcast")
                put("event", "message_mutation")
                put("payload", inner)
            }
            val envelope = JSONObject().apply {
                put("topic", "realtime:public")
                put("event", "broadcast")
                put("payload", payload)
                put("ref", JSONObject.NULL)
            }
            socket.send(envelope.toString())
        } catch (e: Throwable) {
            Log.w(TAG, "message mutation broadcast failed: " + e.message)
        }
    }

    fun startRealtime(userId: String, username: String? = null, email: String? = null) {
        val identityChanged = currentUserId != userId ||
            (!username.isNullOrBlank() && currentUsername != username) ||
            (!email.isNullOrBlank() && currentUserEmail != email)
        currentUserId = userId
        if (!username.isNullOrBlank()) currentUsername = username
        if (!email.isNullOrBlank()) currentUserEmail = email
        startSyncLoop()
        if (identityChanged || webSocket == null) {
            connectWebSocket()
        }
    }

    fun onAppForeground() {
        val uid = currentUserId
        val uname = currentUsername
        if (!uid.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
                try {
                    SupabaseService.updatePresence(uid, true, force = true)
                } catch (_: Exception) {}
            }
        }
        startSyncLoop()
        connectWebSocket()
    }

    fun onAppBackground() {
        val uid = currentUserId
        val uname = currentUsername
        if (!uid.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
                try {
                    SupabaseService.updatePresence(uid, false, force = true)
                } catch (_: Exception) {}
            }
        }
        syncJob?.cancel()
        syncJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "App paused")
        webSocket = null
    }

    fun stopRealtime() {
        onAppBackground()
    }

    fun sendTypingBroadcast(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        val socket = webSocket ?: return
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val payload = JSONObject().apply {
                put("chat_id", chatId)
                put("user_id", userId)
                put("user_name", userName)
                put("is_typing", isTyping)
                put("event", "typing")
            }
            val envelope = JSONObject().apply {
                put("topic", "realtime:public")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("event", "typing")
                    put("payload", payload)
                })
                put("ref", JSONObject.NULL)
            }
            socket.send(envelope.toString())
        } catch (e: Throwable) { Log.w(TAG, "typing broadcast failed: " + e.message) }
    }

    fun broadcastNewMessage(message: SupabaseMessage) {
        val socket = webSocket ?: return
        try {
            val inner = message.toJson().apply { put("event", "new_message") }
            val envelope = JSONObject().apply {
                put("topic", "realtime:public")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("event", "new_message")
                    put("payload", inner)
                })
                put("ref", JSONObject.NULL)
            }
            socket.send(envelope.toString())
        } catch (e: Throwable) { Log.w(TAG, "new message broadcast failed: " + e.message) }
    }

    private fun clearTypingForChat(chatId: String) {
        if (chatId.isBlank()) return
        val map = _typingUsersByChat.value.toMutableMap()
        map.remove(chatId)
        _typingUsersByChat.value = map
        typingExpiryJobs.remove(chatId)?.cancel()
    }

    private fun handleTypingUpdate(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        if (chatId.isBlank() || userId.isBlank() || isFromMe(userId, userName)) return
        val map = _typingUsersByChat.value.toMutableMap()
        val users = map[chatId].orEmpty().toMutableList()
        if (isTyping) {
            if (!users.any { it.equals(userId, ignoreCase = true) }) users += userId
            map[chatId] = users
            _typingUsersByChat.value = map
            typingExpiryJobs.remove(chatId)?.cancel()
            typingExpiryJobs[chatId] = scope.launch {
                delay(5000L)
                clearTypingForChat(chatId)
            }
        } else {
            users.removeAll { it.equals(userId, ignoreCase = true) }
            if (users.isEmpty()) map.remove(chatId) else map[chatId] = users
            _typingUsersByChat.value = map
        }
    }

    fun setCurrentActiveChat(chatId: String?) {
        currentChatId = chatId
    }

    private fun isFromMe(userId: String?, userName: String?): Boolean {
        val myUid = currentUserId?.trim()?.lowercase()
        val uId = userId?.trim()?.lowercase()
        // Internal message ownership is UUID-only. Username/email/name are display/search
        // identifiers and must never be accepted as message sender identity.
        return !myUid.isNullOrBlank() &&
            myUid != "user_me" &&
            myUid != "null" &&
            uId == myUid
    }

    private fun connectWebSocket() {
        try {
            try {
                webSocket?.close(1000, "Reconnecting/Renewing")
            } catch (_: Exception) {}
            webSocket = null

            val url = SupabaseConfig.REALTIME_WS_URL
            val request = Request.Builder().url(url).build()

            webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected successfully to Supabase Realtime")

                    // Join realtime channel with broadcast & postgres_changes config
                    val payload = JSONObject().apply {
                        val config = JSONObject().apply {
                            put("broadcast", JSONObject().apply {
                                put("self", false)
                                put("ack", false)
                            })
                            val changes = JSONArray().apply {
                                val realtimeUid = currentUserId
                                if (!realtimeUid.isNullOrBlank()) {
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", SupabaseConfig.TABLE_MESSAGES)
                                        put("filter", "recipient_id=eq.$realtimeUid")
                                    })
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", SupabaseConfig.TABLE_MESSAGES)
                                        put("filter", "sender_id=eq.$realtimeUid")
                                    })
                                }
                                val callUid = currentUserId
                                if (!callUid.isNullOrBlank()) {
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", SupabaseConfig.TABLE_CALL_SESSIONS)
                                        put("filter", "caller_id=eq.$callUid")
                                    })
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", SupabaseConfig.TABLE_CALL_SESSIONS)
                                        put("filter", "callee_id=eq.$callUid")
                                    })
                                }
                                put(JSONObject().apply {
                                    put("event", "INSERT")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_MESSAGE_DELETIONS)
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_TYPING_STATUS)
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_PRESENCE)
                                })
                            }
                            put("postgres_changes", changes)
                        }
                        put("config", config)
                        val realtimeToken = SupabaseService.getAccessToken()
                        if (realtimeToken.isNotBlank() && realtimeToken != SupabaseConfig.ANON_KEY) {
                            put("access_token", realtimeToken)
                        }
                    }

                    val joinMsg = JSONObject().apply {
                        put("topic", "realtime:public")
                        put("event", "phx_join")
                        put("payload", payload)
                        put("ref", "1")
                    }
                    webSocket.send(joinMsg.toString())

                    // Start heartbeat loop
                    startHeartbeat(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val event = json.optString("event")
                        val payload = json.optJSONObject("payload")

                        // 1. Handle Broadcast typing & instant new_message events
                        val isBroadcast = event == "broadcast" || event == "typing" || event == "new_message" || event == "message_mutation" ||
                            (payload != null && (payload.optString("type") == "broadcast" ||
                                payload.optString("event") == "typing" ||
                                payload.optString("event") == "new_message" ||
                                payload.optString("event") == "message_mutation"))

                        if (isBroadcast) {
                            val innerPayload = payload?.optJSONObject("payload") ?: payload ?: json
                            val subEvent = payload?.optString("event", innerPayload.optString("event", ""))

                            if (subEvent == "message_mutation" || event == "message_mutation" ||
                                innerPayload.optString("event") == "message_mutation") {
                                val msg = SupabaseMessage.fromJson(innerPayload)
                                val myUid = currentUserId
                                val myName = currentUsername
                                val isFromMe = isFromMe(msg.senderId, msg.senderName)
                                val isForMe = isMessageForUser(msg, myUid, myName, currentUserEmail)
                                if (!isFromMe && isForMe && msg.id.isNotBlank()) {
                                    scope.launch {
                                        _incomingMessages.emit(msg)
                                        if (msg.status.equals("DELIVERED", ignoreCase = true) || msg.status.equals("READ", ignoreCase = true) || msg.isRead || msg.isEdited) {
                                            _messageStatusUpdates.emit(msg)
                                        }
                                    }
                                }
                            } else if (subEvent == "new_message" || event == "new_message" || innerPayload.has("text")) {
                                val msg = SupabaseMessage.fromJson(innerPayload)
                                val myUid = currentUserId
                                val myName = currentUsername
                                val myClean = myName?.trim()?.removePrefix("@")?.lowercase()?.removeSuffix(".link")

                                val isFromMe = isFromMe(msg.senderId, msg.senderName)

                                val isForMe = isMessageForUser(msg, myUid, myName, currentUserEmail)

                                if (!isFromMe && isForMe) {
                                    clearTypingForChat(msg.chatId)
                                    if (msg.senderId.isNotBlank()) {
                                        clearTypingForChat(msg.senderId)
                                    }
                                    scope.launch { _incomingMessages.emit(msg) }
                                }
                            } else {
                                val chatId = innerPayload.optString("chat_id", innerPayload.optString("chatId", ""))
                                val userId = innerPayload.optString("user_id", innerPayload.optString("userId", ""))
                                val userName = innerPayload.optString("user_name", innerPayload.optString("userName", "Someone"))
                                val isTyping = if (innerPayload.has("is_typing")) {
                                    innerPayload.optBoolean("is_typing", false)
                                } else if (innerPayload.has("isTyping")) {
                                    innerPayload.optBoolean("isTyping", false)
                                } else {
                                    event == "typing" || payload?.optString("event") == "typing"
                                }

                                if (chatId.isNotBlank() && !isFromMe(userId, userName)) {
                                    handleTypingUpdate(chatId, userId, userName, isTyping)
                                }
                            }
                        }

                        // 2. Handle Postgres changes
                        if (payload != null && (event == "postgres_changes" || event == "INSERT")) {
                            val data = payload.optJSONObject("data") ?: payload
                            val record = data.optJSONObject("record") ?: payload.optJSONObject("record")
                            val table = data.optString("table", payload.optString("table"))
                            if (record != null) {
                                when (table) {
                                     SupabaseConfig.TABLE_MESSAGES -> {
                                        val msg = SupabaseMessage.fromJson(record)
                                        val myUid = currentUserId
                                        val myName = currentUsername
                                        val myClean = myName?.trim()?.removePrefix("@")?.lowercase()?.removeSuffix(".link")

                                        val isFromMe = isFromMe(msg.senderId, msg.senderName)

                                        val isForMe = isMessageForUser(msg, myUid, myName, currentUserEmail)
                                        val changeType = data.optString("type", "INSERT")
                                        val isUpdateOrDelete = changeType == "UPDATE" || changeType == "DELETE"

                                        val shouldEmit = if (isUpdateOrDelete) {
                                            isForMe || isFromMe
                                        } else {
                                            !isFromMe && isForMe
                                        }

                                        if (shouldEmit) {
                                            // When sender sent a message, instantly clear their typing state!
                                            clearTypingForChat(msg.chatId)
                                            if (msg.senderId.isNotBlank()) {
                                                clearTypingForChat(msg.senderId)
                                            }
                                            scope.launch { _incomingMessages.emit(msg) }
                                        }
                                        if (msg.id.isNotBlank() && msg.status.isNotBlank()) {
                                            scope.launch { _messageStatusUpdates.emit(msg) }
                                        }
                                    }
                                    SupabaseConfig.TABLE_MESSAGE_DELETIONS -> {
                                        val deletedId = record.optString("message_id", "")
                                        if (deletedId.isNotBlank()) {
                                            scope.launch { _deletedMessageIds.emit(deletedId) }
                                        }
                                    }
                                    SupabaseConfig.TABLE_CALL_SESSIONS -> {
                                        val call = SupabaseCallSession.fromJson(record)
                                        if (call.receiverId.equals(currentUserId, ignoreCase = true)) {
                                            scope.launch { _incomingCalls.emit(call) }
                                        }
                                        scope.launch { _callSessionUpdates.emit(call) }
                                    }
                                    SupabaseConfig.TABLE_TYPING_STATUS -> {
                                        val typing = SupabaseTyping.fromJson(record)
                                        if (!isFromMe(typing.userId, typing.userName)) {
                                            handleTypingUpdate(typing.chatId, typing.userId, typing.userName, typing.isTyping)
                                        }
                                    }
                                    SupabaseConfig.TABLE_PRESENCE -> {
                                        handlePresenceUpdate(record)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling WS message: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (this@SupabaseRealtimeManager.webSocket === webSocket) this@SupabaseRealtimeManager.webSocket = null
                    Log.w(TAG, "WebSocket connection failed: ${t.message}. Reconnecting in 5s...")
                    scope.launch {
                        delay(5000)
                        if (currentUserId != null) {
                            connectWebSocket()
                        }
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (this@SupabaseRealtimeManager.webSocket === webSocket) this@SupabaseRealtimeManager.webSocket = null
                    Log.d(TAG, "WebSocket closed: $reason")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to connect WebSocket: ${e.message}")
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            var ref = 100
            while (isActive) {
                delay(25000)
                try {
                    val hb = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", "${ref++}")
                    }
                    ws.send(hb.toString())
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private var loopCounter = 0L

    private fun startSyncLoop() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            while (isActive) {
                try {
                    val uid = currentUserId
                    val uname = currentUsername
                    if (!uid.isNullOrBlank()) {
                        SupabaseService.updatePresence(uid, true)

                        // REST is only a degraded fallback while Realtime is unavailable.
                        if (webSocket == null) {
                            // Do not poll the entire message set every 30 seconds. That was
                            // an avoidable egress source. Missed messages are recovered by
                            // authenticated bootstrap/active-chat sync and the FCM wake path.
                            SupabaseService.getIncomingCalls(uid).getOrNull()?.forEach { _incomingCalls.emit(it) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in sync loop: ${e.message}")
                }
                delay(30_000L)
            }
        }
    }

    // Realtime transport intentionally does not deduplicate messages. Room is the
    // durable idempotency boundary, so a reconnect/duplicate event can never cause
    // a message to be dropped before it is persisted or acknowledged.

    // Legacy polling helper retained for compatibility. Message identity is now
    // reconciled in Room, never by an in-memory transport cache.
    private fun isDuplicateAndTrack(msg: SupabaseMessage): Boolean = false

    private val lastSeenMessageTexts = java.util.concurrent.ConcurrentHashMap<String, String>()

    private suspend fun handlePolledMessage(msg: SupabaseMessage, uid: String, uname: String?) {
        val isFromMe = msg.senderId.equals(uid, ignoreCase = true)

        val previousText = lastSeenMessageTexts[msg.id]
        val isTextChanged = previousText != null && previousText != msg.text
        lastSeenMessageTexts[msg.id] = msg.text

        if (isFromMe) {
            if (msg.id.isNotBlank() && msg.status.isNotBlank()) {
                _messageStatusUpdates.emit(msg)
            }
            if (isTextChanged || msg.isEdited || msg.text.startsWith("POLL:")) {
                _incomingMessages.emit(msg)
            }
        } else if (isMessageForUser(msg, uid, uname, currentUserEmail)) {
            val isDuplicate = isDuplicateAndTrack(msg)
            if (!isDuplicate || isTextChanged || msg.isEdited || msg.text.startsWith("POLL:")) {
                _incomingMessages.emit(msg)
            }
            if (msg.status == "SENT") {
                SupabaseService.markMessageDelivered(msg.id)
            }
        }
    }

    fun isMessageForUser(
        msg: SupabaseMessage,
        uid: String?,
        uname: String?,
        email: String? = null,
        knownChatIds: Set<String> = emptySet()
    ): Boolean {
        val myUid = uid?.trim().orEmpty()
        if (myUid.isBlank()) return false
        if (msg.senderId.equals(myUid, ignoreCase = true)) return false

        val recipient = msg.receiverId.trim()
        if (recipient.equals(myUid, ignoreCase = true)) return true

        // Group/system compatibility: direct messages remain UUID-only.
        val chatId = msg.chatId.trim()
        if (msg.messageType.equals("SYSTEM", ignoreCase = true) ||
            msg.messageType.equals("SYSTEM_EVENT", ignoreCase = true)) {
            return knownChatIds.contains(chatId)
        }
        return false
    }

    private fun handlePresenceUpdate(record: JSONObject) {
        try {
            val userId = record.optString("user_id", "").trim()
            if (userId.isBlank()) return
            val isOnline = record.optBoolean("is_online", false)
            val lastSeen = parseIsoTimestamp(record.optString("last_seen_at", ""))
            val recent = isOnline && lastSeen > 0L && System.currentTimeMillis() - lastSeen <= 30_000L
            val map = _userPresenceMap.value.toMutableMap()
            map[userId] = Pair(recent, lastSeen)
            _userPresenceMap.value = map
        } catch (e: Exception) {
            Log.e(TAG, "Error updating presence from Realtime: ${e.message}")
        }
    }

    private fun parseIsoTimestamp(value: String): Long {
        val raw = value.trim()
        if (raw.isBlank()) return 0L
        return try {
            val base = raw.take(19)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            var result = sdf.parse(base)?.time ?: 0L
            val dot = raw.indexOf('.')
            if (dot >= 0) {
                val digits = raw.substring(dot + 1).takeWhile { it.isDigit() }.take(3)
                if (digits.isNotEmpty()) result += digits.padEnd(3, '0').toLong()
            }
            result
        } catch (_: Exception) {
            0L
        }
    }

}
