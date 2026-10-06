package com.example.data.cloudflare

import android.util.Log
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

/**
 * Cloudflare R2 client.
 *
 * R2 credentials are never stored in the APK. An authenticated Supabase
 * Edge Function issues a short-lived presigned PUT URL and the device
 * uploads bytes directly to R2.
 */
object CloudflareR2Config {
    const val BUCKET_NAME = "knotlink-media"
    const val PRESIGN_FUNCTION = "r2-media-url"
}

object CloudflareR2Service {
    private const val TAG = "CloudflareR2"
    private val httpClient = OkHttpClient()

    private fun safeFileName(fileName: String): String {
        val cleaned = fileName.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(120)
        return if (cleaned.isBlank()) "media.bin" else cleaned
    }

    private suspend fun presign(
        action: String,
        objectKey: String,
        contentType: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseService.getValidAccessToken().getOrElse {
                return@withContext Result.failure(it)
            }

            val body = JSONObject().apply {
                put("action", action)
                put("objectKey", objectKey)
                put("contentType", contentType)
                put("expiresIn", 900)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.PROJECT_URL}/functions/v1/${CloudflareR2Config.PRESIGN_FUNCTION}")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Presign failed: HTTP ${response.code}: $responseBody")
                    val serverMessage = try {
                        JSONObject(responseBody).optString("error").ifBlank { responseBody.take(300) }
                    } catch (_: Exception) { responseBody.take(300) }
                    return@withContext Result.failure(Exception("R2 presign failed (${response.code}): $serverMessage"))
                }
                Result.success(JSONObject(responseBody))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Presign request failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg",
        folder: String = "chat_media",
        chatId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = SupabaseService.getAuthenticatedUserId()
                ?: return@withContext Result.failure(Exception("Authenticated user required"))

            val normalizedFolder = folder.trim().trim('/').ifBlank { "chat_media" }
            val objectKey = when (normalizedFolder) {
                "avatar" -> "users/$userId/avatar/${UUID.randomUUID()}-${safeFileName(fileName)}"
                "chat_media" -> {
                    val canonicalChatId = chatId?.trim().orEmpty()
                    if (canonicalChatId.isBlank()) {
                        return@withContext Result.failure(Exception("Chat UUID required for chat media"))
                    }
                    "chats/$canonicalChatId/media/$userId/${UUID.randomUUID()}-${safeFileName(fileName)}"
                }
                else -> return@withContext Result.failure(Exception("Unsupported R2 media scope"))
            }

            val presigned = presign("upload", objectKey, mimeType).getOrElse {
                return@withContext Result.failure(it)
            }

            val signedUrl = presigned.optString("url", "")
            if (signedUrl.isBlank()) {
                return@withContext Result.failure(Exception("R2 upload URL missing"))
            }

            val request = Request.Builder()
                .url(signedUrl)
                .addHeader("Content-Type", mimeType)
                .put(bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    Log.e(TAG, "R2 upload failed: HTTP ${response.code}: $errorBody")
                    return@withContext Result.failure(Exception("R2 upload failed (${response.code}): ${errorBody.take(300)}"))
                }
            }

            Log.d(TAG, "R2 upload complete: $objectKey")
            // Keep chat media private. The server issues a short-lived signed GET URL
            // when the media is actually needed.
            Result.success(objectKey)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during R2 upload", e)
            Result.failure(e)
        }
    }

    suspend fun getDownloadUrl(
        objectKey: String,
        contentType: String = "application/octet-stream"
    ): Result<String> = withContext(Dispatchers.IO) {
        val result = presign("download", objectKey, contentType)
        result.map { it.optString("url", "") }.mapCatching { url ->
            if (url.isBlank()) throw Exception("R2 download URL missing")
            url
        }
    }
}
