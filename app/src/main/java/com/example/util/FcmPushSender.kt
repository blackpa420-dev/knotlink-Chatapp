package com.example.util

import android.util.Log
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseService
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
    private const val FUNCTION_NAME = "fcm-send"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun sendPushToToken(
        targetFcmToken: String,
        type: String,
        title: String,
        body: String,
        callerName: String,
        chatId: String,
        callType: String = "Voice",
        senderAvatar: String? = null,
        senderId: String? = null,
        serverMessageId: String? = null,
        messageType: String? = null
    ) {
        if (targetFcmToken.isBlank()) {
            Log.w(TAG, "Cannot send FCM push: target token is blank")
            return
        }

        val accessToken = SupabaseService.getAccessToken()
        if (accessToken.isBlank() || accessToken == SupabaseConfig.ANON_KEY) {
            Log.w(TAG, "Cannot send FCM push: authenticated Supabase session is required")
            return
        }

        Thread {
            try {
                val data = JSONObject().apply {
                    put("targetFcmToken", targetFcmToken)
                    put("type", type)
                    put("title", title)
                    put("messageBody", body)
                    put("callerName", callerName)
                    put("chatId", chatId)
                    put("callType", callType)
                    put("senderAvatar", senderAvatar ?: "")
                    put("senderId", senderId ?: "")
                    if (!serverMessageId.isNullOrBlank()) put("serverMessageId", serverMessageId)
                    if (!messageType.isNullOrBlank()) put("messageType", messageType)
                }

                val request = Request.Builder()
                    .url("${SupabaseConfig.PROJECT_URL}/functions/v1/$FUNCTION_NAME")
                    .addHeader("apikey", SupabaseConfig.ANON_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(data.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        Log.i(TAG, "FCM push accepted by server (type=$type)")
                    } else {
                        Log.w(TAG, "FCM server rejected push: HTTP ${response.code}: $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM server request failed: ${e.message}", e)
            }
        }.start()
    }

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
        if (targetUserIdOrName.isBlank() && chatId.isBlank()) {
            Log.w(TAG, "FCM Push skipped: target user and chat are blank")
            return
        }

        try {
            val candidates = listOf(
                targetUserIdOrName,
                targetUserIdOrName.removePrefix("chat_").removePrefix("user_").removePrefix("@").trim(),
                chatId,
                chatId.removePrefix("chat_").removePrefix("user_").removePrefix("@").trim()
            ).filter {
                it.isNotBlank() && it != "global" && it != "bitassistant"
            }.distinct()

            var fcmToken: String? = null
            for (candidate in candidates) {
                fcmToken = SupabaseService.getFcmTokenForUser(candidate)
                if (!fcmToken.isNullOrBlank()) break
            }

            if (!fcmToken.isNullOrBlank()) {
                val cleanBody = NotificationHelper.formatCleanPreviewText(body).take(120)
                sendPushToToken(
                    targetFcmToken = fcmToken,
                    type = type,
                    title = title,
                    body = cleanBody,
                    callerName = senderName,
                    chatId = chatId,
                    callType = callType,
                    senderAvatar = senderAvatar,
                    senderId = senderId,
                    serverMessageId = serverMessageId,
                    messageType = messageType
                )
            } else {
                Log.w(TAG, "No FCM token registered for recipient '$targetUserIdOrName'")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up FCM token: ${e.message}")
        }
    }
}
