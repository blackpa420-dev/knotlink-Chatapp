package com.example.util

import android.util.Log
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client-side FCM facade.
 *
 * Firebase service-account credentials are never shipped in the APK.
 * FCM HTTP v1 delivery happens in the authenticated Supabase Edge Function.
 */
object FcmPushSender {
    private const val TAG = "FcmPushSender"
    private const val USER_FUNCTION_NAME = "fcm-send-v2"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Intentionally a no-op. Every push (new message, incoming call, missed call, delete sync) is now
     * sent by Postgres triggers -> `fcm-dispatch` Edge Function, which works even when the sender's
     * app is killed and cannot be spoofed by a client. Keeping this method avoids touching callers;
     * the legacy `fcm-send-v2` function no longer exists (it returned HTTP 404).
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun sendPushToUser(
        targetUserIdOrName: String,
        type: String,
        title: String,
        body: String,
        senderName: String,
        chatId: String,
        callType: String = "Voice",
        senderAvatar: String? = null,
        senderId: String? = null,
        serverMessageId: String? = null,
        messageType: String? = null
    ) {
        Log.d(TAG, "Client push skipped (type=$type); server triggers dispatch pushes")
    }
}
