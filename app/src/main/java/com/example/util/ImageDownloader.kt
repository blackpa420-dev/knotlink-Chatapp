package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloader {
    suspend fun saveImageToDevice(context: Context, imageUrlOrUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val raw = if (imageUrlOrUri.contains("[IMAGE_ATTACHMENT|")) {
                imageUrlOrUri.substringAfter("[IMAGE_ATTACHMENT|").substringBefore("]").substringBefore(" ").trim()
            } else imageUrlOrUri.trim()

            if (raw.isBlank()) return@withContext false

            val bytes: ByteArray = when {
                raw.startsWith("content://") || raw.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(raw))?.use { it.readBytes() } ?: return@withContext false
                }
                raw.startsWith("http://") || raw.startsWith("https://") -> {
                    val url = URL(raw)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 20000
                    conn.inputStream.use { it.readBytes() }
                }
                else -> return@withContext false
            }

            if (bytes.isEmpty()) return@withContext false

            val filename = "BitChat_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BitChat")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ImageDownloader", "Failed to save image: ${e.message}", e)
            false
        }
    }
}
