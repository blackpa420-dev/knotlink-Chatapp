package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.BitChatViewModel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

@Composable
fun FloatingVideoCallPip(
    callState: ActiveCallState,
    viewModel: BitChatViewModel,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remoteTrack by viewModel.callEngine.remoteVideoTrack.collectAsState()
    val localTrack by viewModel.callEngine.localVideoTrack.collectAsState()

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = callState.isActive && callState.callType == "VIDEO",
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.zIndex(150f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 80.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .size(104.dp, 172.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpandClick
                    )
            ) {
                if (remoteTrack != null) {
                    // Live Remote Participant Video Stream (TextureView for 100% smooth rounded corners)
                    AndroidView(
                        factory = { ctx ->
                            com.example.ui.components.WebRtcTextureView(ctx, cornerRadiusDp = 20f).apply {
                                viewModel.callEngine.eglBaseContext?.let { eglCtx ->
                                    init(eglCtx)
                                }
                                setMirror(false)
                                viewModel.callEngine.attachRemoteVideoSink(this)
                            }
                        },
                        onRelease = { textureView ->
                            viewModel.callEngine.detachRemoteVideoSink(textureView)
                            textureView.release()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    // Live Self Video Stream before remote is connected (setMirror = true for self view)
                    AndroidView(
                        factory = { ctx ->
                            com.example.ui.components.WebRtcTextureView(ctx, cornerRadiusDp = 20f).apply {
                                viewModel.callEngine.eglBaseContext?.let { eglCtx ->
                                    init(eglCtx)
                                }
                                setMirror(true)
                                viewModel.callEngine.attachLocalVideoSink(this)
                            }
                        },
                        onRelease = { textureView ->
                            viewModel.callEngine.detachLocalVideoSink(textureView)
                            textureView.release()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }
        }
    }
}
