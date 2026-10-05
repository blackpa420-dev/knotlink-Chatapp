package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NothingNumpad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    val keyBg = if (isDarkTheme) Color(0xFF16181E) else Color(0xFFF1F5F9)
    val keyBorder = if (isDarkTheme) Color(0xFF23262F) else Color(0xFFCBD5E1)
    val textColor = if (isDarkTheme) Color(0xFFF4F4F6) else Color(0xFF0F172A)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        digits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { digit ->
                    NumpadKeyButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigitClick(digit)
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = keyBg,
                        borderColor = keyBorder
                    ) {
                        Text(
                            text = digit,
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // Bottom row: More/Extra, 0, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Extra / Blank key
            NumpadKeyButton(
                onClick = { onMoreClick?.invoke() },
                modifier = Modifier.weight(1f),
                backgroundColor = if (onMoreClick != null) keyBg else Color.Transparent,
                borderColor = if (onMoreClick != null) keyBorder else Color.Transparent
            ) {
                if (onMoreClick != null) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = if (isDarkTheme) Color(0xFF8E8E93) else Color(0xFF64748B)
                    )
                }
            }

            // Zero key
            NumpadKeyButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigitClick("0")
                },
                modifier = Modifier.weight(1f),
                backgroundColor = keyBg,
                borderColor = keyBorder
            ) {
                Text(
                    text = "0",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Backspace key
            NumpadKeyButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspaceClick()
                },
                modifier = Modifier.weight(1f),
                backgroundColor = keyBg,
                borderColor = keyBorder
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = if (isDarkTheme) Color(0xFFF4F4F6) else Color(0xFF334155),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NumpadKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "NumpadKeyScale"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isPressed && backgroundColor != Color.Transparent) Color(0xFF2563EB).copy(alpha = 0.15f) else backgroundColor,
        animationSpec = tween(110),
        label = "NumpadKeyBg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isPressed && borderColor != Color.Transparent) Color(0xFF2563EB) else borderColor,
        animationSpec = tween(110),
        label = "NumpadKeyBorder"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .scale(animatedScale)
            .clip(CircleShape)
            .background(animatedBgColor)
            .border(
                width = 1.dp,
                color = animatedBorderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
