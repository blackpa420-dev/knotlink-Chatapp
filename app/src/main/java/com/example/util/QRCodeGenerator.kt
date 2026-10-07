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
        val content = if (uuidRegex.matches(canonicalUserId)) {
            "KNOTLINK:USER:$canonicalUserId"
        } else {
            "KNOTLINK:INVALID"
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 1)
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

        val canvas = Canvas(qrBitmap)
        val centerX = width / 2f
        val centerY = height / 2f
        // Keep a generous circular quiet zone around the logo so the QR remains
        // reliably scannable while the KnotLink mark sits neatly inside the ring.
        val bgRadius = width * 0.125f
        val logoRadius = bgRadius * 0.72f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, bgRadius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            style = Paint.Style.STROKE
            strokeWidth = width * 0.012f
        }
        canvas.drawCircle(centerX, centerY, bgRadius, borderPaint)

        val centerLogo = logoBitmap ?: context?.let { ctx ->
            try { BitmapFactory.decodeResource(ctx.resources, R.drawable.appicon) } catch (_: Exception) { null }
        }

        if (centerLogo != null) {
            val targetLogoSize = (logoRadius * 2f).toInt().coerceAtLeast(1)
            val scaledLogo = Bitmap.createScaledBitmap(centerLogo, targetLogoSize, targetLogoSize, true)
            val left = centerX - targetLogoSize / 2f
            val top = centerY - targetLogoSize / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Render the app icon as a true rounded/circular mark inside the
            // existing white circular quiet zone instead of letting a square
            // icon touch the surrounding QR modules.
            canvas.save()
            val clipPath = Path().apply {
                addCircle(centerX, centerY, logoRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(scaledLogo, left, top, paint)
            canvas.restore()
        } else {
            val logoSize = width * 0.18f
            val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2563EB")
                style = Paint.Style.FILL
            }
            val iconRadius = logoSize * 0.45f
            val rect = RectF(centerX - iconRadius, centerY - iconRadius, centerX + iconRadius, centerY + iconRadius)
            canvas.drawRoundRect(rect, iconRadius * 0.4f, iconRadius * 0.4f, logoPaint)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = logoSize * 0.55f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val metrics = textPaint.fontMetrics
            val textY = centerY - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText("K", centerX, textY, textPaint)
        }

        return qrBitmap
    }
}