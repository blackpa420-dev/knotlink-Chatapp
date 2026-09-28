package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.GlassPanel
import com.example.ui.theme.BitPrimary
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.BitChatViewModel
import com.example.webrtc.CallEngineState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

@Composable
fun VideoCallScreen(
    contactId: String = "alex",
    contactName: String = "Alex Rivera",
    viewModel: BitChatViewModel? = null,
    isInPipMode: Boolean = false,
    onBackClick: () -> Unit = {},
    onEndCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current


    val callEngineState by (viewModel?.callEngineState ?: remember { MutableStateFlow(CallEngineState()) }).collectAsState()
    val activeCall by (viewModel?.activeCall ?: remember { MutableStateFlow(ActiveCallState()) }).collectAsState()
    val localTrack by (viewModel?.callEngine?.localVideoTrack ?: remember { MutableStateFlow(null) }).collectAsState()
    val remoteTrack by (viewModel?.callEngine?.remoteVideoTrack ?: remember { MutableStateFlow(null) }).collectAsState()

    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    // Auto-hide UI controls after 3 seconds of inactivity (Rule 1)
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isControlsVisible, lastInteractionTime) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // Permissions check and request
    var hasCallPermissions by remember {
        mutableStateOf(com.example.util.PermissionUtils.hasCallPermissions(context, isVideo = true))
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = com.example.util.PermissionUtils.hasCallPermissions(context, isVideo = true)
        hasCallPermissions = granted
        if (granted) {
            viewModel?.callEngine?.ensureCameraStarted(isVideo = true)
            if (viewModel?.activeCall?.value?.isActive != true) {
                viewModel?.startCall(
                    contactId = contactId,
                    contactName = contactName,
                    callType = "VIDEO"
                )
            }
        }
    }

    // Initialize call or continue active call
    LaunchedEffect(Unit) {
        if (!hasCallPermissions) {
            callPermissionLauncher.launch(com.example.util.PermissionUtils.getCallPermissions(isVideo = true))
        } else {
            viewModel?.callEngine?.ensureCameraStarted(isVideo = true)
            if (viewModel?.activeCall?.value?.isActive != true) {
                viewModel?.startCall(
                    contactId = contactId,
                    contactName = contactName,
                    callType = "VIDEO"
                )
            }
        }
    }

    val secondsElapsed = activeCall.secondsElapsed

    // Auto-exit if call terminates remotely
    LaunchedEffect(activeCall.isActive, callEngineState.isCallActive) {
        if ((!activeCall.isActive || !callEngineState.isCallActive) && secondsElapsed > 1) {
            onEndCallClick()
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    // PIP Drag position state
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    // Pulsing dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isInPipMode) Modifier.clip(RoundedCornerShape(18.dp))
                else Modifier
            )
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
                if (isControlsVisible) {
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
    ) {
        // ==========================================
        // MAIN FULLSCREEN VIDEO STREAM
        // Rule 3: Show self camera full screen BEFORE opponent answers, then switch to remoteTrack
        // ==========================================
        if (remoteTrack != null && viewModel != null) {
            // Opponent's Remote Video Stream Fullscreen
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        viewModel.callEngine.eglBaseContext?.let { eglCtx ->
                            init(eglCtx, null)
                        }
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        // Fixed-size hardware scaling is documented by WebRTC as potentially
                        // buggy on some devices. Keep the remote renderer dynamically sized.
                        setEnableHardwareScaler(true)
                        setFpsReduction(30f)
                        disableFpsReduction()
                        setMirror(false)
                        viewModel.callEngine.attachRemoteVideoSink(this)
                    }
                },
                onRelease = { renderer ->
                    viewModel.callEngine.detachRemoteVideoSink(renderer)
                    renderer.release()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasCallPermissions && !isCameraOff && localTrack != null && viewModel != null) {
            // Fullscreen Local Camera Preview BEFORE call is answered (Rule 3)
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        viewModel.callEngine.eglBaseContext?.let { eglCtx ->
                            init(eglCtx, null)
                        }
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(false)
                        disableFpsReduction()
                        setMirror(isFrontCamera)
                        viewModel.callEngine.attachLocalVideoSink(this)
                    }
                },
                update = { renderer ->
                    renderer.setMirror(isFrontCamera)
                },
                onRelease = { renderer ->
                    viewModel.callEngine.detachLocalVideoSink(renderer)
                    renderer.release()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Clean camera view without artificial glare overlays
        } else {
            // Dark gradient fallback if camera is off
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF0B0F19), Color(0xFF020617))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val avatarUrl = activeCall.contactAvatar
                    if (avatarUrl.isNotBlank()) {
                        coil.compose.SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Contact avatar",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(CircleShape),
                            error = { Text(text = contactName.take(1).uppercase().ifBlank { "U" }, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = contactName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Calling...",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ==========================================
        // FLOATING PIP MINI PREVIEW (SELF CAMERA WHEN REMOTE CONNECTED)
        // Rebuilt with TextureView-based WebRTC Renderer for 100% smooth curved rounded corners
        // ==========================================
        if (!isInPipMode && remoteTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 28.dp, end = 14.dp)
                    .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pipOffsetX += dragAmount.x
                            pipOffsetY += dragAmount.y
                        }
                    }
                    .size(96.dp, 158.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0B0E14))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            ) {
                if (hasCallPermissions && !isCameraOff && localTrack != null && viewModel != null) {
                    // TextureView-based WebRTC Renderer (Respects View Hierarchy Clipping)
                    AndroidView(
                        factory = { ctx ->
                            com.example.ui.components.WebRtcTextureView(ctx, cornerRadiusDp = 18f).apply {
                                viewModel.callEngine.eglBaseContext?.let { eglCtx ->
                                    init(eglCtx)
                                }
                                setMirror(isFrontCamera)
                                viewModel.callEngine.attachLocalVideoSink(this)
                            }
                        },
                        update = { textureView ->
                            textureView.setMirror(isFrontCamera)
                        },
                        onRelease = { textureView ->
                            viewModel.callEngine.detachLocalVideoSink(textureView)
                            textureView.release()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Camera Off",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // TOP NAVIGATION HEADER (AUTO-HIDES AFTER 3s, HIDDEN IN PIP)
        // ==========================================
        AnimatedVisibility(
            visible = !isInPipMode && isControlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassPanel(
                            modifier = Modifier.size(44.dp),
                            cornerRadius = 22.dp,
                            backgroundColor = Color.White.copy(alpha = 0.12f),
                            onClick = {
                                onBackClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = contactName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (remoteTrack != null) Color(0xFF10B981).copy(alpha = dotAlpha)
                                            else BitPrimary.copy(alpha = dotAlpha)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val statusText = when (activeCall.callStatus) {
                                    "CONNECTED" -> timeFormatted
                                    "BUSY" -> "User Busy / Declined"
                                    "TIMEOUT" -> "No Answer"
                                    "ENDED" -> "Call Ended"
                                    else -> "Ringing..."
                                }
                                val statusColor = when (activeCall.callStatus) {
                                    "CONNECTED" -> Color(0xFF34D399)
                                    "BUSY", "TIMEOUT", "ENDED" -> Color(0xFFEF4444)
                                    else -> BitPrimary
                                }
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // BOTTOM CALL CONTROLS BAR (AUTO-HIDES AFTER 3s, HIDDEN IN PIP)
        // Issue 1: Speaker / Earpiece button included
        // ==========================================
        AnimatedVisibility(
            visible = !isInPipMode && isControlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(modifier = Modifier.padding(bottom = 36.dp)) {
                    GlassPanel(
                        cornerRadius = 32.dp,
                        backgroundColor = Color.White.copy(alpha = 0.1f),
                        borderColor = Color.White.copy(alpha = 0.18f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Mic
                            CallControlButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (isMuted) "MUTED" else "MUTE",
                                isActive = isMuted,
                                onClick = {
                                    isMuted = !isMuted
                                    viewModel?.toggleMute()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // Speaker / In-Ear Earpiece Toggle (Issue 1)
                            CallControlButton(
                                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.Hearing,
                                label = if (isSpeakerOn) "SPEAKER" else "EARPIECE",
                                isActive = isSpeakerOn,
                                onClick = {
                                    isSpeakerOn = !isSpeakerOn
                                    viewModel?.toggleSpeaker()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // Toggle Camera On/Off
                            CallControlButton(
                                icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                label = if (isCameraOff) "OFF" else "CAMERA",
                                isActive = isCameraOff,
                                onClick = {
                                    isCameraOff = !isCameraOff
                                    viewModel?.toggleCamera()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // Flip Camera
                            CallControlButton(
                                icon = Icons.Default.FlipCameraIos,
                                label = "FLIP",
                                isActive = false,
                                onClick = {
                                    isFrontCamera = !isFrontCamera
                                    viewModel?.switchCamera()
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            )

                            // End Call
                            CallControlButton(
                                icon = Icons.Default.Call,
                                label = "END",
                                isActive = true,
                                isEndCall = true,
                                onClick = {
                                    onEndCallClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    isEndCall: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isEndCall -> Color(0xFFB91C1C)
            isActive -> Color(0xFFEF4444)
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(250),
        label = "btn_bg"
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
                .size(if (isEndCall) 56.dp else 48.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = 1.dp,
                    color = if (isEndCall) Color(0xFFEF4444).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (isEndCall) 26.dp else 20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isEndCall) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
