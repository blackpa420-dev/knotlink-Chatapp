package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.data.local.BitChatDatabase
import com.example.data.local.MessageEntity
import com.example.data.supabase.SupabaseMessage
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NotificationReplyReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        private const val TAG = "NotificationReply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = results.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim() ?: return
        if (replyText.isBlank()) return

        val chatId = intent.getStringExtra("chat_id") ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BitChatDatabase.getDatabase(context)
                val dao = db.bitChatDao()

                val currentIdentity = dao.getUserIdentity().firstOrNull()
                val currentUid = currentIdentity?.supabaseUid?.takeIf { it.isNotBlank() }
                    ?: currentIdentity?.email?.takeIf { it.isNotBlank() }
                    ?: "user_me"

                val mySenderName = currentIdentity?.fullName?.takeIf { it.isNotBlank() }
                    ?: currentIdentity?.username?.takeIf { it.isNotBlank() }
                    ?: "Me"

                val existingChat = dao.getChatById(chatId)
                val myUsername = currentIdentity?.username ?: ""
                val myEmail = currentIdentity?.email ?: ""
                val participants = existingChat?.participantUids?.split(",")?.map { it.trim() } ?: emptyList()
                val otherParticipant = participants.firstOrNull {
                    it.isNotBlank() &&
                    !it.equals(currentUid, ignoreCase = true) &&
                    !it.equals(myUsername, ignoreCase = true) &&
                    !it.equals(myEmail, ignoreCase = true)
                } ?: ""

                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentTime = sdf.format(Date())
                val clientMsgId = UUID.randomUUID().toString()
                val serverMsgId = "msg_" + UUID.randomUUID().toString().take(8)
                val serverTs = System.currentTimeMillis()

                val message = MessageEntity(
                    chatId = chatId,
                    senderName = mySenderName,
                    text = replyText,
                    timestampString = currentTime,
                    isFromUser = true,
                    isRead = true,
                    senderUid = currentUid,
                    clientMessageId = clientMsgId,
                    serverMessageId = serverMsgId,
                    syncStatus = "SYNCED",
                    deliveryState = "SENT",
                    timestamp = serverTs,
                    messageType = "TEXT"
                )

                // 1. Save directly to local Room DB
                dao.insertMessage(message)

                if (existingChat != null) {
                    val updatedChat = existingChat.copy(
                        lastMessage = replyText,
                        timeString = currentTime,
                        lastUpdated = System.currentTimeMillis()
                    )
                    dao.insertChats(listOf(updatedChat))
                }

                // 2. Send to Supabase REST
                val supaMsg = SupabaseMessage(
                    id = serverMsgId,
                    chatId = chatId,
                    senderId = currentUid,
                    senderName = mySenderName,
                    receiverId = otherParticipant,
                    text = replyText,
                    timestamp = serverTs,
                    isRead = false,
                    messageType = "TEXT"
                )
                SupabaseService.sendMessage(supaMsg)

                // 3. Trigger FCM High Priority Push Notification to recipient
                if (otherParticipant.isNotBlank()) {
                    try {
                        val myAvatarUrl = currentIdentity?.avatarPath ?: ""
                        FcmPushSender.sendPushToUser(
                            targetUserIdOrName = otherParticipant,
                            type = "message",
                            title = "New Message from $mySenderName",
                            body = NotificationHelper.formatCleanPreviewText(replyText),
                            senderName = mySenderName,
                            chatId = chatId,
                            senderAvatar = myAvatarUrl,
                            senderId = currentUid
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error sending FCM reply push: ${e.message}")
                    }
                }

                // 4. Dismiss notification or update state
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                manager?.cancel(chatId.hashCode())

                Log.i(TAG, "Inline reply sent and synced successfully for chat $chatId")
            } catch (e: Throwable) {
                Log.e(TAG, "Error handling notification reply: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
