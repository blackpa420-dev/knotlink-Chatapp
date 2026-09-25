package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.random.Random

data class StitchParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    val baseAlpha: Float
)

@Composable
fun InteractiveStitchParticlesBackground(
    modifier: Modifier = Modifier
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080B))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed) {
                                touchPos = change.position
                            } else {
                                touchPos = null
                            }
                        } else {
                            touchPos = null
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val touch = touchPos
            val numNodes = 24
            val rad = Math.toRadians(phase.toDouble()).toFloat()

            for (i in 0 until numNodes) {
                val factor = (i.toFloat() / numNodes) * 2f * Math.PI.toFloat()
                val px = (w * 0.5f) + (w * 0.38f) * kotlin.math.cos(factor + rad * (1f + (i % 3) * 0.3f))
                val py = (h * 0.45f) + (h * 0.35f) * kotlin.math.sin(factor * 1.5f + rad)

                val nodeCenter = Offset(px, py)
                val nodeAlpha = 0.35f + 0.3f * kotlin.math.sin(rad * 2f + i).coerceIn(0f, 1f)

                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = nodeAlpha),
                    radius = 3.5.dp.toPx(),
                    center = nodeCenter
                )

                // Connect to touch if near
                if (touch != null) {
                    val dist = hypot(touch.x - px, touch.y - py)
                    if (dist < 320f) {
                        val lineAlpha = (1f - (dist / 320f)) * 0.75f
                        drawLine(
                            color = Color(0xFF34D399).copy(alpha = lineAlpha),
                            start = nodeCenter,
                            end = touch,
                            strokeWidth = 1.6.dp.toPx()
                        )
                    }
                }
            }

            if (touch != null) {
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                    radius = 36.dp.toPx(),
                    center = touch
                )
                drawCircle(
                    color = Color(0xFF34D399).copy(alpha = 0.8f),
                    radius = 8.dp.toPx(),
                    center = touch
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    onCreateAccountClick: () -> Unit,
    onLogInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typedText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val fullText = "KnotLink"
        while (isActive) {
            for (i in 1..fullText.length) {
                typedText = fullText.take(i)
                delay(120)
            }
            delay(2500)
            for (i in fullText.length - 1 downTo 0) {
                typedText = fullText.take(i)
                delay(70)
            }
            delay(400)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            showCursor = !showCursor
            delay(450)
        }
    }

    val dynamicWords = remember { listOf("secure", "encrypted", "private", "instant", "seamless", "direct") }
    var wordIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2200)
            wordIndex = (wordIndex + 1) % dynamicWords.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Interactive Google Stitch Dotted Particles Background
        InteractiveStitchParticlesBackground(
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF07080B).copy(alpha = 0.15f),
                            Color(0xFF07080B).copy(alpha = 0.5f),
                            Color(0xFF07080B).copy(alpha = 0.88f),
                            Color(0xFF07080B)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                val cursorStr = if (showCursor) "|" else " "

                Text(
                    text = buildAnnotatedString {
                        append("Welcome to ")
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        ) {
                            append(typedText)
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF9EA3B0),
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            append(cursorStr)
                        }
                    },
                    color = BitOnSurface,
                    fontSize = 24.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "The ultimate ",
                        color = BitOnSurfaceVariant,
                        fontSize = 15.sp
                    )
                    AnimatedContent(
                        targetState = wordIndex,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = "wordWheel"
                    ) { idx ->
                        Text(
                            text = dynamicWords[idx],
                            color = Color(0xFF10B981),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                    Text(
                        text = " chatting experience",
                        color = BitOnSurfaceVariant,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "Connect instantly with friends with zero trace.",
                    color = BitOnSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val createInteractionSource = remember { MutableInteractionSource() }
                val isCreatePressed by createInteractionSource.collectIsPressedAsState()
                val createScale by animateFloatAsState(
                    targetValue = if (isCreatePressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "createScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(
                            scaleX = createScale,
                            scaleY = createScale
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                            )
                        )
                        .clickable(
                            interactionSource = createInteractionSource,
                            indication = ripple(color = Color.Black),
                            onClick = onCreateAccountClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create Account",
                            color = Color(0xFF07080B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF07080B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val loginInteractionSource = remember { MutableInteractionSource() }
                val isLoginPressed by loginInteractionSource.collectIsPressedAsState()
                val loginScale by animateFloatAsState(
                    targetValue = if (isLoginPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "loginScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(
                            scaleX = loginScale,
                            scaleY = loginScale
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF10131B).copy(alpha = 0.95f))
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                            ),
                            RoundedCornerShape(28.dp)
                        )
                        .clickable(
                            interactionSource = loginInteractionSource,
                            indication = ripple(),
                            onClick = onLogInClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log In",
                        color = Color(0xFFF4F4F6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = buildAnnotatedString {
                        append("By continuing, you agree to our ")
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        ) {
                            append("Terms of Service")
                        }
                        append(" and ")
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        ) {
                            append("Privacy Policy")
                        }
                    },
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
