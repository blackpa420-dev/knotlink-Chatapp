package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.BitChatDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationMarkReadReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationMarkRead"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra("chat_id") ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BitChatDatabase.getDatabase(context)
                val dao = db.bitChatDao()
                dao.markUserMessagesAsRead(chatId)

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                manager?.cancel(chatId.hashCode())

                Log.i(TAG, "Marked messages as read from notification for chat $chatId")
            } catch (e: Throwable) {
                Log.e(TAG, "Error marking messages as read from notification: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
