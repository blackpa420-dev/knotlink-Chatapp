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
        val fallbackSenderId = intent.getStringExtra("sender_id") ?: ""
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseService.init(appContext)
                val db = BitChatDatabase.getDatabase(appContext)
                val dao = db.bitChatDao()

                val currentIdentity = dao.getUserIdentitySync() ?: dao.getUserIdentity().firstOrNull()
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
                val resolvedFromChat = participants.firstOrNull {
                    it.isNotBlank() &&
                    !it.equals(currentUid, ignoreCase = true) &&
                    !it.equals(myUsername, ignoreCase = true) &&
                    !it.equals(myEmail, ignoreCase = true)
                } ?: ""
                val resolvedFromChatId = if (chatId.startsWith("chat_")) {
                    chatId.removePrefix("chat_").split("_").firstOrNull {
                        it.isNotBlank() && !it.equals(currentUid, ignoreCase = true)
                    } ?: ""
                } else ""
                val otherParticipant = resolvedFromChat.ifBlank { fallbackSenderId }.ifBlank { resolvedFromChatId }

                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val serverTs = System.currentTimeMillis()
                val currentTime = sdf.format(Date(serverTs))
                val canonicalMsgId = UUID.randomUUID().toString()
                val targetChatId = existingChat?.id ?: chatId

                val message = MessageEntity(
                    chatId = targetChatId,
                    senderName = mySenderName,
                    text = replyText,
                    timestampString = currentTime,
                    isFromUser = true,
                    isRead = true,
                    senderUid = currentUid,
                    receiverUid = otherParticipant,
                    clientMessageId = canonicalMsgId,
                    serverMessageId = canonicalMsgId,
                    syncStatus = "SYNCED",
                    deliveryState = "SENT",
                    timestamp = serverTs,
                    serverTimestamp = serverTs,
                    messageType = "TEXT"
                )

                // 1. Save directly to local Room DB and mark incoming messages read
                val localRowId = dao.insertMessage(message)
                dao.markUserMessagesAsRead(targetChatId)
                dao.resetChatUnreadCount(targetChatId)

                if (existingChat != null) {
                    val updatedChat = existingChat.copy(
                        lastMessage = replyText,
                        timeString = currentTime,
                        unreadCount = 0,
                        lastUpdated = serverTs
                    )
                    dao.insertChats(listOf(updatedChat))
                }

                // 2. Broadcast & Send to Supabase REST
                val supaMsg = SupabaseMessage(
                    id = canonicalMsgId,
                    chatId = targetChatId,
                    senderId = currentUid,
                    senderName = mySenderName,
                    receiverId = otherParticipant,
                    text = replyText,
                    timestamp = serverTs,
                    isRead = false,
                    messageType = "TEXT",
                    clientMsgId = canonicalMsgId
                )
                com.example.data.supabase.SupabaseRealtimeManager.broadcastNewMessage(supaMsg)
                val sendRes = SupabaseService.sendMessage(supaMsg).getOrNull()
                val finalServerId = sendRes?.id?.takeIf { it.isNotBlank() && !it.startsWith("msg_") } ?: canonicalMsgId
                if (finalServerId.isNotBlank() && finalServerId != canonicalMsgId) {
                    dao.updateMessageServerId(localRowId, canonicalMsgId, finalServerId)
                }

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
                            chatId = targetChatId,
                            senderAvatar = myAvatarUrl,
                            senderId = currentUid,
                            serverMessageId = finalServerId,
                            messageType = "TEXT"
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error sending FCM reply push: ${e.message}")
                    }
                }

                // 4. Dismiss notification
                val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                manager?.cancel(Math.abs(chatId.hashCode()))
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
