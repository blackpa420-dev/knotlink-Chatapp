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

    /**
     * Generates a high quality QR Code bitmap with Level H error correction
     * and the official KnotLink logo in the center over a white circular background.
     */
    fun generateProfileQRCode(
        publicId: String,
        context: Context? = null,
        logoBitmap: Bitmap? = null,
        size: Int = 512
    ): Bitmap {
        val content = if (publicId.startsWith("KNOTLINK:USER:") || publicId.startsWith("BITCHAT:USER:")) publicId else "KNOTLINK:USER:$publicId"

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

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

        // Draw Center Logo with White Circular Background
        val canvas = Canvas(qrBitmap)
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. White Circular background behind logo (~12% radius = 24% width)
        val bgRadius = (width * 0.125f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, bgRadius, bgPaint)

        // Blue accent outer ring
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            style = Paint.Style.STROKE
            strokeWidth = width * 0.012f
        }
        canvas.drawCircle(centerX, centerY, bgRadius, borderPaint)

        // 2. Load center BitChat Logo (using attached image / R.drawable.appicon)
        val centerLogo = logoBitmap ?: context?.let { ctx ->
            try {
                BitmapFactory.decodeResource(ctx.resources, R.drawable.appicon)
            } catch (e: Exception) {
                null
            }
        }

        if (centerLogo != null) {
            val targetLogoSize = (width * 0.19f).toInt()
            val scaledLogo = Bitmap.createScaledBitmap(centerLogo, targetLogoSize, targetLogoSize, true)

            val left = centerX - (targetLogoSize / 2f)
            val top = centerY - (targetLogoSize / 2f)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Clip logo neatly within the circular badge area
            canvas.save()
            val clipPath = Path().apply {
                addCircle(centerX, centerY, bgRadius * 0.92f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(scaledLogo, left, top, paint)
            canvas.restore()
        } else {
            // Fallback: Custom drawn BitChat symbol if image resource cannot be loaded
            val logoSize = (width * 0.18f)
            val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2563EB")
                style = Paint.Style.FILL
            }

            val iconRadius = logoSize * 0.45f
            val rect = RectF(
                centerX - iconRadius,
                centerY - iconRadius,
                centerX + iconRadius,
                centerY + iconRadius
            )
            canvas.drawRoundRect(rect, iconRadius * 0.4f, iconRadius * 0.4f, logoPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = logoSize * 0.55f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val fontMetrics = textPaint.fontMetrics
            val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText("B", centerX, textY, textPaint)
        }

        return qrBitmap
    }
}

