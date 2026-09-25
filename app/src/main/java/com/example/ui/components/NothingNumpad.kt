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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSurfaceContainer

@Composable
fun NothingNumpad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        digits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { digit ->
                    NumpadKeyButton(
                        onClick = { onDigitClick(digit) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = digit,
                            color = BitOnSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Bottom row: More/Extra, 0, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Extra / Blank key
            NumpadKeyButton(
                onClick = { onMoreClick?.invoke() },
                modifier = Modifier.weight(1f),
                backgroundColor = Color.Transparent
            ) {
                if (onMoreClick != null) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = BitOnSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Zero key
            NumpadKeyButton(
                onClick = { onDigitClick("0") },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "0",
                    color = BitOnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Backspace key
            NumpadKeyButton(
                onClick = onBackspaceClick,
                modifier = Modifier.weight(1f),
                backgroundColor = BitSurfaceContainer.copy(alpha = 0.35f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = BitPrimary
                )
            }
        }
    }
}

@Composable
private fun NumpadKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BitSurfaceContainer.copy(alpha = 0.6f),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "NumpadKeyScale"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isPressed) BitPrimary.copy(alpha = 0.22f) else backgroundColor,
        animationSpec = tween(120),
        label = "NumpadKeyBg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isPressed) BitPrimary.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(120),
        label = "NumpadKeyBorder"
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .scale(animatedScale)
            .clip(RoundedCornerShape(18.dp))
            .background(animatedBgColor)
            .border(
                width = 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(18.dp)
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

