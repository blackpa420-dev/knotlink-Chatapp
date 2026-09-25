package com.example.util

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class FileDetails(
    val name: String,
    val size: Long,
    val formattedSize: String,
    val extension: String,
    val mimeType: String
)

enum class DocumentCategory {
    EXCEL,
    PDF,
    WORD,
    POWERPOINT,
    ZIP,
    CODE_TEXT,
    AUDIO,
    VIDEO,
    IMAGE,
    APK,
    GENERIC
}

object FileHelper {
    fun getFileDetails(context: Context, uri: Uri): FileDetails {
        var name = "Document"
        var size = 0L

        try {
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            val resolvedName = it.getString(nameIndex)
                            if (!resolvedName.isNullOrBlank()) {
                                name = resolvedName
                            }
                        }
                        if (sizeIndex != -1) {
                            size = it.getLong(sizeIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    name = file.name
                    size = file.length()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FileHelper", "Error querying file details: ${e.message}")
        }

        if (name == "Document" || name.isBlank()) {
            val lastSegment = uri.lastPathSegment ?: ""
            if (lastSegment.isNotBlank() && lastSegment.contains(".")) {
                name = lastSegment.substringAfterLast('/')
            }
        }

        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mimeType = context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"

        return FileDetails(
            name = name,
            size = size,
            formattedSize = formatFileSize(size),
            extension = extension,
            mimeType = mimeType
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun getCategory(extension: String, mimeType: String = ""): DocumentCategory {
        val ext = extension.lowercase(Locale.ROOT)
        val mime = mimeType.lowercase(Locale.ROOT)

        return when {
            ext in listOf("xls", "xlsx", "csv", "ods") || mime.contains("spreadsheet") || mime.contains("excel") || mime.contains("csv") -> DocumentCategory.EXCEL
            ext == "pdf" || mime.contains("pdf") -> DocumentCategory.PDF
            ext in listOf("doc", "docx", "odt", "rtf") || mime.contains("word") || mime.contains("msword") -> DocumentCategory.WORD
            ext in listOf("ppt", "pptx", "odp") || mime.contains("presentation") || mime.contains("powerpoint") -> DocumentCategory.POWERPOINT
            ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") || mime.contains("zip") || mime.contains("compressed") || mime.contains("archive") -> DocumentCategory.ZIP
            ext in listOf("txt", "json", "xml", "html", "htm", "css", "js", "ts", "kt", "java", "py", "c", "cpp", "h", "cs", "php", "sql", "md", "yaml", "yml", "log") || mime.contains("text/") || mime.contains("json") -> DocumentCategory.CODE_TEXT
            ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus", "wma") || mime.startsWith("audio/") -> DocumentCategory.AUDIO
            ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "webm", "3gp") || mime.startsWith("video/") -> DocumentCategory.VIDEO
            ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg") || mime.startsWith("image/") -> DocumentCategory.IMAGE
            ext == "apk" || mime.contains("android.package-archive") -> DocumentCategory.APK
            else -> DocumentCategory.GENERIC
        }
    }

    suspend fun downloadOrGetLocalFile(context: Context, fileUrlOrUri: String, suggestedFileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val raw = fileUrlOrUri.trim()
            if (raw.isBlank()) return@withContext null

            // If it's already a local file://
            if (raw.startsWith("file://")) {
                val f = File(Uri.parse(raw).path ?: "")
                if (f.exists()) return@withContext f
            }

            val sanitizedName = (if (suggestedFileName.isNotBlank()) suggestedFileName else "document_${System.currentTimeMillis()}")
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")

            val cacheDir = File(context.cacheDir, "documents")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val targetFile = File(cacheDir, sanitizedName)
            if (targetFile.exists() && targetFile.length() > 0) {
                return@withContext targetFile
            }

            if (raw.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(raw))?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext if (targetFile.exists() && targetFile.length() > 0) targetFile else null
            }

            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                val url = URL(raw)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext if (targetFile.exists() && targetFile.length() > 0) targetFile else null
            }

            null
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "Download/save file error: ${e.message}", e)
            null
        }
    }

    suspend fun saveFileToDownloads(context: Context, fileUrlOrUri: String, suggestedFileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = downloadOrGetLocalFile(context, fileUrlOrUri, suggestedFileName) ?: return@withContext false
            val ext = file.extension.lowercase(Locale.ROOT)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BitChat")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
                out.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "Save to downloads error: ${e.message}", e)
            false
        }
    }

    fun openFile(context: Context, file: File, mimeType: String = "") {
        try {
            val ext = file.extension.lowercase(Locale.ROOT)
            val effectiveMime = if (mimeType.isNotBlank()) mimeType
            else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, effectiveMime)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(intent, "Open ${file.name} with").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this file type (${file.extension.uppercase()})", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
