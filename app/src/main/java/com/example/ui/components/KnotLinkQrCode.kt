package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.QRCodeGenerator

/**
 * Telegram-style profile QR used on every screen that shows "My QR".
 *
 * - Pure black round dots on a pure white card (no shadow, no tint), so it scans fast.
 * - The generator leaves a clear circle in the middle; the animated KnotLink logo sits there.
 * - Logo pops in with a spring, then idles with a slow breathe + tilt. The animation only runs
 *   while this composable is on screen.
 *
 * Give it a size from the caller, e.g. `Modifier.size(260.dp)`.
 */
@Composable
fun KnotLinkQrCode(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    contentDescription: String = "My KnotLink QR Code"
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(26.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        // Telegram-style center emblem: circular badge with white halo and crisp silhouette
        KnotLinkQrLogo(modifier = Modifier.fillMaxSize(QRCodeGenerator.LOGO_CLEAR_FRACTION - 0.02f))
    }
}

@Composable
private fun KnotLinkQrLogo(modifier: Modifier = Modifier) {
    val pop = remember { Animatable(0.75f) }
    LaunchedEffect(Unit) {
        pop.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(CircleShape)
            .background(Color.White)
            .padding(3.5.dp)
            .clip(CircleShape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        KnotLinkSilhouette(
            modifier = Modifier
                .fillMaxSize(0.62f),
            color = Color.White
        )
    }
}

/**
 * Authentic Telegram-style KnotLink interlocking links silhouette.
 */
@Composable
private fun KnotLinkSilhouette(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.15f).coerceAtLeast(3f)

        // Draw left link loop (tilted at -25 degrees)
        rotate(degrees = -25f, pivot = Offset(w * 0.38f, h * 0.5f)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.14f, h * 0.30f),
                size = Size(w * 0.44f, h * 0.40f),
                cornerRadius = CornerRadius(h * 0.20f, h * 0.20f),
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round
                )
            )
        }

        // Draw right link loop (tilted at 25 degrees, interlinked)
        rotate(degrees = 25f, pivot = Offset(w * 0.62f, h * 0.5f)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.42f, h * 0.30f),
                size = Size(w * 0.44f, h * 0.40f),
                cornerRadius = CornerRadius(h * 0.20f, h * 0.20f),
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
