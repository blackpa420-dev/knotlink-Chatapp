package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QRCodeGenerator {

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /**
     * Generates the canonical KnotLink profile QR.
     * The QR contains only the Supabase Auth UUID; usernames are never
     * used as an internal identity or routing key.
     */
    fun generateProfileQRCode(
        userId: String,
        context: Context? = null,
        logoBitmap: Bitmap? = null,
        size: Int = 512
    ): Bitmap {
        val canonicalUserId = userId.trim()
        require(uuidRegex.matches(canonicalUserId)) {
            "KnotLink QR requires the authenticated Supabase UUID"
        }
        val content = canonicalUserId

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 2)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        qrBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        // Keep the QR itself clean and quiet-zone-safe. The branded KnotLink
        // mark is rendered by the Compose layer as a small integrated center tile,
        // where it can animate smoothly without a circular ring that looks like
        // an unrelated overlay on top of the QR.
        return qrBitmap
    }
}