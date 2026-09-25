package com.example.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.BitChatDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationMuteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationMute"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra("chat_id") ?: return
        val appContext = context.applicationContext

        // 1. Dismiss active notification for this chat immediately
        try {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(Math.abs(chatId.hashCode()))
            manager?.cancel(chatId.hashCode())
        } catch (e: Throwable) {
            Log.w(TAG, "Error dismissing notification on mute: ${e.message}")
        }

        // 3. Update local Room DB chat mute status asynchronously
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BitChatDatabase.getDatabase(appContext)
                db.bitChatDao().updateChatMuteStatus(chatId, true)
                Log.i(TAG, "Muted notifications for chat $chatId from notification action")
            } catch (e: Throwable) {
                Log.e(TAG, "Error updating chat mute status in DB: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
