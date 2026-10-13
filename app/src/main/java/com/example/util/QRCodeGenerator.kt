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
import kotlin.math.hypot

/**
 * KnotLink's canonical profile QR renderer (Telegram-style).
 *
 * Payload is ONLY the immutable Supabase Auth UUID. The module matrix is the
 * untouched ZXing matrix, so standard QR readers decode it. Visual language:
 *  - round dots instead of squares,
 *  - rounded "ring" finder eyes,
 *  - a clear circular hole in the middle where the UI overlays the animated
 *    KnotLink logo (see ui/components/KnotLinkQrCode.kt).
 *
 * Error correction is level H so the hole in the middle is always recoverable.
 * Pure black ink on pure white paper, no shadows, so it scans fast.
 */
object QRCodeGenerator {

    /** Diameter of the empty centre hole as a fraction of the bitmap width. */
    const val LOGO_CLEAR_FRACTION = 0.24f

    private const val QUIET_MODULES = 1
    private const val DOT_RADIUS = 0.46f

    @Suppress("UNUSED_PARAMETER")
    fun generateProfileQRCode(
        userId: String,
        context: Context? = null,
        logoBitmap: Bitmap? = null,
        size: Int = 768
    ): Bitmap {
        val canonicalUserId = userId.trim()
        if (canonicalUserId.isBlank()) {
            val emptyBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(emptyBitmap).drawColor(Color.WHITE)
            return emptyBitmap
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 0)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        // Requesting a 1x1 output makes ZXing return the raw module matrix (no upscaling),
        // which is what we need to draw our own shapes.
        val matrix = QRCodeWriter().encode(canonicalUserId, BarcodeFormat.QR_CODE, 1, 1, hints)
        val n = matrix.width
        val total = n + QUIET_MODULES * 2
        val cell = size.toFloat() / total

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val paper = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Finder "eyes": 7x7 ring (1 module), 5x5 white, 3x3 core. Same 1:1:3:1:1 ratio as a
        // standard finder pattern, just with rounded corners.
        fun drawEye(moduleX: Int, moduleY: Int) {
            val l = (QUIET_MODULES + moduleX) * cell
            val t = (QUIET_MODULES + moduleY) * cell
            canvas.drawRoundRect(RectF(l, t, l + 7 * cell, t + 7 * cell), 2.4f * cell, 2.4f * cell, ink)
            canvas.drawRoundRect(RectF(l + cell, t + cell, l + 6 * cell, t + 6 * cell), 1.6f * cell, 1.6f * cell, paper)
            canvas.drawRoundRect(RectF(l + 2 * cell, t + 2 * cell, l + 5 * cell, t + 5 * cell), 1.1f * cell, 1.1f * cell, ink)
        }
        drawEye(0, 0)
        drawEye(n - 7, 0)
        drawEye(0, n - 7)

        fun inEye(x: Int, y: Int): Boolean =
            (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)

        val centre = size / 2f
        val holeRadius = size * LOGO_CLEAR_FRACTION / 2f + cell * 0.4f
        val dotRadius = cell * DOT_RADIUS

        for (y in 0 until n) {
            for (x in 0 until n) {
                if (!matrix.get(x, y) || inEye(x, y)) continue
                val cx = (QUIET_MODULES + x + 0.5f) * cell
                val cy = (QUIET_MODULES + y + 0.5f) * cell
                if (hypot(cx - centre, cy - centre) < holeRadius) continue
                canvas.drawCircle(cx, cy, dotRadius, ink)
            }
        }

        return bitmap
    }
}
