package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ActiveCallState

@Composable
fun DynamicIslandCallBanner(
    callState: ActiveCallState,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = callState.isActive && callState.callType == "AUDIO",
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        val minutes = callState.secondsElapsed / 60
        val seconds = callState.secondsElapsed % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val infiniteTransition = rememberInfiniteTransition(label = "bannerGradient")
        val animatedProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bannerGradientProgress"
        )

        val darkGradientBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF06101E), // Ultra dark Navy
                Color(0xFF022C22), // Deep Dark Emerald
                Color(0xFF1E1B4B), // Deep Dark Indigo
                Color(0xFF0F172A)  // Dark Slate
            ),
            startX = animatedProgress * 500f,
            endX = animatedProgress * 500f + 700f,
            tileMode = TileMode.Mirror
        )

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .scale(if (isPressed) 0.985f else 1f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(darkGradientBrush)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onExpandClick
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Active Call",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (callState.isConnected) "Voice Call • ${callState.contactName.ifBlank { "Voice Call" }}" else "Calling... • ${callState.contactName.ifBlank { "Voice Call" }}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = timeFormatted,
                        color = Color(0xFF34D399),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
