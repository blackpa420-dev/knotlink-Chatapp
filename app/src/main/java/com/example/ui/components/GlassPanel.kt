package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSurfaceContainer

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = Color.Unspecified,
    isNightMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val actualBgColor = if (backgroundColor != Color.Unspecified) {
        backgroundColor
    } else {
        if (isNightMode) BitSurfaceContainer.copy(alpha = 0.60f) else Color.White
    }

    val actualBorderColor = if (borderColor != Color.Unspecified) {
        borderColor
    } else {
        if (isNightMode) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    }

    val shape = RoundedCornerShape(cornerRadius)
    var boxModifier = modifier
        .then(
            if (!isNightMode) {
                Modifier.shadow(
                    elevation = 3.dp,
                    shape = shape,
                    ambientColor = Color(0x140F172A),
                    spotColor = Color(0x140F172A)
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(actualBgColor, shape)
        .border(borderWidth, actualBorderColor, shape)

    if (onClick != null) {
        boxModifier = boxModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = boxModifier,
        content = content
    )
}

@Composable
fun GlowingGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glowColor: Color = BitPrimary.copy(alpha = 0.3f),
    isNightMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = if (isNightMode) BitSurfaceContainer.copy(alpha = 0.70f) else Color.White
    GlassPanel(
        modifier = modifier,
        cornerRadius = cornerRadius,
        borderColor = glowColor,
        borderWidth = 1.dp,
        backgroundColor = bg,
        isNightMode = isNightMode,
        onClick = onClick,
        content = content
    )
}

