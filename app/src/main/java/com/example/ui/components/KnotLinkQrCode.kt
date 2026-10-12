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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        // A little smaller than the hole the generator clears, so a white margin remains.
        KnotLinkQrLogo(modifier = Modifier.fillMaxSize(QRCodeGenerator.LOGO_CLEAR_FRACTION - 0.03f))
    }
}

@Composable
private fun KnotLinkQrLogo(modifier: Modifier = Modifier) {
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        pop.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val idle = rememberInfiniteTransition(label = "knotlinkQrLogo")
    val tilt by idle.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    val breathe by idle.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = pop.value * breathe
                scaleX = s
                scaleY = s
                rotationZ = tilt
            }
            .clip(CircleShape)
            .background(Color.White)
            .padding(3.dp)
            .clip(CircleShape)
    ) {
        Image(
            painter = painterResource(id = R.drawable.appicon),
            contentDescription = "KnotLink",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
