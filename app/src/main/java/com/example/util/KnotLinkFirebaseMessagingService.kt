package com.example.util

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.supabase.SupabaseRealtimeManager
import com.example.data.supabase.SupabaseService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KnotLinkFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            val masked = if (token.length > 10) "${token.take(6)}...${token.takeLast(4)}" else "***"
            Log.i("KnotLinkFCM", "New Firebase FCM Token received: $masked")
            // Save locally
            val prefs = applicationContext.getSharedPreferences("knotlink_fcm_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("fcm_token", token).apply()

            // Upload token to Supabase profile
            scope.launch {
                try {
                    val uid = SupabaseRealtimeManager.getCurrentUserId()
                    if (!uid.isNullOrBlank()) {
                        SupabaseService.updateFcmToken(uid, token)
                    }

                    // Fallback to Room DB UserIdentity if SupabaseRealtimeManager is not active
                    val db = com.example.data.local.BitChatDatabase.getDatabase(applicationContext)
                    val iden = db.bitChatDao().getUserIdentitySync()
                    if (iden != null) {
                        if (iden.supabaseUid.isNotBlank()) SupabaseService.updateFcmToken(iden.supabaseUid, token)
                        if (iden.username.isNotBlank()) SupabaseService.updateFcmToken(iden.username, token)
                        if (iden.email.isNotBlank()) SupabaseService.updateFcmToken(iden.email, token)
                    }
                } catch (e: Throwable) {
                    Log.w("KnotLinkFCM", "Error updating token to Supabase: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.w("KnotLinkFCM", "Error handling new token: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        var wakeLock: PowerManager.WakeLock? = null
        try {
            Log.d("KnotLinkFCM", "Incoming FCM Push Data: ${remoteMessage.data}")

            // Check if push notifications are enabled in settings
            val prefs = applicationContext.getSharedPreferences("knotlink_fcm_prefs", Context.MODE_PRIVATE)
            val pushEnabled = prefs.getBoolean("push_notifications_enabled", true)
            if (!pushEnabled) {
                Log.i("KnotLinkFCM", "Push notifications are disabled in settings. Skipping push processing.")
                return
            }

            // Acquire temporary WakeLock to wake screen for incoming call or urgent message
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "KnotLink:FCMPushWakeLock"
            )
            wakeLock?.acquire(10000L) // 10 seconds max

            SupabaseService.init(applicationContext)
            NotificationHelper.createNotificationChannels(applicationContext)

            val data = remoteMessage.data
            val type = data["type"] ?: "message"
            val title = data["title"] ?: remoteMessage.notification?.title ?: "KnotLink"
            val body = data["body"] ?: remoteMessage.notification?.body ?: "New notification"
            val callerName = data["caller_name"]?.ifBlank { null }
                ?: data["sender_name"]?.ifBlank { null }
                ?: (if (title.startsWith("New Message from ")) title.removePrefix("New Message from ") else title)
            val callType = data["call_type"] ?: "Voice"
            val senderId = data["sender_id"] ?: ""
            val chatId = data["chat_id"] ?: data["room_id"] ?: senderId
            val rawSenderAvatarUrl = data["sender_avatar"] ?: ""
            // FCM must render the notification immediately. Do not perform a
            // Supabase/profile network lookup here; Android gives onMessageReceived
            // only a short processing window for high-priority pushes.
            val senderAvatarUrl = if (
                rawSenderAvatarUrl.startsWith("http://") ||
                rawSenderAvatarUrl.startsWith("https://")
            ) {
                rawSenderAvatarUrl
            } else {
                try {
                    val localDao = com.example.data.local.BitChatDatabase.getDatabase(applicationContext).bitChatDao()
                    if (senderId.isNotBlank()) localDao.getCachedProfile(senderId)?.avatarUrl.orEmpty() else ""
                } catch (_: Throwable) {
                    ""
                }
            }

            // Handle Call Cancelled / Ended / Declined Signal
            if (type.equals("call_ended", ignoreCase = true) ||
                type.equals("call_declined", ignoreCase = true) ||
                type.equals("call_cancelled", ignoreCase = true)) {

                Log.i("KnotLinkFCM", "Received call termination push ($type) for $callerName")
                NotificationHelper.cancelCallNotification(applicationContext, callerName, chatId)

                val endIntent = Intent("com.knotlink.CALL_ENDED").apply {
                    putExtra("caller_name", callerName)
                    putExtra("call_id", chatId)
                    putExtra("chat_id", chatId)
                    putExtra("caller_id", senderId)
                    setPackage(packageName)
                }
                applicationContext.sendBroadcast(endIntent)
                return
            }

            if (type.equals("call", ignoreCase = true) || type.equals("incoming_call", ignoreCase = true)) {
                Log.i("KnotLinkFCM", "Triggering High-Priority Call Notification for $callerName")

                // Resolve the caller avatar before the first render so the call
                // notification never flashes an avatar-less version and then gets replaced.
                // 1. Render the call notification immediately. Avatar download is
                // deliberately deferred so a sleeping device does not miss the call alert.
                NotificationHelper.showIncomingCallNotification(
                    context = applicationContext,
                    callerName = callerName,
                    callType = callType,
                    callId = chatId,
                    callerId = senderId,
                    callerAvatarBitmap = null
                )

                // 2. Enrich the already-visible notification with the avatar silently.
                if (senderAvatarUrl.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        val bitmap = downloadBitmapFromUrl(senderAvatarUrl)
                        if (bitmap != null) {
                            NotificationHelper.showIncomingCallNotification(
                                context = applicationContext,
                                callerName = callerName,
                                callType = callType,
                                callId = chatId,
                                callerId = senderId,
                                callerAvatarBitmap = bitmap,
                                silentUpdate = true
                            )
                        }
                    }
                }

                // 2. INSTANT ROOM DB CALL LOG INSERTION (so call history shows up even when app is closed)
                scope.launch {
                    try {
                        val db = com.example.data.local.BitChatDatabase.getDatabase(applicationContext)
                        val dao = db.bitChatDao()
                        val ts = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        val timeStr = sdf.format(java.util.Date(ts))
                        val callLogId = if (chatId.isNotBlank()) "remote_call_$chatId" else "remote_call_${java.util.UUID.randomUUID().toString().take(8)}"
                        val callLog = com.example.data.local.CallLogEntity(
                            id = callLogId,
                            contactId = if (senderId.isNotBlank()) senderId else chatId,
                            contactName = callerName,
                            callType = if (callType.equals("Video", ignoreCase = true) || callType.equals("VIDEO", ignoreCase = true)) "VIDEO" else "AUDIO",
                            direction = "INCOMING",
                            timestampMillis = ts,
                            timeString = timeStr,
                            durationSeconds = 0,
                            avatarType = senderAvatarUrl.ifBlank { "default" }
                        )
                        dao.insertCallLog(callLog)

                        // Avatar was already resolved before the first notification render.
                    } catch (e: Throwable) {
                        Log.e("KnotLinkFCM", "Error inserting call log or avatar: ${e.message}")
                    }
                }

                // Send broadcast intent for active app receivers
                val callIntent = Intent("com.knotlink.INCOMING_CALL_PUSH").apply {
                    putExtra("caller_name", callerName)
                    putExtra("caller_id", senderId)
                    putExtra("call_type", callType)
                    putExtra("call_id", chatId)
                    putExtra("room_id", chatId)
                    setPackage(packageName)
                }
                applicationContext.sendBroadcast(callIntent)
            } else {
                Log.i("KnotLinkFCM", "Triggering Message Notification from $callerName")

                val msgId = data["server_message_id"]?.ifBlank { null } ?: ("msg_" + java.util.UUID.randomUUID().toString().take(8))

                // 1. Render the message notification immediately. Never block the
                // first notification on a profile/avatar HTTP request.
                NotificationHelper.showIncomingMessageNotification(
                    context = applicationContext,
                    senderName = callerName,
                    text = body,
                    chatId = chatId,
                    avatarBitmap = null,
                    senderId = senderId,
                    serverMessageId = msgId
                )

                // 2. Fetch avatar only after the visible notification is posted.
                if (senderAvatarUrl.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        val bitmap = downloadBitmapFromUrl(senderAvatarUrl)
                        if (bitmap != null) {
                            NotificationHelper.showIncomingMessageNotification(
                                context = applicationContext,
                                senderName = callerName,
                                text = body,
                                chatId = chatId,
                                avatarBitmap = bitmap,
                                senderId = senderId,
                                serverMessageId = msgId,
                                isAvatarUpdate = true
                            )
                        }
                    }
                }

                // 2. INSTANT ROOM DB INSERTION so inbox is immediately updated without network lag
                val ts = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val timeStr = sdf.format(java.util.Date(ts))

                if (chatId.isNotBlank() && body.isNotBlank()) {
                    scope.launch {
                        try {
                            val db = com.example.data.local.BitChatDatabase.getDatabase(applicationContext)
                            val dao = db.bitChatDao()

                            // Check if message was already saved (e.g., from Supabase Realtime)
                            val existingMsg = if (msgId.isNotBlank()) dao.findMessageByServerId(msgId) else null
                            if (existingMsg != null) {
                                Log.d("KnotLinkFCM", "Message $msgId already present in local DB. Skipping duplicate insertion.")
                                return@launch
                            }

                            val msgType = data["knotlink_message_type"] ?: "TEXT"
                            val isRichMedia = msgType != "TEXT" || 
                                              body.startsWith("🎙️") || 
                                              body.startsWith("🖼️") || 
                                              body.startsWith("📹") || 
                                              body.startsWith("📄") || 
                                              body.contains("Voice message") || 
                                              body.contains("Video message") || 
                                              body.contains("Photo") || 
                                              body.contains("Document")

                            if (!isRichMedia) {
                                val newMsg = com.example.data.local.MessageEntity(
                                    chatId = chatId,
                                    senderName = callerName,
                                    text = body,
                                    timestampString = timeStr,
                                    isFromUser = false,
                                    isRead = false,
                                    senderUid = senderId,
                                    serverMessageId = msgId,
                                    syncStatus = "SYNCED",
                                    deliveryState = "DELIVERED",
                                    timestamp = ts,
                                    messageType = "TEXT"
                                )
                                dao.insertMessage(newMsg)
                            }

                            val allLocal = dao.getAllChatsList()
                            val existingChat = dao.getChatById(chatId) 
                                ?: allLocal.find { chat ->
                                    chat.chatType != "GROUP" && chat.category != "Group" && (
                                        (senderId.isNotBlank() && chat.participantUids.split(",").map { it.trim().lowercase() }.contains(senderId.lowercase())) ||
                                        (callerName.isNotBlank() && callerName != "User" && callerName != "Contact" && chat.name.equals(callerName, ignoreCase = true))
                                    )
                                }

                            if (existingChat != null) {
                                val updatedChat = existingChat.copy(
                                    lastMessage = body,
                                    timeString = timeStr,
                                    lastUpdated = ts,
                                    unreadCount = existingChat.unreadCount + 1
                                )
                                dao.insertChats(listOf(updatedChat))
                            } else {
                                val newChat = com.example.data.local.ChatEntity(
                                    id = chatId,
                                    name = callerName,
                                    lastMessage = body,
                                    timeString = timeStr,
                                    unreadCount = 1,
                                    isOnline = false,
                                    category = "All",
                                    lastUpdated = ts,
                                    participantUids = senderId
                                )
                                dao.insertChats(listOf(newChat))
                            }
                        } catch (e: Throwable) {
                            Log.e("KnotLinkFCM", "Error inserting message to DB: ${e.message}")
                        }
                    }
                }

                // Avatar was resolved before the first notification render.
                // Do not issue a second notify() update for the same message.
            }
        } catch (e: Throwable) {
            Log.w("KnotLinkFCM", "Error processing incoming FCM message: ${e.message}")
        } finally {
            try {
                wakeLock?.let { if (it.isHeld) it.release() }
            } catch (_: Throwable) {}
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        // FCM explicitly recommends a full server sync when pending messages were
        // dropped. Recover both chat history and call history on the next callback.
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.BitChatDatabase.getDatabase(applicationContext)
                val identity = db.bitChatDao().getUserIdentitySync()
                val uid = identity?.let { it.supabaseUid.ifBlank { it.email } }.orEmpty()
                if (uid.isNotBlank()) {
                    val repository = com.example.data.repository.BitChatRepository(db.bitChatDao())
                    repository.syncAllChatHistory(uid, identity?.username.orEmpty())
                    repository.syncCallHistory(uid, identity?.username.orEmpty())
                }
            } catch (e: Throwable) {
                Log.w("KnotLinkFCM", "Full sync after deleted FCM messages failed: " + e.message)
            }
        }
    }

    private fun downloadBitmapFromUrl(url: String): android.graphics.Bitmap? {
        if (url.isBlank()) return null
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val input = connection.inputStream
            android.graphics.BitmapFactory.decodeStream(input)
        } catch (e: Throwable) {
            null
        }
    }
}
