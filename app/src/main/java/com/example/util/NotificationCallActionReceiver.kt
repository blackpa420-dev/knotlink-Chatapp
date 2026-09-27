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
        const val ACTION_END_CALL = "com.knotlink.ACTION_END_CALL"
        private const val TAG = "NotificationCallAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val callId = intent.getStringExtra("call_id") ?: ""
        val callerId = intent.getStringExtra("caller_id") ?: ""
        val peerId = intent.getStringExtra("peer_id") ?: callerId
        val callerName = intent.getStringExtra("caller_name") ?: "Caller"
        val callType = if (intent.getStringExtra("call_type")?.equals("VIDEO", ignoreCase = true) == true) "VIDEO" else "AUDIO"

        Log.i(TAG, "Notification call action received: $action for callId=$callId, caller=$callerName")

        if (action == ACTION_DECLINE_CALL) {
            val pendingResult = goAsync()
            NotificationHelper.cancelCallNotification(context, callerName, callId)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (callId.isNotBlank()) {
                        SupabaseService.updateCallSessionStatus(callId, "DECLINED", endedAt = System.currentTimeMillis())
                    }
                    if (callerId.isNotBlank()) {
                        FcmPushSender.sendPushToUser(
                            targetUserIdOrName = peerId,
                            type = "call_declined",
                            title = "Call Declined",
                            body = "Call was declined",
                            senderName = callerName,
                            chatId = callId
                        )
                    }
                    val endIntent = Intent("com.knotlink.CALL_ENDED").apply {
                        putExtra("caller_name", callerName)
                        putExtra("call_id", callId)
                        putExtra("chat_id", callId)
                        putExtra("caller_id", callerId)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(endIntent)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error declining call from notification: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_END_CALL) {
            val pendingResult = goAsync()
            NotificationHelper.cancelCallNotification(context, callerName, callId)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (callId.isNotBlank()) {
                        SupabaseService.updateCallSessionStatus(
                            callId,
                            "ENDED",
                            endedAt = System.currentTimeMillis()
                        )
                    }
                    if (callerId.isNotBlank()) {
                        FcmPushSender.sendPushToUser(
                            targetUserIdOrName = callerId,
                            type = "call_ended",
                            title = "Call Ended",
                            body = "Call was ended",
                            senderName = callerName,
                            chatId = callId
                        )
                    }
                    val endIntent = Intent("com.knotlink.CALL_ENDED").apply {
                        putExtra("caller_name", callerName)
                        putExtra("call_id", callId)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(endIntent)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error ending call from ongoing notification: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_ACCEPT_CALL) {
            NotificationHelper.cancelCallNotification(context, callerName, callId)

            val mainIntent = Intent(context, com.example.CallActivity::class.java).apply {
                // Keep notification answer inside the dedicated call task.
                // MainActivity must never be opened from the lock screen.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
