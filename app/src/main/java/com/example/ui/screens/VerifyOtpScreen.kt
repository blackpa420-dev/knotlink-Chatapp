package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.NothingNumpad
import com.example.ui.theme.AppFontFamily
import com.example.ui.viewmodel.BitChatViewModel
import kotlinx.coroutines.delay

@Composable
fun VerifyOtpScreen(
    bitChatViewModel: BitChatViewModel,
    targetEmailOrNumber: String,
    onNavigateBack: () -> Unit,
    onOtpVerifiedSuccess: () -> Unit
) {
    var otpDigits by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var resendCountdown by remember { mutableIntStateOf(30) }
    var isVerifying by remember { mutableStateOf(false) }
    var showOtpError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val emailAuthError by bitChatViewModel.emailAuthError.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }

    LaunchedEffect(emailAuthError) {
        emailAuthError?.let { err ->
            if (err.isNotBlank()) {
                isVerifying = false
                showOtpError = true
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badge_float_otp")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDCEBFE), // Soft pastel blue top
                        Color(0xFFEBF3FF),
                        Color(0xFFF1F5F9)  // White bottom
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Step Indicator Bar & Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.appicon),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KnotLink",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFontFamily,
                            color = Color(0xFF1E293B)
                        )
                    }

                    // Step Counter Tag (Step 3 of 4)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Step 3 of 4",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar Segments (75% completed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step <= 3) Color(0xFF2563EB) else Color(0xFFCBD5E1).copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3D Floating Blue Key Badge (Flat, No Shadows)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .offset(y = floatOffsetY.dp)
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFF2563EB),
                                        Color(0xFF1D4ED8)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Key Icon",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Verify Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter the 6-digit OTP code sent to your email",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                if (targetEmailOrNumber.isNotBlank()) {
                    Text(
                        text = targetEmailOrNumber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom UI Card Tile (NO Shadow, Flat Clean White-Blue Mix Background with Doodles)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF8FAFC),
                                Color(0xFFEFF6FF)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFDBEAFE)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                // Vector Doodles Pattern Layer inside Card
                CardDoodleBackground(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 6 Side-by-Side Circular Digit Input Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        otpDigits.forEachIndexed { index, digit ->
                            val isSelected = activeIndex == index
                            val hasValue = digit.isNotBlank()

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            hasValue -> Color.White
                                            isSelected -> Color(0xFFEFF6FF)
                                            else -> Color(0xFFF8FAFC)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected || hasValue) 1.5.dp else 1.dp,
                                        color = when {
                                            showOtpError -> Color(0xFFEF4444)
                                            isSelected -> Color(0xFF2563EB)
                                            hasValue -> Color(0xFF3B82F6)
                                            else -> Color(0xFFCBD5E1)
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { activeIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showOtpError) Color(0xFFEF4444) else Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    // Instant Error Badge
                    AnimatedVisibility(
                        visible = showOtpError,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Please enter all 6 digits correctly",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Vibrant Blue Capsule CTA "Verify & Continue"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFF2563EB)
                                    )
                                )
                            )
                            .clickable(enabled = !isVerifying) {
                                val code = otpDigits.joinToString("")
                                if (code.length < 6) {
                                    showOtpError = true
                                    Toast.makeText(context, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
                                } else {
                                    showOtpError = false
                                    isVerifying = true
                                    bitChatViewModel.setEntireOtp(code)
                                    bitChatViewModel.verifyOtp(
                                        onSuccess = {
                                            isVerifying = false
                                            onOtpVerifiedSuccess()
                                        },
                                        onError = {
                                            isVerifying = false
                                            showOtpError = true
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Verify & Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resend Link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable(enabled = resendCountdown == 0) {
                            if (resendCountdown == 0) {
                                resendCountdown = 30
                                bitChatViewModel.resendEmailOtp()
                            }
                        }
                    ) {
                        Text(
                            text = "Didn't receive any code? ",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = if (resendCountdown > 0) "Resend in ${resendCountdown}s" else "Resend Code",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (resendCountdown > 0) Color(0xFF94A3B8) else Color(0xFF2563EB)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Soft Day Mode Numpad
                    NothingNumpad(
                        onDigitClick = { digit ->
                            showOtpError = false
                            if (activeIndex < 6) {
                                val updated = otpDigits.toMutableList()
                                updated[activeIndex] = digit
                                otpDigits = updated
                                if (activeIndex < 5) activeIndex++
                            }
                        },
                        onBackspaceClick = {
                            showOtpError = false
                            if (otpDigits[activeIndex].isNotBlank()) {
                                val updated = otpDigits.toMutableList()
                                updated[activeIndex] = ""
                                otpDigits = updated
                            } else if (activeIndex > 0) {
                                activeIndex--
                                val updated = otpDigits.toMutableList()
                                updated[activeIndex] = ""
                                otpDigits = updated
                            }
                        },
                        isDarkTheme = false
                    )
                }
            }
        }
    }
}
