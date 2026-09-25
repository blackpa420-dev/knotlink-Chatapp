package com.example.data.cloudflare

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CloudflareR2Config {
    var ACCOUNT_ID: String = "4114666a87ef501b3d043a8351e5c1f7"
    var ACCESS_KEY_ID: String = "0d51ff5285c20ea98e6528883cd7d41c"
    var SECRET_ACCESS_KEY: String = "af944e5dc3d6799dcc3e15948916d48749ad66df9f3238fb3d20edff0c026b70"
    var BUCKET_NAME: String = "knotlink-media"
    var PUBLIC_DEV_URL: String = "https://pub-70e023b4fd5d4025a978d48768b63499.r2.dev"

    val S3_ENDPOINT: String
        get() = if (ACCOUNT_ID.isNotBlank()) "https://$ACCOUNT_ID.r2.cloudflarestorage.com" else ""
}

object CloudflareR2Service {
    private const val TAG = "CloudflareR2"
    private val httpClient = OkHttpClient()

    /**
     * Uploads bytes directly to Cloudflare R2 bucket using AWS SigV4 authorization.
     * Returns the public media URL.
     */
    suspend fun uploadFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg",
        folder: String = "chat_media"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bucket = CloudflareR2Config.BUCKET_NAME
            val accountId = CloudflareR2Config.ACCOUNT_ID
            val accessKey = CloudflareR2Config.ACCESS_KEY_ID
            val secretKey = CloudflareR2Config.SECRET_ACCESS_KEY
            val publicUrl = CloudflareR2Config.PUBLIC_DEV_URL.trim().removeSuffix("/")

            if (bucket.isBlank() || accessKey.isBlank() || secretKey.isBlank() || accountId.isBlank()) {
                Log.w(TAG, "R2 credentials not fully configured yet.")
                // Return placeholder or failure
                return@withContext Result.failure(Exception("Cloudflare R2 credentials not configured"))
            }

            val objectKey = "$folder/${System.currentTimeMillis()}_$fileName"
            val host = "$accountId.r2.cloudflarestorage.com"
            val endpoint = "https://$host/$bucket/$objectKey"

            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val amzDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            // Payload SHA256
            val payloadHash = sha256Hex(bytes)

            // Canonical Request
            val canonicalUri = "/$bucket/$objectKey"
            val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
            val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            // String to Sign
            val credentialScope = "$dateStamp/auto/s3/aws4_request"
            val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            // Signature
            val signingKey = getSignatureKey(secretKey, dateStamp, "auto", "s3")
            val signature = hmacSha256Hex(signingKey, stringToSign)

            val authorizationHeader = "AWS4-HMAC-SHA256 Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Host", host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", payloadHash)
                .addHeader("Authorization", authorizationHeader)
                .put(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Upload error"
                Log.e(TAG, "R2 Upload failed: ${response.code} - $errorBody")
                return@withContext Result.failure(Exception("Upload failed: ${response.code}"))
            }

            val finalUrl = if (publicUrl.isNotBlank()) {
                "$publicUrl/$objectKey"
            } else {
                "https://$host/$bucket/$objectKey"
            }

            Log.i(TAG, "Uploaded to R2 successfully: $finalUrl")
            Result.success(finalUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during R2 upload", e)
            Result.failure(e)
        }
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        return hmacSha256(key, data).joinToString("") { "%02x".format(it) }
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }
}
