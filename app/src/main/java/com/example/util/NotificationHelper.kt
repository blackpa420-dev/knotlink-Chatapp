package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat

object NotificationHelper {

    const val MSG_CHANNEL_ID = "knotlink_msg_channel_v4"
    const val CALL_CHANNEL_ID = "knotlink_calls_channel_v3"
    const val ONGOING_CALL_CHANNEL_ID = "knotlink_ongoing_calls_channel"

    @Volatile
    var activeChatId: String? = null

    private val recentlyShownMap = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun isRecentlyShown(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val lastShown = recentlyShownMap[key] ?: return false
        val now = System.currentTimeMillis()
        if (now - lastShown < 25000L) {
            return true
        }
        recentlyShownMap.remove(key)
        return false
    }

    fun markAsShown(key: String?) {
        if (key.isNullOrBlank()) return
        val now = System.currentTimeMillis()
        recentlyShownMap[key] = now
        if (recentlyShownMap.size > 200) {
            val it = recentlyShownMap.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                if (now - entry.value > 60000L) {
                    it.remove()
                }
            }
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            val msgSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val msgChannelV4 = NotificationChannel(
                MSG_CHANNEL_ID,
                "KnotLink Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority instant message alerts in KnotLink"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                lightColor = 0xFF00A884.toInt()
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    msgSoundUri,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val legacyMsgChannel = NotificationChannel(
                "knotlink_messages_channel",
                "KnotLink Messages (Legacy)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming message notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    msgSoundUri,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "KnotLink Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming voice and video calls in KnotLink"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    callSoundUri,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val ongoingCallChannel = NotificationChannel(
                ONGOING_CALL_CHANNEL_ID,
                "KnotLink Active Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent ongoing status bar notification for active voice calls"
                enableVibration(false)
                vibrationPattern = longArrayOf(0)
                setSound(null, null)
            }

            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(msgChannelV4)
                manager?.createNotificationChannel(legacyMsgChannel)
                manager?.createNotificationChannel(callChannel)
                manager?.createNotificationChannel(ongoingCallChannel)
            } catch (e: Throwable) {
                android.util.Log.e("BitChat_Debug", "Error creating notification channels: ${e.message}", e)
            }
        }
    }

    fun formatCleanPreviewText(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("[REPLY_QUOTE:")) {
            clean = clean.substringAfter("]").trim()
        }
        if (clean.isBlank()) return "💬 Message"

        val lower = clean.lowercase()
        return when {
            lower.contains("[audio_base64|") || lower.contains("[audio_file|") || lower.contains("voice note") || lower.contains("voice_note") || lower.contains("🎙️") -> "🎙️ Voice message"
            lower.contains("video note") || lower.contains("video_note") || lower.contains("[video_base64|") || lower.contains("[video_file|") || lower.contains("📹") -> "📹 Video message"
            lower.contains("[image_album|") -> {
                val albumPart = if (lower.contains("[image_album|")) {
                    val startIdx = lower.indexOf("[image_album|") + 13
                    clean.substring(startIdx).substringBefore("]").substringBefore("|")
                } else ""
                val count = albumPart.split(",").filter { it.isNotBlank() }.size
                if (count > 1) "📷 $count Photos" else "📷 Photo"
            }
            lower.contains("[image_attachment|") || lower.contains("[photo|") || lower.contains("🖼️ photo") || lower.contains("📸") -> "📷 Photo"
            lower.contains("sticker") || lower.contains("🎨") -> "🎨 Sticker"
            lower.contains("gif") || lower.contains("🎬") -> "🎬 GIF"
            lower.contains("poll:") || lower.contains("📊") -> "📊 Poll"
            lower.contains("contact:") || lower.contains("👤") -> "👤 Contact"
            lower.contains("[document|") || lower.contains("document:") || lower.contains("📄") -> "📄 Document"
            lower.contains("[video|") || lower.contains("🎥") -> "🎥 Video"
            clean.startsWith("[") && clean.contains("|") && clean.contains("]") -> {
                val tag = clean.substringAfter("[").substringBefore("|").uppercase()
                when (tag) {
                    "AUDIO_BASE64", "AUDIO_FILE", "VOICE" -> "🎙️ Voice message"
                    "IMAGE_ATTACHMENT", "PHOTO" -> "📷 Photo"
                    "IMAGE_ALBUM" -> "📷 Photos"
                    "VIDEO_BASE64", "VIDEO_FILE", "VIDEO" -> "📹 Video message"
                    "DOCUMENT" -> "📄 Document"
                    else -> "💬 Message"
                }
            }
            clean.length > 30 && (clean.contains("=") || clean.contains("/") || !clean.contains(" ")) && !clean.contains("http") -> "💬 Message"
            else -> clean
        }
    }

    fun getCircularBitmap(srcBitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val squareSize = Math.min(srcBitmap.width, srcBitmap.height)
        val output = android.graphics.Bitmap.createBitmap(squareSize, squareSize, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val rect = android.graphics.Rect(0, 0, squareSize, squareSize)
        val srcRect = android.graphics.Rect(
            (srcBitmap.width - squareSize) / 2,
            (srcBitmap.height - squareSize) / 2,
            (srcBitmap.width + squareSize) / 2,
            (srcBitmap.height + squareSize) / 2
        )

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(squareSize / 2f, squareSize / 2f, squareSize / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(srcBitmap, srcRect, rect, paint)
        return output
    }

    fun createLetterAvatarBitmap(context: Context, name: String): android.graphics.Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (120 * density).toInt()
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        val colors = intArrayOf(
            0xFF00A884.toInt(), 0xFF128C7E.toInt(), 0xFF075E54.toInt(),
            0xFF25D366.toInt(), 0xFF34B7F1.toInt(), 0xFFE542A3.toInt(),
            0xFF9C27B0.toInt(), 0xFFFF9800.toInt(), 0xFF3F51B5.toInt()
        )
        val color = colors[Math.abs(name.hashCode()) % colors.size]

        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = size * 0.42f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = android.graphics.Paint.Align.CENTER

        val letter = name.trim().take(1).uppercase()
        val baseline = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(letter, size / 2f, baseline, paint)

        return bitmap
    }

    fun showIncomingMessageNotification(
        context: Context,
        senderName: String,
        text: String,
        chatId: String,
        avatarBitmap: android.graphics.Bitmap? = null,
        senderId: String = "",
        serverMessageId: String? = null,
        isAvatarUpdate: Boolean = false
    ) {
        try {
            if (!isAvatarUpdate) {
                // If user is currently actively viewing this chat, do not buzz/notify
                if (chatId.isNotBlank() && chatId == activeChatId) {
                    android.util.Log.d("NotificationHelper", "Skipping notification: Chat $chatId is currently active")
                    return
                }

                // De-duplicate notifications between Supabase Realtime & FCM push
                val dedupeKey = if (!serverMessageId.isNullOrBlank()) {
                    "msg_srv_$serverMessageId"
                } else {
                    "msg_hash_${chatId}_${text.hashCode()}"
                }
                if (isRecentlyShown(dedupeKey)) {
                    android.util.Log.d("NotificationHelper", "Duplicate notification for $dedupeKey suppressed")
                    return
                }
                markAsShown(dedupeKey)
            }

            createNotificationChannels(context)

            val biometricSecurity = com.example.security.BiometricSecurityManager(context.applicationContext)
            val appLockEnabled = biometricSecurity.isAppUnlockEnabled
            val chatLocked = chatId.isNotBlank() && biometricSecurity.isChatLocked(chatId)
            val hideMessageContext = appLockEnabled || chatLocked
            val visibleSenderName = if (hideMessageContext) "KnotLink" else senderName
            val cleanPreview = if (hideMessageContext) "New message" else formatCleanPreviewText(text)
            val msgSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_chat_id", chatId)
                putExtra("chat_id", chatId)
            }
            val notifId = if (chatId.isNotBlank()) Math.abs(chatId.hashCode()) else (System.currentTimeMillis() % 100000).toInt()
            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Safely resolve avatar/profile bitmap
            val rawBitmap = try {
                if (hideMessageContext) {
                    createLetterAvatarBitmap(context, "KnotLink")
                } else {
                    avatarBitmap ?: createLetterAvatarBitmap(context, senderName)
                }
            } catch (e: Throwable) {
                null
            }
            val profileBitmap = try {
                rawBitmap?.let { getCircularBitmap(it) }
            } catch (e: Throwable) {
                null
            }

            val senderPerson = Person.Builder()
                .setName(visibleSenderName)
                .setIcon(
                    profileBitmap?.let { IconCompat.createWithBitmap(it) }
                        ?: IconCompat.createWithBitmap(createLetterAvatarBitmap(context, senderName))
                )
                .setImportant(true)
                .build()

            // Create bulletproof high-priority notification builder
            val builder = NotificationCompat.Builder(context, MSG_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(visibleSenderName)
                .setContentText(cleanPreview)
                .setColor(0xFF00A884.toInt()) // KnotLink Teal accent
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(if (isAvatarUpdate) null else msgSoundUri)
                .setVibrate(if (isAvatarUpdate) longArrayOf(0) else longArrayOf(0, 500, 250, 500))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(
                    NotificationCompat.MessagingStyle(senderPerson)
                        .addMessage(cleanPreview, System.currentTimeMillis(), senderPerson)
                )

            if (profileBitmap != null) {
                builder.setLargeIcon(profileBitmap)
            }

            // Privacy mode: do not expose sender context or reply actions for protected chats/app lock.
            if (!hideMessageContext) {
                // Add WhatsApp/Telegram style Direct Reply
                try {
                    val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
                        .setLabel("Reply to $visibleSenderName...")
                        .build()

                    val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
                        putExtra("chat_id", chatId)
                        putExtra("sender_name", senderName)
                        putExtra("sender_id", senderId)
                    }
                    val replyPendingIntent = PendingIntent.getBroadcast(
                        context,
                        notifId + 1,
                        replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    val replyAction = NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_send,
                        "Reply",
                        replyPendingIntent
                    ).addRemoteInput(remoteInput).build()

                    val markReadIntent = Intent(context, NotificationMarkReadReceiver::class.java).apply {
                        putExtra("chat_id", chatId)
                    }
                    val markReadPendingIntent = PendingIntent.getBroadcast(
                        context,
                        notifId + 2,
                        markReadIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val markReadAction = NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_view,
                        "Mark as Read",
                        markReadPendingIntent
                    ).build()

                    builder.addAction(replyAction)
                    builder.addAction(markReadAction)
                } catch (e: Throwable) {
                    android.util.Log.w("NotificationHelper", "Action buttons error: ${e.message}")
                }

            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(notifId, builder.build())
        } catch (e: Throwable) {
            android.util.Log.e("BitChat_Debug", "Error showing message notification: ${e.message}", e)
        }
    }

    private fun callNotificationId(callId: String, callerName: String): Int =
        if (callId.isNotBlank()) 20000 + Math.abs(callId.hashCode() % 100000) else 20000 + Math.abs(callerName.hashCode() % 100000)

    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        callType: String,
        callId: String = "",
        callerId: String = "",
        callerAvatarBitmap: android.graphics.Bitmap? = null,
        silentUpdate: Boolean = false
    ) {
        // Warm native WebRTC/audio resources while the phone is still ringing.
        // This removes first-call initialization latency from the Answer action.
        try {
            com.example.webrtc.WebRtcCallEngine.getInstance(context).prewarmForCall()
        } catch (e: Throwable) {
            android.util.Log.w("WebRtcEngine", "Incoming-call prewarm unavailable: ${e.message}")
        }

        createNotificationChannels(context)

        // Android 14+ can revoke/disable full-screen intent access. Log the
        // actual system state so a device-side restriction is diagnosable.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                android.util.Log.i(
                    "NotificationHelper",
                    "Call full-screen intent allowed=" + nm?.canUseFullScreenIntent()
                )
            } catch (_: Throwable) {}
        }

        val normalizedCallType = if (callType.contains("video", ignoreCase = true)) "VIDEO" else "AUDIO"
        val isVideoCall = normalizedCallType == "VIDEO"
        val callLabel = if (isVideoCall) "Incoming video call" else "Incoming audio call"

        val intent = Intent(context, com.example.CallActivity::class.java).apply {
            // Dedicated call task: safe to display above the lock screen without
            // constructing/revealing MainActivity.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action_incoming_call_screen", true)
            putExtra("is_incoming_call", true)
            putExtra("call_id", callId)
            putExtra("caller_id", callerId)
            putExtra("caller_name", callerName)
            putExtra("call_type", normalizedCallType)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            callNotificationId(callId, callerName),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer goes directly to the dedicated CallActivity so cold-start acceptance
        // does not depend on a BroadcastReceiver -> MainActivity handoff.
        val acceptIntent = Intent(context, com.example.CallActivity::class.java).apply {
            // Answer must enter the call-only task directly. This prevents the
            // normal KnotLink home/chat UI from being exposed while locked.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action_accept_call", true)
            putExtra("call_id", callId)
            putExtra("caller_id", callerId)
            putExtra("caller_name", callerName)
            putExtra("call_type", normalizedCallType)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            callNotificationId(callId, callerName) + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, NotificationCallActionReceiver::class.java).apply {
            action = NotificationCallActionReceiver.ACTION_DECLINE_CALL
            putExtra("call_id", callId)
            putExtra("caller_id", callerId)
            putExtra("caller_name", callerName)
            putExtra("call_type", callType)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            callNotificationId(callId, callerName) + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rawBitmap = callerAvatarBitmap ?: createLetterAvatarBitmap(context, callerName)
        val profileBitmap = getCircularBitmap(rawBitmap)
        val iconCompat = IconCompat.createWithBitmap(profileBitmap)

        val callerPerson = Person.Builder()
            .setName(callerName)
            .setIcon(iconCompat)
            .setImportant(true)
            .build()

        val acceptAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            "Answer",
            acceptPendingIntent
        ).build()

        val declineAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Decline",
            declinePendingIntent
        ).build()

        val callSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(profileBitmap)
            .setContentTitle(callLabel)
            .setContentText("$callerName • $callLabel")
            .setColor(0xFF00A884.toInt())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(if (silentUpdate) null else callSoundUri)
            .setVibrate(if (silentUpdate) longArrayOf(0) else longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setOngoing(true)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)

        // Use Android CallStyle for native colored action buttons (Red Decline, Green Answer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callStyle = NotificationCompat.CallStyle.forIncomingCall(
                callerPerson,
                declinePendingIntent,
                acceptPendingIntent
            )
                .setIsVideo(isVideoCall)
                .setAnswerButtonColorHint(0xFF10B981.toInt())
                .setDeclineButtonColorHint(0xFFE11D48.toInt())
                .setVerificationText(callLabel)
            builder.setStyle(callStyle)
        } else {
            builder.addAction(declineAction)
            builder.addAction(acceptAction)
        }

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(callNotificationId(callId, callerName), builder.build())
        } catch (e: Throwable) {
            android.util.Log.e("BitChat_Debug", "Error showing call notification: ${e.message}", e)
        }
    }

    fun cancelCallNotification(context: Context, callerName: String? = null, callId: String? = null) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (!callId.isNullOrBlank() || !callerName.isNullOrBlank()) {
                manager?.cancel(callNotificationId(callId ?: "", callerName ?: "Caller"))
                manager?.cancel(callNotificationId(callId ?: "", callerName ?: "Caller") + 1)
                manager?.cancel(callNotificationId(callId ?: "", callerName ?: "Caller") + 2)
            }
            manager?.cancel(10099)
        } catch (e: Throwable) {
            android.util.Log.e("BitChat_Debug", "Error cancelling call notification: ${e.message}", e)
        }
    }

    fun showOngoingAudioCallNotification(
        context: Context,
        callerName: String,
        secondsElapsed: Int,
        callId: String = "",
        callType: String = "AUDIO"
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, com.example.CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("call_id", callId)
            putExtra("caller_name", callerName)
            putExtra("call_type", callType)
            putExtra("action_accept_call", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            10099,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(context, NotificationCallActionReceiver::class.java).apply {
            action = NotificationCallActionReceiver.ACTION_END_CALL
            putExtra("call_id", callId)
            putExtra("caller_name", callerName)
            putExtra("call_type", callType)
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            10100,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val video = callType.equals("VIDEO", ignoreCase = true)

        val builder = NotificationCompat.Builder(context, ONGOING_CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (video) "Ongoing Video Call" else "Ongoing Voice Call")
            .setContentText("$callerName • $timeFormatted")
            .setColor(0xFF00A884.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Hang up",
                    endPendingIntent
                ).build()
            )

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(10099, builder.build())
        } catch (e: Throwable) {
            android.util.Log.e("BitChat_Debug", "Error showing ongoing call notification: ${e.message}", e)
        }
    }


    private fun persistFcmTokenWithRetry(context: Context, userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val identityIds = try {
                val db = com.example.data.local.BitChatDatabase.getDatabase(context)
                val iden = db.bitChatDao().getUserIdentitySync()
                listOfNotNull(
                    userId.takeIf { it.isNotBlank() },
                    iden?.supabaseUid?.takeIf { it.isNotBlank() },
                    iden?.username?.takeIf { it.isNotBlank() },
                    iden?.email?.takeIf { it.isNotBlank() }
                ).distinct()
            } catch (_: Throwable) {
                listOf(userId)
            }

            val delays = longArrayOf(0L, 1000L, 2500L, 5000L, 8000L)
            for ((attempt, waitMs) in delays.withIndex()) {
                if (waitMs > 0) delay(waitMs)

                var anySuccess = false
                for (identityId in identityIds) {
                    try {
                        val result = com.example.data.supabase.SupabaseService.updateFcmToken(identityId, token)
                        if (result.getOrNull() == true) anySuccess = true
                    } catch (e: Throwable) {
                        android.util.Log.w("KnotLinkFCM", "Token upload attempt ${attempt + 1} failed for $identityId: ${e.message}")
                    }
                }

                if (anySuccess) {
                    android.util.Log.i("KnotLinkFCM", "FCM token persisted to Supabase after attempt ${attempt + 1}")
                    return@launch
                }
            }

            android.util.Log.w("KnotLinkFCM", "FCM token could not be persisted after bounded retries")
        }
    }

    fun registerFcmToken(context: Context, userId: String) {
        if (userId.isBlank()) return
        try {
            createNotificationChannels(context)

            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                android.util.Log.w("KnotLinkFCM", "FirebaseApp is not initialized.")
                return
            }

            // Check if Google Play Services is available before attempting FCM registration
            val isPlayServicesAvailable: Boolean = try {
                val clazz = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
                val getInstance = clazz.getMethod("getInstance")
                val instance = getInstance.invoke(null)
                val isAvailable = clazz.getMethod("isGooglePlayServicesAvailable", Context::class.java)
                val res = isAvailable.invoke(instance, context) as? Int
                res == 0 // ConnectionResult.SUCCESS is 0
            } catch (e: Throwable) {
                try {
                    context.packageManager.getPackageInfo("com.google.android.gms", 0)
                    true
                } catch (_: Throwable) {
                    false
                }
            }

            if (!isPlayServicesAvailable) {
                android.util.Log.i("KnotLinkFCM", "Google Play Services unavailable for FCM. FCM push registration skipped.")
                // Fallback: If a valid token was cached previously, ensure Supabase has it
                val prefs = context.getSharedPreferences("knotlink_fcm_prefs", Context.MODE_PRIVATE)
                val cachedToken = prefs.getString("fcm_token", null)
                if (!cachedToken.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        com.example.data.supabase.SupabaseService.updateFcmToken(userId, cachedToken)
                    }
                }
                return
            }

            val fm = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            fm.isAutoInitEnabled = true
            @Suppress("DEPRECATION")
            fm.token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrBlank()) {
                            val masked = if (token.length > 10) "${token.take(6)}...${token.takeLast(4)}" else "***"
                            android.util.Log.i("KnotLinkFCM", "Fetched FCM token for $userId: $masked")
                            val prefs = context.getSharedPreferences("knotlink_fcm_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("fcm_token", token).apply()

                            persistFcmTokenWithRetry(context, userId, token)
                        }
                    } else {
                        android.util.Log.w("KnotLinkFCM", "FCM token registration failed: ${task.exception?.message}")
                        // Fresh installs can briefly fail token generation. Retry a few times
                        // with bounded exponential backoff instead of losing the first pushes.
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(1500L)
                            registerFcmToken(context, userId)
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("KnotLinkFCM", "Error processing FCM token callback: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("KnotLinkFCM", "FCM token registration skipped: ${e.message}")
        }
    }
}
