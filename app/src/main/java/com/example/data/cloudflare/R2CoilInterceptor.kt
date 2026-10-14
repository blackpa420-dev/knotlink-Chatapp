package com.example.data.cloudflare

import android.content.Context
import coil.intercept.Interceptor
import coil.request.ImageResult
import java.io.File
import java.net.URL

/**
 * Makes every Coil image load understand private R2 objects.
 *
 * Avatars and chat photos are stored as permanent keys ("users/<uuid>/avatar/..", "chats/<uuid>/media/..").
 * Older rows may still hold an expired presigned R2 URL. In both cases the key is extracted, the
 * object is downloaded through a fresh presigned URL once and cached on disk, so avatars show
 * everywhere (chat list, chat header, message bubbles, QR result, profile) without per-screen code.
 */
class R2CoilInterceptor(private val context: Context) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val raw = (chain.request.data as? String)?.trim().orEmpty()
        val key = extractKey(raw) ?: return chain.proceed(chain.request)
        val path = CloudflareR2Service.getCachedDownloadPath(context, key).getOrNull()
        val newData: Any = if (!path.isNullOrBlank()) File(path)
        else CloudflareR2Service.getDownloadUrl(key, "image/jpeg").getOrNull() ?: return chain.proceed(chain.request)
        return chain.proceed(chain.request.newBuilder().data(newData).build())
    }

    companion object {
        fun extractKey(raw: String): String? {
            if (raw.isBlank()) return null
            if (raw.startsWith("users/") || raw.startsWith("chats/")) return raw.substringBefore('?')
            if (raw.startsWith("https://", true) && raw.contains("r2.cloudflarestorage.com")) {
                return try {
                    val path = URL(raw).path.trimStart('/')
                    val key = path.substringAfter("${CloudflareR2Config.BUCKET_NAME}/", path)
                    if (key.startsWith("users/") || key.startsWith("chats/")) java.net.URLDecoder.decode(key, "UTF-8") else null
                } catch (_: Exception) { null }
            }
            return null
        }
    }
}
