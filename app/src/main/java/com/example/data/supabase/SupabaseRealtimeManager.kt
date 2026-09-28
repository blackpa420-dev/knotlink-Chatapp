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
    private val processedMessageIds = ConcurrentHashMap.newKeySet<String>()

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
                    if (!uname.isNullOrBlank() && uname != uid) {
                        SupabaseService.updatePresence(uname, true, force = true)
                    }
                    val presenceRes = SupabaseService.getAllUserPresence()
                    if (presenceRes.isSuccess) {
                        _userPresenceMap.value = presenceRes.getOrNull() ?: emptyMap()
                    }
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
                    SupabaseService.updatePresence(uid, false)
                    if (!uname.isNullOrBlank() && uname != uid) {
                        SupabaseService.updatePresence(uname, false)
                    }
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

    fun setCurrentActiveChat(chatId: String?) {
        currentChatId = chatId
    }

    private fun isFromMe(userId: String?, userName: String?): Boolean {
        val myUid = currentUserId?.trim()?.lowercase()
        val myUname = currentUsername?.trim()?.lowercase()
        val myClean = myUname?.removePrefix("@")?.removeSuffix(".link")

        val uId = userId?.trim()?.lowercase()
        val uName = userName?.trim()?.lowercase()
        val uClean = uName?.removePrefix("@")?.removeSuffix(".link")

        if (!myUid.isNullOrBlank() && myUid != "user_me" && myUid != "null") {
            if (uId == myUid) return true
        }
        if (!myUname.isNullOrBlank() && myUname != "me" && myUname != "someone" && myUname != "user" && myUname != "null") {
            if (uId == myUname || uName == myUname) return true
        }
        if (!myClean.isNullOrBlank() && myClean != "me" && myClean != "someone" && myClean != "user" && myClean != "null") {
            if (uClean == myClean || uId == myClean || uName == myClean) return true
        }
        return false
    }

    private fun connectWebSocket() {
        try {
            try {
                webSocket?.close(1000, "Reconnecting/Renewing")
            } catch (_: Exception) {}
            webSocket = null

            val baseUrl = SupabaseConfig.REALTIME_WS_URL
            val accessToken = SupabaseService.getAccessToken()
            val url = if (accessToken.isNotBlank() && accessToken != SupabaseConfig.ANON_KEY) {
                "$baseUrl&access_token=${java.net.URLEncoder.encode(accessToken, "UTF-8")}"
            } else {
                baseUrl
            }
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
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_MESSAGES)
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_CALL_SESSIONS)
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_TYPING_STATUS)
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", SupabaseConfig.TABLE_PROFILES)
                                })
                            }
                            put("postgres_changes", changes)
                        }
                        put("config", config)
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
                                        if (msg.status.equals("READ", ignoreCase = true) || msg.isRead) {
                                            _messageStatusUpdates.emit(msg)
                                        }
                                    }
                                }
                            } else if (subEvent == "new_message" || event == "new_message" || innerPayload.has("text")) {
                                val msg = SupabaseMessage.fromJson(innerPayload)
                                val myUid = currentUserId
                                val myName = currentUsername
                                val myClean = myName?.trim()?.removePrefix("@")?.lowercase()?.removeSuffix(".link")

                                val isFromMe = (myUid != null && msg.senderId == myUid) ||
                                    (myName != null && msg.senderId.equals(myName, ignoreCase = true)) ||
                                    (myClean != null && myClean.isNotBlank() && msg.senderId.trim().removePrefix("@").lowercase().removeSuffix(".link") == myClean)

                                val isForMe = isMessageForUser(msg, myUid, myName, currentUserEmail)

                                if (!isFromMe && isForMe && !isDuplicateAndTrack(msg)) {
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

                                        val isFromMe = (myUid != null && msg.senderId == myUid) ||
                                            (myName != null && msg.senderId.equals(myName, ignoreCase = true)) ||
                                            (myClean != null && myClean.isNotBlank() && msg.senderId.trim().removePrefix("@").lowercase().removeSuffix(".link") == myClean)

                                        val isForMe = isMessageForUser(msg, myUid, myName, currentUserEmail)
                                        val changeType = data.optString("type", "INSERT")
                                        val isUpdateOrDelete = changeType == "UPDATE" || changeType == "DELETE"

                                        val shouldEmit = if (isUpdateOrDelete) {
                                            isForMe || isFromMe
                                        } else {
                                            !isFromMe && isForMe && !isDuplicateAndTrack(msg)
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
                                    SupabaseConfig.TABLE_CALL_SESSIONS -> {
                                        val call = SupabaseCallSession.fromJson(record)
                                        if (call.receiverId == currentUserId || (currentUsername?.isNotBlank() == true && call.receiverId == currentUsername)) {
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
                                    SupabaseConfig.TABLE_PROFILES, "profiles" -> {
                                        handleProfilePresenceUpdate(record)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling WS message: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket connection failed: ${t.message}. Reconnecting in 5s...")
                    scope.launch {
                        delay(5000)
                        if (currentUserId != null) {
                            connectWebSocket()
                        }
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
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

                        if (_userPresenceMap.value.isEmpty() || loopCounter % 1L == 0L) {
                            val presenceRes = SupabaseService.getAllUserPresence()
                            if (presenceRes.isSuccess) {
                                _userPresenceMap.value = presenceRes.getOrNull() ?: emptyMap()
                            }
                        }

                        // Realtime is the fast path; polling is the delivery safety-net.
                        // Do not disable it merely because a WebSocket object exists: a socket
                        // can be connected while an event is missed due to reconnects, OEM
                        // background restrictions, or a Realtime subscription/RLS mismatch.
                        SupabaseService.getIncomingCalls(uid).getOrNull()?.forEach { _incomingCalls.emit(it) }
                        SupabaseService.fetchUserMessages(
                            userId = uid,
                            username = uname,
                            email = currentUserEmail,
                            limit = 15
                        ).getOrNull()?.forEach { msg ->
                            handlePolledMessage(msg, uid, uname)
                        }
                    }
                    loopCounter++
                } catch (e: Exception) {
                    Log.e(TAG, "Error in sync loop: " + e.message)
                }
                // Keep presence fresh enough for chat-list indicators without
                // hammering the backend; realtime remains the primary fast path.
                delay(8_000L)
            }
        }
    }
    /**
     * Realtime is an at-least-once transport: the same logical message can arrive
     * from broadcast, Postgres INSERT and the polling safety-net. Deduplicate only
     * by immutable message identity. Never use text/time buckets because two real
     * messages may legitimately contain the same text within the same second.
     */
    private fun isDuplicateAndTrack(msg: SupabaseMessage): Boolean {
        val serverKey = msg.id.trim().takeIf { it.isNotBlank() }?.let { "server:$it" }
        val clientKey = msg.clientMsgId?.trim()?.takeIf { it.isNotBlank() }?.let { "client:$it" }

        val keys = listOfNotNull(serverKey, clientKey)
        if (keys.isEmpty()) return false

        if (keys.any { processedMessageIds.contains(it) }) return true
        keys.forEach { processedMessageIds.add(it) }

        // Bound memory for long-running app processes.
        if (processedMessageIds.size > 5000) {
            val snapshot = processedMessageIds.take(1000)
            snapshot.forEach { processedMessageIds.remove(it) }
        }
        return false
    }

    private val lastSeenMessageTexts = java.util.concurrent.ConcurrentHashMap<String, String>()

    private suspend fun handlePolledMessage(msg: SupabaseMessage, uid: String, uname: String?) {
        val myClean = uname?.trim()?.removePrefix("@")?.lowercase()?.removeSuffix(".link")
        val isFromMe = msg.senderId == uid ||
            (!uname.isNullOrBlank() && msg.senderId.equals(uname, ignoreCase = true)) ||
            (myClean != null && myClean.isNotBlank() && msg.senderId.trim().removePrefix("@").lowercase().removeSuffix(".link") == myClean)

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

    fun isMessageForUser(msg: SupabaseMessage, uid: String?, uname: String?, email: String? = null, knownChatIds: Set<String> = emptySet()): Boolean {
        // If the message was sent BY ME, it's not an incoming message for me
        if (isFromMe(msg.senderId, msg.senderName)) return false

        val myUid = (uid ?: currentUserId)?.trim() ?: ""
        val myUname = (uname ?: currentUsername)?.trim() ?: ""
        val myEmail = (email ?: currentUserEmail)?.trim() ?: ""
        val myClean = myUname.removePrefix("@").lowercase().removeSuffix(".link")

        val recUid = msg.receiverId.trim()
        val recClean = recUid.removePrefix("@").lowercase().removeSuffix(".link")

        val chat = msg.chatId.trim()

        // 1. Direct 1-on-1 exact recipient check (STRICT equality, NO substring matches) - CHECK THIS FIRST!
        val isDirectRecipient = (
            (myUid.isNotBlank() && recUid.equals(myUid, ignoreCase = true)) ||
            (myUname.isNotBlank() && recUid.equals(myUname, ignoreCase = true)) ||
            (myEmail.isNotBlank() && recUid.equals(myEmail, ignoreCase = true)) ||
            (myClean.isNotBlank() && myClean != "user" && myClean != "me" && recClean.equals(myClean, ignoreCase = true))
        )
        if (isDirectRecipient) return true

        // 2. Group or Broadcast messages
        val isGroupOrBroadcast = recUid.equals("all", ignoreCase = true) || 
            recUid.equals("group", ignoreCase = true) || 
            chat == "global" || 
            chat.startsWith("group_") ||
            msg.messageType == "SYSTEM_EVENT"

        if (isGroupOrBroadcast) {
            if (chat == "global") return true
            if (knownChatIds.contains(chat)) return true
            // If it's a group/system message and receiver is specifically 'all' or 'group', allow if user is in group
            return recUid.equals("all", ignoreCase = true)
        }

        // 3. Unaddressed messages (blank receiver and not group) MUST NOT leak to 3rd party users
        if (recUid.isBlank()) {
            return knownChatIds.contains(chat)
        }

        // 4. Known local chat match check
        if (chat.isNotBlank() && knownChatIds.contains(chat)) {
            return true
        }

        // 5. Otherwise, reject 3rd party message
        return false
    }

    fun broadcastNewMessage(msg: SupabaseMessage) {
        // Message bodies are intentionally NOT sent through a public Realtime
        // broadcast channel. Durable Postgres INSERT + RLS-gated postgres_changes
        // + the polling safety-net are the only delivery paths for message content.
        // This prevents an unrelated client from subscribing to a public topic and
        // receiving another user's message payload.
        Log.d(TAG, "Skipping public message broadcast; using RLS-gated database delivery for ${msg.id}")
    }

    fun broadcastMessageMutation(msg: SupabaseMessage, mutation: String) {
        scope.launch(Dispatchers.IO) {
            try {
                if (webSocket == null) connectWebSocket()

                val payload = JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "message_mutation")
                    put("payload", JSONObject().apply {
                        put("mutation", mutation)
                        put("id", msg.id)
                        put("chat_id", msg.chatId)
                        put("sender_id", msg.senderId)
                        put("sender_name", msg.senderName)
                        put("recipient_id", msg.receiverId)
                        put("text", msg.text)
                        put("created_at", msg.timestamp)
                        put("status", msg.status)
                        put("message_type", msg.messageType)
                        put("is_edited", msg.isEdited)
                        put("is_deleted_for_everyone", msg.isDeletedForEveryone)
                        put("is_pinned", msg.isPinned)
                        put("client_msg_id", msg.clientMsgId ?: "")
                    })
                }
                val packet = JSONObject().apply {
                    put("topic", "realtime:public")
                    put("event", "broadcast")
                    put("payload", payload)
                    put("ref", "mutation_${System.currentTimeMillis()}")
                }
                webSocket?.send(packet.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Error sending message mutation broadcast: ${e.message}")
            }
        }
    }

    fun sendTypingBroadcast(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                if (webSocket == null) {
                    connectWebSocket()
                }

                val msgRef = "typing_${System.currentTimeMillis()}"

                // 1. Send WebSocket Realtime broadcast packet
                val payload = JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "typing")
                    put("payload", JSONObject().apply {
                        put("chat_id", chatId)
                        put("user_id", userId)
                        put("user_name", userName)
                        put("is_typing", isTyping)
                        put("timestamp", System.currentTimeMillis())
                    })
                }
                val broadcastMsg = JSONObject().apply {
                    put("topic", "realtime:public")
                    put("event", "broadcast")
                    put("payload", payload)
                    put("ref", msgRef)
                }
                webSocket?.send(broadcastMsg.toString())

                // Also send for sanitized/unprefixed chatId if applicable
                if (chatId.startsWith("chat_")) {
                    val rawChatId = chatId.removePrefix("chat_")
                    val rawPayload = JSONObject().apply {
                        put("type", "broadcast")
                        put("event", "typing")
                        put("payload", JSONObject().apply {
                            put("chat_id", rawChatId)
                            put("user_id", userId)
                            put("user_name", userName)
                            put("is_typing", isTyping)
                            put("timestamp", System.currentTimeMillis())
                        })
                    }
                    val rawMsg = JSONObject().apply {
                        put("topic", "realtime:public")
                        put("event", "broadcast")
                        put("payload", rawPayload)
                        put("ref", "${msgRef}_raw")
                    }
                    webSocket?.send(rawMsg.toString())
                }

                // Typing is Realtime broadcast-only; do not persist keystrokes in Postgres.
            } catch (e: Exception) {
                Log.w(TAG, "Error sending typing broadcast: ${e.message}")
            }
        }
    }

    fun handleTypingUpdate(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        val key = "$chatId:$userId"
        val altKey = if (chatId.startsWith("chat_")) "${chatId.removePrefix("chat_")}:$userId" else "chat_$chatId:$userId"
        val name = userName.ifBlank { "Someone" }
        val cleanName = name.trim().lowercase().removePrefix("@").removeSuffix(".link")

        val keysToUpdate = mutableSetOf<String>()
        if (chatId.isNotBlank()) {
            keysToUpdate.add(chatId)
            if (chatId.startsWith("chat_")) {
                keysToUpdate.add(chatId.removePrefix("chat_"))
            } else {
                keysToUpdate.add("chat_$chatId")
            }
        }

        val map = _typingUsersByChat.value.toMutableMap()

        if (isTyping) {
            var mapChanged = false
            keysToUpdate.forEach { k ->
                val list = (map[k] ?: emptyList()).toMutableList()
                if (!list.contains(name)) {
                    list.add(name)
                    map[k] = list
                    mapChanged = true
                }
            }
            if (mapChanged) {
                _typingUsersByChat.value = map
            }

            // Cancel any previous expiry timer for this user and reschedule timer without re-triggering StateFlow
            typingExpiryJobs[key]?.cancel()
            typingExpiryJobs[altKey]?.cancel()

            // Auto-clear typing status after 3.5s if no new typing event is received
            val job = scope.launch {
                delay(3500)
                handleTypingUpdate(chatId, userId, userName, false)
            }
            typingExpiryJobs[key] = job
        } else {
            typingExpiryJobs.remove(key)?.cancel()
            typingExpiryJobs.remove(altKey)?.cancel()

            keysToUpdate.forEach { k ->
                val list = (map[k] ?: emptyList()).toMutableList()
                list.remove(name)
                if (list.isEmpty()) {
                    map.remove(k)
                } else {
                    map[k] = list
                }
            }
            _typingUsersByChat.value = map
        }
    }

    fun clearTypingForChat(chatId: String) {
        val map = _typingUsersByChat.value.toMutableMap()
        map.remove(chatId)
        if (chatId.startsWith("chat_")) {
            map.remove(chatId.removePrefix("chat_"))
        } else {
            map.remove("chat_$chatId")
        }
        _typingUsersByChat.value = map
    }

    private fun handleProfilePresenceUpdate(record: JSONObject) {
        try {
            val id = record.optString("id", "").trim()
            val username = record.optString("username", "").trim()
            val email = record.optString("email", "").trim()
            val fullName = record.optString("full_name", "").trim()
            val isOnline = record.optBoolean("is_online", false)
            val lastSeen = record.optLong("last_seen", 0L)
            val now = System.currentTimeMillis()
            val diff = if (lastSeen > 0L) Math.abs(now - lastSeen) else Long.MAX_VALUE
            val isRecentlyActive = isOnline && lastSeen > 0L && diff <= 30000L
            val presencePair = Pair(isRecentlyActive, lastSeen)

            val currentMap = _userPresenceMap.value.toMutableMap()
            if (id.isNotBlank()) {
                currentMap[id] = presencePair
                currentMap[id.lowercase()] = presencePair
            }
            if (username.isNotBlank()) {
                currentMap[username] = presencePair
                currentMap[username.lowercase()] = presencePair
                val clean = username.lowercase().removePrefix("@").removeSuffix(".link")
                currentMap[clean] = presencePair
                currentMap["$clean.link"] = presencePair
                currentMap["@$clean"] = presencePair
                currentMap["@$clean.link"] = presencePair
            }
            if (email.isNotBlank()) {
                currentMap[email] = presencePair
                currentMap[email.lowercase()] = presencePair
                val prefix = email.substringBefore("@").lowercase()
                currentMap[prefix] = presencePair
            }
            if (fullName.isNotBlank()) {
                currentMap[fullName] = presencePair
                currentMap[fullName.lowercase()] = presencePair
            }
            _userPresenceMap.value = currentMap
        } catch (e: Exception) {
            Log.e(TAG, "Error updating presence from WS record: ${e.message}")
        }
    }
}
