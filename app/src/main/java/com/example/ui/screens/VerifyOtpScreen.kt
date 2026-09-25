package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassPanel
import com.example.ui.components.GlowingGlassPanel
import com.example.ui.components.NothingNumpad
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitError
import com.example.ui.theme.BitOnPrimary
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.viewmodel.BitChatViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun VerifyOtpScreen(
    viewModel: BitChatViewModel,
    onVerifySuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otpCode by viewModel.enteredOtpCode.collectAsState()
    val enteredEmail by viewModel.enteredEmail.collectAsState()
    val countdown by viewModel.otpCountdown.collectAsState()
    val isVerifyingOtp by viewModel.isVerifyingOtp.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    // Check verification status whenever otpCode changes
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6) {
            viewModel.verifyFirebaseOtp(
                onSuccess = {
                    isSuccess = true
                    isError = false
                    onVerifySuccess()
                },
                onError = {
                    isError = true
                    isSuccess = false
                }
            )
        } else {
            isError = false
            isSuccess = false
        }
    }

    // Pulse animation for current active focus box
    val infiniteTransition = rememberInfiniteTransition(label = "OtpFocusPulse")
    val activeScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ActiveBoxScale"
    )

    val activeGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ActiveGlowAlpha"
    )

    // Shake offset for error state
    var shakeState by remember { mutableStateOf(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            for (i in 0..5) {
                shakeState = if (i % 2 == 0) 12f else -12f
                delay(60)
            }
            shakeState = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BitBackground)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Verify Email",
                    color = BitOnSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (enteredEmail.isNotBlank()) "Enter the 6-digit code sent to:\n$enteredEmail" else "Enter the 6-digit code sent to your email.",
                    color = BitOnSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Verification Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSuccess) Color(0xFF00FF88).copy(alpha = 0.15f)
                            else BitSecondary.copy(alpha = 0.12f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSuccess) Color(0xFF00FF88).copy(alpha = 0.4f)
                            else BitSecondary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.MarkEmailRead,
                            contentDescription = "Email Verification",
                            tint = if (isSuccess) Color(0xFF00FF88) else BitSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSuccess) "VERIFIED" else "ENTER MAIL VERIFICATION CODE",
                            color = if (isSuccess) Color(0xFF00FF88) else BitSecondary,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6-digit Animated OTP Code Boxes
                val selectedIndex by viewModel.selectedOtpIndex.collectAsState()
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val clipboardText = clipboardManager.getText()?.text?.trim()
                val isClipboard6Digits = clipboardText != null && clipboardText.length == 6 && clipboardText.all { it.isDigit() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeState.toInt(), 0) },
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(6) { index ->
                        val char = otpCode.getOrNull(index)
                        val isCurrentFocus = index == selectedIndex && !isSuccess && !isError

                        val boxScale by animateFloatAsState(
                            targetValue = when {
                                isCurrentFocus -> activeScale
                                char != null -> 1.03f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "BoxScale_$index"
                        )

                        val isComplete = otpCode.length == 6
                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isSuccess || isComplete -> Color(0xFF00FF88)
                                isError -> BitError
                                isCurrentFocus -> BitSecondary.copy(alpha = activeGlowAlpha)
                                char != null -> Color.White.copy(alpha = 0.6f)
                                else -> Color.White.copy(alpha = 0.12f)
                            },
                            animationSpec = tween(200),
                            label = "BorderColor_$index"
                        )

                        val bgColor by animateColorAsState(
                            targetValue = when {
                                isSuccess || isComplete -> Color(0xFF00FF88).copy(alpha = 0.18f)
                                isError -> BitError.copy(alpha = 0.18f)
                                isCurrentFocus -> BitSecondary.copy(alpha = 0.08f)
                                char != null -> Color.White.copy(alpha = 0.08f)
                                else -> Color(0x9916161A)
                            },
                            animationSpec = tween(200),
                            label = "BgColor_$index"
                        )

                        val textColor by animateColorAsState(
                            targetValue = when {
                                isSuccess || isComplete -> Color(0xFF00FF88)
                                isError -> BitError
                                else -> BitOnSurface
                            },
                            animationSpec = tween(200),
                            label = "TextColor_$index"
                        )

                        Box(
                            modifier = Modifier
                                .size(46.dp, 58.dp)
                                .scale(boxScale)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(
                                    width = if (isSuccess || isError || isCurrentFocus || char != null) 2.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { viewModel.setSelectedOtpIndex(index) },
                                        onLongPress = { clipboardText?.let { viewModel.setEntireOtp(it) } }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char?.toString() ?: "",
                                color = textColor,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Status Text (Green Success / Red Error)
                AnimatedVisibility(
                    visible = isSuccess || isError,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isSuccess) Color(0xFF00FF88) else BitError,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isSuccess) "VERIFIED! REDIRECTING..." else "INVALID CODE! PLEASE ENTER CORRECT OTP",
                            color = if (isSuccess) Color(0xFF00FF88) else BitError,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resend Timer
                Row(
                    modifier = Modifier.clickable(enabled = countdown == 0) {
                        if (countdown == 0) {
                            viewModel.resendEmailOtp()
                        }
                    },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (countdown == 0) "Didn't get email? " else "Didn't receive code? ",
                        color = BitOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (countdown == 0) "Resend OTP Now" else "Resend in 0:${if (countdown < 10) "0$countdown" else countdown}",
                        color = BitSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Fixed Bottom Section: Verify OTP Button + Numeric Keypad (Always 100% visible)
            val coroutineScope = rememberCoroutineScope()
            var isArrowLaunching by remember { mutableStateOf(false) }
            val arrowOffsetDx by animateDpAsState(
                targetValue = if (isArrowLaunching) 50.dp else 0.dp,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "OtpArrowLaunch"
            )

            val verifyBtnInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val isVerifyPressed by verifyBtnInteraction.collectIsPressedAsState()
            val verifyScale by animateFloatAsState(
                targetValue = if (isVerifyPressed) 0.96f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "VerifyScale"
            )

            val activePlatinumGradient = Brush.horizontalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFE2E8F0),
                    Color(0xFFCBD5E1)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pinned Verify OTP Button directly above keypad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(verifyScale)
                        .clip(RoundedCornerShape(25.dp))
                        .then(
                            if (isSuccess) Modifier.background(Color(0xFF10B981))
                            else Modifier.background(activePlatinumGradient)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSuccess) Color(0xFF34D399) else Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .clickable(
                            interactionSource = verifyBtnInteraction,
                            indication = null
                        ) {
                            if (!isArrowLaunching && !isVerifyingOtp) {
                                coroutineScope.launch {
                                    isArrowLaunching = true
                                    delay(200)
                                    if (otpCode.length == 6) {
                                        viewModel.verifyFirebaseOtp(
                                            onSuccess = {
                                                isSuccess = true
                                                isError = false
                                                onVerifySuccess()
                                            },
                                            onError = {
                                                isError = true
                                                isSuccess = false
                                                isArrowLaunching = false
                                            }
                                        )
                                    } else {
                                        isError = true
                                        isArrowLaunching = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSuccess) "Verified Successfully" else "Verify OTP",
                            color = if (isSuccess) Color.White else Color(0xFF07080B),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isSuccess) Color.White else Color(0xFF07080B),
                            modifier = Modifier
                                .offset(x = arrowOffsetDx)
                                .size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numeric Keypad
                NothingNumpad(
                    onDigitClick = { digit ->
                        if (otpCode.length < 6) {
                            viewModel.appendOtpDigit(digit)
                        }
                    },
                    onBackspaceClick = {
                        viewModel.backspaceOtp()
                        isError = false
                    }
                )
            }
        }
    }
}
