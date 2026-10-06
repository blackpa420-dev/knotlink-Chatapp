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
    ) = withContext(Dispatchers.IO) {
        if (targetUserIdOrName.isBlank()) {
            Log.w(TAG, "FCM Push skipped: target user is blank")
            return@withContext
        }

        try {
            val targetUuid = targetUserIdOrName.trim()
            val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            if (!targetUuid.matches(uuidRegex)) {
                Log.w(TAG, "FCM Push skipped: recipient must be a canonical UUID")
                return@withContext
            }

            if (!targetUuid.matches(uuidRegex)) {
                Log.w(TAG, "FCM Push skipped: recipient is not a resolvable canonical UUID")
                return@withContext
            }

            val accessToken = SupabaseService.getAccessToken()
            if (accessToken.isBlank() || accessToken == SupabaseConfig.ANON_KEY) {
                Log.w(TAG, "FCM Push skipped: authenticated Supabase session required")
                return@withContext
            }

            val cleanBody = NotificationHelper.formatCleanPreviewText(body).take(120)
            val payload = JSONObject().apply {
                put("targetUserId", targetUuid)
                put("type", type)
                put("title", title)
                put("messageBody", cleanBody)
                put("callerName", senderName)
                put("chatId", chatId)
                put("callType", callType)
                put("senderAvatar", senderAvatar ?: "")
                put("senderId", senderId ?: "")
                if (!serverMessageId.isNullOrBlank()) put("serverMessageId", serverMessageId)
                if (!messageType.isNullOrBlank()) put("messageType", messageType)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.PROJECT_URL}/functions/v1/$USER_FUNCTION_NAME")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Log.i(TAG, "FCM user push accepted by server for $targetUuid")
                } else {
                    Log.w(TAG, "FCM user push rejected: HTTP ${response.code}: $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error sending FCM user push: ${e.message}")
        }
    }
}
