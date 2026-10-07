package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap
import kotlin.math.min

/**
 * KnotLink's canonical profile QR renderer.
 *
 * Payload is ONLY the immutable Supabase Auth UUID.  The visual renderer is
 * deliberately custom, but the underlying QR module matrix remains untouched,
 * so standard QR readers can decode it reliably.
 */
object QRCodeGenerator {

    private val uuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )

    fun generateProfileQRCode(
        userId: String,
        context: Context? = null,
        logoBitmap: Bitmap? = null,
        size: Int = 768
    ): Bitmap {
        val canonicalUserId = userId.trim()
        require(uuidRegex.matches(canonicalUserId)) {
            "KnotLink QR requires the authenticated Supabase UUID"
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 3)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val matrix = QRCodeWriter().encode(
            canonicalUserId,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Rounded modules give KnotLink a distinct Telegram-like visual language
        // without inserting a logo or altering the QR's logical module matrix.
        val module = min(size.toFloat() / matrix.width, size.toFloat() / matrix.height)
        val radius = module * 0.30f
        val inset = module * 0.035f

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (!matrix.get(x, y)) continue
                val left = x * module + inset
                val top = y * module + inset
                val right = (x + 1) * module - inset
                val bottom = (y + 1) * module - inset
                canvas.drawRoundRect(
                    RectF(left, top, right, bottom),
                    radius,
                    radius,
                    paint
                )
            }
        }

        return bitmap
    }
}
