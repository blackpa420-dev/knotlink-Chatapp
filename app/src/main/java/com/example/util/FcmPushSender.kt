package com.example.util

import android.util.Base64
import android.util.Log
import com.example.data.supabase.SupabaseService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

object FcmPushSender {

    private const val TAG = "FcmPushSender"
    private const val PROJECT_ID = "knotlink-1"
    private const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@knotlink-1.iam.gserviceaccount.com"
    private const val PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC8RC5jfE1oi+ba\nzw9f7Cg1Ya6NYKcYSkUFykL9bVmpN3+J5cHphkSeT9Znp9ydi9Oh81D7OA5Ee913\n6OYdmbEV4INddiRyBkvoqR1oZB3YqX9HQV/8dJkpQmm5aY//j+6GZ/s7TRpiAMQ0\nNs8M4iv6bQbmr2VMWfS4uoNC2CS4+gKFJ+6R8K12gcBP9OMiP8wn4mvbd2OS/SbT\nTPX14iCQMZB8QnzVVdILXVc3f2sHaAyAXnmfAScGOQ+UyfFi1KsPWo494kV846pJ\nlSRGDPAmEx7gvZ2kMumqflB3JT8MnhJYH0I9QXpWAkOBZe7oX0CwgoTYNyPEYQWj\nX+x6exVTAgMBAAECggEAGig4776Z/LAPY5BIORIVJhGL0H3AyYmsHFlVRGO4hN++\nmxiuf/UVPI+oIN+8MbF3NnWZZ0YLCW3SP/I+YpdzeLAoYEhlWOhSDKHOga4DTZKD\nKhHFtcw3aXmeOdIWXTIQuGDjEYKtazdjC0QgMVCNPq5+OnfdEaYf77iOOy/Prllk\nxuYACTb3CRnBhX/aQCM+JHCG5ha7//QjpEy9UnfDMfhKTFr9OHxbaQR1GySBxsl8\nkN/JADoF9ZisoczOch7ElW00gMWNrvYD/3cgXpDQBjm3JIH0RR/4OuTeiMXv4bX1\nEm35v4Ex+8NU0cAO8zTFrcyEq5TRfm5DBsqRD4proQKBgQDyy7VaYo8D2wZKy4DX\nezET/hFFUTXyV+j9A+eiFF1LV6/mI3VrAnBQon5Gb3ZmbXjOtY70GesZ8D/sWUBy\nCX8UYobLiLbULRoCkweC9Ltb1RhNCa71KjMqUvWyaY3xOaceZxJRKBRa5jb7/K6F\n8ZM3I+uXFLjGuNgkJr+mU+AS4QKBgQDGgUtcRcn9Y0lXQjRCiHf818XlJEBJf8mg\ny0QpqR45GzmrdAzjZI63wl1eP8X3XXAfxPshmWmaca4e51Iar4rnF50JdZ1LifdC\nNHK6lmfmaSNEQuc4HPz/Cj46OisASww98XFc8izB9LZcDdYMspkf3tHtvy2kvrtR\n8eBk58wiswKBgDmBKejIYxsEqw7X+CIRGWSkzi5et6o4TARxAlGPBTGtCQo25756\ni2NtuP6xs5c64lwDAGKsKNSx5FH0KaFYwnJvi4F1VegW7owhiqvnjuCHLgRBAOEs\nJ4Yks/CXs3iosP4wZ1Q1a+zDjc5M4ID04Gs05L2ZuNHIQdj+HHImd3HBAoGBALQB\nKPpamsk40JfdBBDVOaFBdUXNsrIzu/4gdQYmQq7cPlZ4nPtTA4wlJ4/A2t7ujy9v\n657TRAz2S0Pg1fY2+wmFwzSBwApw6JPThboni75H1uAenxemSdFoU3dvqfDRHR/K\rotb7EJUtOHSPY2wORIa/ArTJ6TT8dFbhtG8sN1O7AoGADzdQ6FBEJR+YfxyRrxJ8\nlgP+eWLUtueDyM2y52p07pLzWEJlhejQWoSJBGMKF4H+V2Nu4VpFznEjrwXjOIgx\nxG5JrbQTImzmgb1mmtlIhZgOl6HWosGe3nBidqW8zsMOWOa+jAkFTxmtX7CHsnWI\nUCwxcymTi7p6O/aT2uxz+PU=\n-----END PRIVATE KEY-----\n"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var accessTokenExpiryTime: Long = 0L

    /**
     * Obtains a valid Google OAuth2 Access Token using JWT with SHA256withRSA
     */
    @Synchronized
    private fun getAccessToken(): String? {
        val now = System.currentTimeMillis()
        if (!cachedAccessToken.isNullOrEmpty() && now < accessTokenExpiryTime) {
            return cachedAccessToken
        }

        try {
            val iatSeconds = now / 1000L
            val expSeconds = iatSeconds + 3600L

            val header = JSONObject().apply {
                put("alg", "RS256")
                put("typ", "JWT")
            }.toString().toByteArray(Charsets.UTF_8).toBase64Url()

            val payload = JSONObject().apply {
                put("iss", CLIENT_EMAIL)
                put("scope", "https://www.googleapis.com/auth/firebase.messaging")
                put("aud", "https://oauth2.googleapis.com/token")
                put("exp", expSeconds)
                put("iat", iatSeconds)
            }.toString().toByteArray(Charsets.UTF_8).toBase64Url()

            val signingInput = "$header.$payload"

            // Parse PKCS8 RSA Private Key
            val cleanPem = PRIVATE_KEY_PEM
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replace("\n", "")
                .trim()

            val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(keySpec)

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(signingInput.toByteArray(Charsets.UTF_8))
            val signatureBytes = signature.sign()
            val signatureB64 = signatureBytes.toBase64Url()

            val jwt = "$signingInput.$signatureB64"

            // Request OAuth2 Token from Google
            val requestBody = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$jwt"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val respStr = response.body?.string() ?: ""

            if (response.isSuccessful && respStr.isNotBlank()) {
                val json = JSONObject(respStr)
                val token = json.optString("access_token", "")
                if (token.isNotBlank()) {
                    cachedAccessToken = token
                    accessTokenExpiryTime = now + 3300000L // 55 minutes
                    Log.i(TAG, "Successfully acquired Google OAuth2 access token for FCM")
                    return token
                }
            } else {
                Log.e(TAG, "Failed to get Google OAuth2 token: $respStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Google OAuth2 token for FCM: ${e.message}", e)
        }
        return null
    }

    /**
     * Send FCM High Priority Push Notification to target FCM Token
     */
    fun sendPushToToken(
        targetFcmToken: String,
        type: String, // "call", "message", "call_ended", "call_declined"
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
            Log.w(TAG, "Cannot send FCM push: targetFcmToken is blank")
            return
        }

        val token = getAccessToken()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Cannot send FCM push: OAuth2 token is null")
            return
        }

        try {
            // All call lifecycle events remain data-only so FirebaseMessagingService
            // receives them while the app is backgrounded/closed.
            val isCall = type == "call" || type == "incoming_call" ||
                type == "call_ended" || type == "call_declined" || type == "call_cancelled"
            val channelId = if (type == "call" || type == "incoming_call") "knotlink_calls_channel" else "knotlink_msg_channel_v4"
            val ttl = if (isCall) "60s" else "86400s"

            val cleanBody = NotificationHelper.formatCleanPreviewText(body).take(120)

            val messageObj = JSONObject().apply {
                put("token", targetFcmToken)

                // All notifications are data-only so KnotLinkFirebaseMessagingService
                // consistently renders messages/calls and can attach profile avatars.
                put("android", JSONObject().apply {
                    put("priority", "HIGH")
                    put("ttl", ttl)
                    if (type == "message") {
                        put("notification", JSONObject().apply {
                            put("channel_id", "knotlink_msg_channel_v4")
                            put("click_action", "OPEN_CHAT")
                            if (!senderAvatar.isNullOrBlank() &&
                                (senderAvatar.startsWith("http://") || senderAvatar.startsWith("https://"))) {
                                put("image", senderAvatar)
                            }
                        })
                    }
                })

                // Message pushes use notification + data. In background/closed state
                // FCM displays the notification in the system tray; in foreground
                // onMessageReceived still handles our richer in-app notification.
                if (type == "message") {
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", cleanBody)
                        if (!senderAvatar.isNullOrBlank() &&
                            (senderAvatar.startsWith("http://") || senderAvatar.startsWith("https://"))) {
                            put("image", senderAvatar)
                        }
                    })
                }

                put("data", JSONObject().apply {
                    put("type", type)
                    put("title", title)
                    put("body", body)
                    put("caller_name", callerName)
                    put("sender_name", callerName)
                    put("chat_id", chatId)
                    put("call_id", chatId)
                    put("room_id", chatId)
                    put("call_type", callType)
                    put("sender_avatar", senderAvatar ?: "")
                    put("sender_id", senderId ?: "")
                    if (!serverMessageId.isNullOrBlank()) {
                        put("server_message_id", serverMessageId)
                    }
                    if (!messageType.isNullOrBlank()) {
                        put("message_type", messageType)
                    }
                })
            }
            val payloadObj = JSONObject().apply {
                put("message", messageObj)
            }

            val url = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
            val reqBody = payloadObj.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(reqBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            val maskedToken = if (targetFcmToken.length > 10) "${targetFcmToken.take(6)}...${targetFcmToken.takeLast(4)}" else "***"
            if (response.isSuccessful) {
                Log.i(TAG, "FCM Push successfully sent to $maskedToken (type=$type, channel=$channelId)")
            } else {
                Log.w(TAG, "FCM Push response error code ${response.code} for $maskedToken: $respBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM push to token: ${e.message}")
        }
    }

    /**
     * Send FCM Push to a recipient user by their Supabase targetUid or Username
     */
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
            Log.w(TAG, "FCM Push skipped: both targetUserIdOrName and chatId are blank.")
            return
        }
        try {
            val candidates = listOf(
                targetUserIdOrName,
                targetUserIdOrName.removePrefix("chat_").removePrefix("user_").removePrefix("@").trim(),
                chatId,
                chatId.removePrefix("chat_").removePrefix("user_").removePrefix("@").trim()
            ).filter { it.isNotBlank() && it != "global" && it != "bitassistant" }.distinct()

            var fcmToken: String? = null
            for (candidate in candidates) {
                fcmToken = SupabaseService.getFcmTokenForUser(candidate)
                if (!fcmToken.isNullOrBlank()) {
                    break
                }
            }

            if (!fcmToken.isNullOrBlank()) {
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
                Log.w(TAG, "FCM Push Diagnostic: No FCM token registered for recipient '$targetUserIdOrName' (candidates: $candidates). Receiver profile may not have an active fcm_token in Supabase.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up FCM token for $targetUserIdOrName: ${e.message}")
        }
    }

    private fun ByteArray.toBase64Url(): String {
        return Base64.encodeToString(this, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }
}
