package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.call.ActiveCallBridge
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.BitChatViewModel

@Composable
fun DynamicIslandCallBanner(
    callState: ActiveCallState,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
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

    val blueGlassBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xDD1E3A8A), // Translucent Deep Royal Blue
            Color(0xDD2563EB), // Translucent Vibrant Blue
            Color(0xDD3B82F6), // Translucent Sky Blue
            Color(0xDD1D4ED8)  // Translucent Navy Blue
        ),
        startX = animatedProgress * 500f,
        endX = animatedProgress * 500f + 700f,
        tileMode = TileMode.Mirror
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp)
            .scale(if (isPressed) 0.985f else 1f),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color(0xFF60A5FA).copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(blueGlassBrush)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onExpandClick
                )
                .padding(horizontal = 12.dp, vertical = 5.dp)
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Active Call",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (callState.isConnected) "Call • ${callState.contactName.ifBlank { "Voice Call" }}" else "Calling... • ${callState.contactName.ifBlank { "Voice Call" }}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = timeFormatted,
                    color = Color(0xFF6EE7B7),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}


@Composable
fun ActiveCallBulletinSlot(
    viewModel: BitChatViewModel,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localCallState by viewModel.activeCall.collectAsState()
    val bridgeCall by ActiveCallBridge.state.collectAsState()

    // CallActivity uses its own ViewModel instance. When an incoming receiver
    // presses Back, MainActivity returns with its ViewModel still idle, while
    // the process-local bridge still contains the live call. Use the bridge
    // as the fallback source so the bulletin is visible on the receiver too.
    var bridgeElapsedSeconds by remember(bridgeCall?.callId) { mutableIntStateOf(0) }

    LaunchedEffect(bridgeCall?.callId, bridgeCall?.startedAt, bridgeCall?.isConnected) {
        val currentBridge = bridgeCall
        if (currentBridge == null) {
            bridgeElapsedSeconds = 0
            return@LaunchedEffect
        }
        while (true) {
            bridgeElapsedSeconds = ((System.currentTimeMillis() - currentBridge.startedAt)
                .coerceAtLeast(0L) / 1000L).toInt()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val effectiveCallState = if (localCallState.isActive) {
        localCallState
    } else {
        bridgeCall?.let {
            ActiveCallState(
                isActive = true,
                isConnected = it.isConnected,
                contactId = it.peerId,
                contactName = it.peerName,
                callType = it.callType,
                secondsElapsed = bridgeElapsedSeconds
            )
        } ?: localCallState
    }

    AnimatedVisibility(
        visible = effectiveCallState.isActive && effectiveCallState.callType == "AUDIO",
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 220)
        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 220)
        ) + fadeOut(animationSpec = tween(durationMillis = 150)),
        modifier = modifier.fillMaxWidth()
    ) {
        DynamicIslandCallBanner(
            callState = effectiveCallState,
            onExpandClick = onExpandClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
