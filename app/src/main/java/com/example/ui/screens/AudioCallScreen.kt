package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassPanel
import com.example.ui.viewmodel.BitChatViewModel
import com.example.util.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun AudioCallScreen(
    contactId: String = "alex",
    contactName: String = "Alex Rivera",
    viewModel: BitChatViewModel? = null,
    onBackClick: () -> Unit = {},
    onEndCallClick: () -> Unit,
    onChatClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    val activeCall by (viewModel?.activeCall ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.example.ui.viewmodel.ActiveCallState()) }).collectAsState()
    val secondsElapsed = activeCall.secondsElapsed

    var hasCallPermissions by remember {
        mutableStateOf(PermissionUtils.hasCallPermissions(context, isVideo = false))
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = PermissionUtils.hasCallPermissions(context, isVideo = false)
        hasCallPermissions = granted
        if (granted && viewModel?.activeCall?.value?.isActive != true) {
            viewModel?.startCall(
                contactId = contactId,
                contactName = contactName,
                callType = "AUDIO"
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCallPermissions) {
            callPermissionLauncher.launch(PermissionUtils.getCallPermissions(isVideo = false))
        } else if (viewModel?.activeCall?.value?.isActive != true) {
            viewModel?.startCall(
                contactId = contactId,
                contactName = contactName,
                callType = "AUDIO"
            )
        }
    }

    LaunchedEffect(activeCall.isActive) {
        if (!activeCall.isActive && secondsElapsed > 1) {
            onEndCallClick()
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    // Pulsing animation for active call audio waves around avatar
    val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1117),
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Top Header Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPanel(
                modifier = Modifier.size(44.dp),
                cornerRadius = 22.dp,
                backgroundColor = Color.White.copy(alpha = 0.1f),
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encrypted",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(44.dp))
        }

        // Center Profile & Call Details (Lifted Higher up as requested)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulse outer rings
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(waveScale)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(waveScale * 0.9f)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.22f))
                )

                // Main Avatar Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF00C6FF))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = activeCall.contactAvatar
                    if (avatarUrl.isNotBlank()) {
                        coil.compose.SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Contact avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            loading = {
                                Text(text = contactName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                            },
                            error = {
                                Text(text = contactName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                    } else {
                        Text(
                            text = contactName.take(1).uppercase().ifBlank { "U" },
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = contactName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Timer / Ringing Status Display
            val statusText = when (activeCall.callStatus) {
                "CONNECTED" -> timeFormatted
                "BUSY" -> "User Busy / Declined"
                "TIMEOUT" -> "No Answer"
                "ENDED" -> "Call Ended"
                else -> "Ringing..."
            }
            val statusColor = when (activeCall.callStatus) {
                "CONNECTED" -> Color(0xFF60A5FA)
                "BUSY", "TIMEOUT", "ENDED" -> Color(0xFFEF4444)
                else -> Color(0xFFF59E0B)
            }
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Talking Voice Beats Waveform Line
            LiveVoiceBeatsWaveform(isMuted = isMuted)
        }

        // Bottom Call Controls Navbar (5 Buttons with End Call Centered & Dark Red)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            GlassPanel(
                cornerRadius = 38.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                borderColor = Color.White.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. MUTE
                    CallNavButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "MUTED" else "MUTE",
                        isActive = isMuted,
                        buttonSize = 48,
                        activeBgColor = Color(0xFFEF4444),
                        onClick = {
                            isMuted = !isMuted
                            viewModel?.toggleMute()
                        }
                    )

                    // 2. SPEAKER
                    CallNavButton(
                        icon = Icons.Default.VolumeUp,
                        label = "SPEAKER",
                        isActive = isSpeakerOn,
                        buttonSize = 48,
                        activeBgColor = Color(0xFF2563EB),
                        onClick = {
                            isSpeakerOn = !isSpeakerOn
                            viewModel?.toggleSpeaker()
                        }
                    )

                    // 3. END CALL (CENTERED, BIGGER, SOFT DARK RED)
                    CallNavButton(
                        icon = Icons.Default.CallEnd,
                        label = "END",
                        isActive = true,
                        isCenterEndButton = true,
                        buttonSize = 64,
                        activeBgColor = Color(0xFF881337), // Soft Deep Dark Red
                        onClick = {
                            onEndCallClick()
                        }
                    )

                    // 4. ADD CALL
                    CallNavButton(
                        icon = Icons.Default.PersonAdd,
                        label = "ADD",
                        isActive = false,
                        buttonSize = 48,
                        onClick = { /* Add call action */ }
                    )

                    // 5. CHAT (Keep call active & open chat)
                    CallNavButton(
                        icon = Icons.Default.Chat,
                        label = "CHAT",
                        isActive = false,
                        buttonSize = 48,
                        onClick = onChatClick
                    )
                }
            }
        }
    }
}

@Composable
fun CallNavButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    buttonSize: Int = 48,
    isCenterEndButton: Boolean = false,
    activeBgColor: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    val containerBg by animateColorAsState(
        targetValue = when {
            isCenterEndButton -> activeBgColor
            isActive -> activeBgColor
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(250),
        label = "container_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scaleAnim)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize.dp)
                .clip(CircleShape)
                .background(containerBg)
                .border(
                    width = if (isCenterEndButton) 1.5.dp else 1.dp,
                    color = if (isCenterEndButton) Color(0xFFF87171).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (isCenterEndButton) 28.dp else 22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isCenterEndButton) Color(0xFFF87171).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.8f),
            fontSize = if (isCenterEndButton) 11.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LiveVoiceBeatsWaveform(isMuted: Boolean = false) {
    var isSpeaking by remember { mutableStateOf(!isMuted) }

    LaunchedEffect(isMuted) {
        if (isMuted) {
            isSpeaking = false
        } else {
            while (!isMuted) {
                isSpeaking = true
                delay(1600)
                isSpeaking = false
                delay(500)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voice_beats")

    val h1Anim by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb1"
    )
    val h2Anim by infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb2"
    )
    val h3Anim by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb3"
    )
    val h4Anim by infiniteTransition.animateFloat(
        initialValue = 30f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb4"
    )
    val h5Anim by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb5"
    )
    val h6Anim by infiniteTransition.animateFloat(
        initialValue = 26f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(440, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb6"
    )
    val h7Anim by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hb7"
    )

    val isActiveSpeaking = isSpeaking && !isMuted

    val animatedH1 by animateFloatAsState(if (isActiveSpeaking) h1Anim else 6f, animationSpec = tween(200), label = "h1")
    val animatedH2 by animateFloatAsState(if (isActiveSpeaking) h2Anim else 6f, animationSpec = tween(200), label = "h2")
    val animatedH3 by animateFloatAsState(if (isActiveSpeaking) h3Anim else 6f, animationSpec = tween(200), label = "h3")
    val animatedH4 by animateFloatAsState(if (isActiveSpeaking) h4Anim else 6f, animationSpec = tween(200), label = "h4")
    val animatedH5 by animateFloatAsState(if (isActiveSpeaking) h5Anim else 6f, animationSpec = tween(200), label = "h5")
    val animatedH6 by animateFloatAsState(if (isActiveSpeaking) h6Anim else 6f, animationSpec = tween(200), label = "h6")
    val animatedH7 by animateFloatAsState(if (isActiveSpeaking) h7Anim else 6f, animationSpec = tween(200), label = "h7")

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val barColors = listOf(
            Color(0xFF2563EB),
            Color(0xFF10B981),
            Color(0xFF60A5FA),
            Color(0xFF2563EB),
            Color(0xFF10B981),
            Color(0xFF60A5FA),
            Color(0xFF2563EB)
        )
        val heights = listOf(animatedH1, animatedH2, animatedH3, animatedH4, animatedH5, animatedH6, animatedH7)

        heights.forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(h.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) Color(0xFF6B7280) else barColors[index])
            )
        }
    }
}
