package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationCallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DECLINE_CALL = "com.knotlink.ACTION_DECLINE_CALL"
        const val ACTION_ACCEPT_CALL = "com.knotlink.ACTION_ACCEPT_CALL"
        private const val TAG = "NotificationCallAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val callId = intent.getStringExtra("call_id") ?: ""
        val callerId = intent.getStringExtra("caller_id") ?: ""
        val callerName = intent.getStringExtra("caller_name") ?: "Caller"
        val callType = intent.getStringExtra("call_type") ?: "Voice"

        Log.i(TAG, "Notification call action received: $action for callId=$callId, caller=$callerName")

        if (action == ACTION_DECLINE_CALL) {
            val pendingResult = goAsync()
            NotificationHelper.cancelCallNotification(context, callerName)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (callId.isNotBlank()) {
                        SupabaseService.updateCallSessionStatus(callId, "DECLINED", endedAt = System.currentTimeMillis())
                    }
                    if (callerId.isNotBlank()) {
                        FcmPushSender.sendPushToUser(
                            targetUserIdOrName = callerId,
                            type = "call_declined",
                            title = "Call Declined",
                            body = "Call was declined",
                            senderName = callerName,
                            chatId = callId
                        )
                    }
                    val endIntent = Intent("com.knotlink.CALL_ENDED").apply {
                        putExtra("caller_name", callerName)
                        putExtra("chat_id", callId)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(endIntent)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error declining call from notification: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_ACCEPT_CALL) {
            NotificationHelper.cancelCallNotification(context, callerName)

            val mainIntent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_accept_call", true)
                putExtra("call_id", callId)
                putExtra("caller_id", callerId)
                putExtra("caller_name", callerName)
                putExtra("call_type", callType)
            }
            context.startActivity(mainIntent)
        }
    }
}
