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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassPanel
import com.example.ui.components.NothingNumpad
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitOnPrimary
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.viewmodel.BitChatViewModel

@Composable
fun NumberVerificationScreen(
    viewModel: BitChatViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phoneNumber by viewModel.enteredPhoneNumber.collectAsState()
    val isSendingOtp by viewModel.isSendingOtp.collectAsState()
    val phoneCheckError by viewModel.phoneCheckError.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Format phone number for display (e.g. 017 1234 5678)
    val formattedDisplay = buildString {
        phoneNumber.forEachIndexed { index, c ->
            if (index == 3 || index == 7) append(' ')
            append(c)
        }
    }

    // Cursor blink animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BitBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header & Inputs Section
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BitOnSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "KnotLink",
                        color = BitPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your Number",
                    color = BitOnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "KnotLink is currently available worldwide. Please enter your phone number to continue.",
                    color = BitOnSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Country Picker
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    backgroundColor = Color(0x9916161A)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp, 20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Flag",
                                    tint = BitSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Bangladesh",
                                color = BitOnSurface,
                                fontSize = 16.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Exclusive",
                            tint = BitOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Input Field Box with animated active glow & scale
                val isPhoneActive = phoneNumber.isNotEmpty()
                val phoneBoxScale by animateFloatAsState(
                    targetValue = if (isPhoneActive) 1.02f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "PhoneBoxScale"
                )

                val phoneBorderColor by animateColorAsState(
                    targetValue = when {
                        phoneNumber.length == 11 && phoneNumber.startsWith("01") -> Color(0xFF10B981)
                        phoneNumber.isNotEmpty() -> Color.White.copy(alpha = 0.35f)
                        else -> Color.White.copy(alpha = 0.12f)
                    },
                    animationSpec = tween(200),
                    label = "PhoneBorderColor"
                )

                val phoneBgColor by animateColorAsState(
                    targetValue = when {
                        phoneNumber.length == 11 && phoneNumber.startsWith("01") -> Color(0xFF10B981).copy(alpha = 0.12f)
                        phoneNumber.isNotEmpty() -> Color.White.copy(alpha = 0.06f)
                        else -> Color(0x9916161A)
                    },
                    animationSpec = tween(200),
                    label = "PhoneBgColor"
                )

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(phoneBoxScale),
                    cornerRadius = 18.dp,
                    borderColor = phoneBorderColor,
                    backgroundColor = phoneBgColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (phoneNumber.isEmpty()) {
                                Text(
                                    text = "01xxxxxxxxx",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formattedDisplay,
                                    color = BitOnSurface,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                // Blinking cursor
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(22.dp)
                                        .background(BitSecondary.copy(alpha = cursorAlpha), RoundedCornerShape(1.dp))
                                )
                            }
                        }

                        // Sleek animated checkmark badge when 11-digit valid number entered
                        AnimatedVisibility(
                            visible = phoneNumber.length == 11 && phoneNumber.startsWith("01"),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .border(width = 1.5.dp, color = Color(0xFF34D399), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Valid Number",
                                    tint = Color(0xFF07080B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Error message text when phone number is entered but incomplete or invalid
                AnimatedVisibility(
                    visible = (phoneNumber.isNotEmpty() && (phoneNumber.length < 11 || !phoneNumber.startsWith("01"))) || phoneCheckError != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp, start = 6.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = phoneCheckError ?: "Please enter a valid 11-digit Bangladeshi number (01xxxxxxxxx)",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // OTP Cue message & Hexagon Next Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // OTP Cue message
                    AnimatedVisibility(
                        visible = phoneNumber.length == 11 && phoneNumber.startsWith("01") && phoneCheckError == null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(BitSecondary, CircleShape)
                            )
                            Text(
                                text = "READY TO SEND OTP",
                                color = BitSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    if (phoneNumber.length < 11 || phoneCheckError != null) {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Modern Curved Rounded Hexagon Continue Button with Cyber Neon Gradient
                    val coroutineScope = rememberCoroutineScope()
                    var isArrowLaunching by remember { mutableStateOf(false) }
                    val arrowOffsetDx by animateDpAsState(
                        targetValue = if (isArrowLaunching) 48.dp else 0.dp,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "PhoneArrowLaunch"
                    )

                    val buttonInteractionSource = remember { MutableInteractionSource() }
                    val isBtnPressed by buttonInteractionSource.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isBtnPressed) 0.92f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ContinueBtnScale"
                    )

                    val activePlatinumGradient = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFE2E8F0),
                            Color(0xFFCBD5E1)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(buttonScale)
                            .clip(RoundedHexagonShape)
                            .then(
                                if (phoneNumber.length == 11 && phoneNumber.startsWith("01")) {
                                    Modifier.background(activePlatinumGradient)
                                } else {
                                    Modifier.background(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(
                                width = if (phoneNumber.length == 11 && phoneNumber.startsWith("01")) 1.5.dp else 1.dp,
                                color = if (phoneNumber.length == 11 && phoneNumber.startsWith("01")) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedHexagonShape
                            )
                            .clickable(
                                interactionSource = buttonInteractionSource,
                                indication = null
                            ) {
                                if (phoneNumber.length == 11 && phoneNumber.startsWith("01") && !isArrowLaunching && !isSendingOtp) {
                                    coroutineScope.launch {
                                        isArrowLaunching = true
                                        viewModel.validatePhoneAndSendOtp(activity) {
                                            onNextClick()
                                        }
                                        isArrowLaunching = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = if (phoneNumber.length == 11 && phoneNumber.startsWith("01")) Color(0xFF07080B) else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .offset(x = arrowOffsetDx)
                                .size(24.dp)
                        )
                    }
                }
            }

            // Bottom Section: Larger Numeric Keypad with 48dp bottom padding (20dp extra space)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
            ) {
                // Numeric Keypad
                NothingNumpad(
                    onDigitClick = { digit -> viewModel.appendPhoneDigit(digit) },
                    onBackspaceClick = { viewModel.backspacePhone() }
                )
            }
        }
    }
}

// Curved Rounded Hexagon Shape with smooth corner arcs
private val RoundedHexagonShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = w * 0.18f

    moveTo(w * 0.25f + r, 0f)
    lineTo(w * 0.75f - r, 0f)
    quadraticTo(w * 0.75f, 0f, w * 0.75f + r * 0.5f, r * 0.866f)

    lineTo(w - r * 0.5f, h * 0.5f - r * 0.866f)
    quadraticTo(w, h * 0.5f, w - r * 0.5f, h * 0.5f + r * 0.866f)

    lineTo(w * 0.75f + r * 0.5f, h - r * 0.866f)
    quadraticTo(w * 0.75f, h, w * 0.75f - r, h)

    lineTo(w * 0.25f + r, h)
    quadraticTo(w * 0.25f, h, w * 0.25f - r * 0.5f, h - r * 0.866f)

    lineTo(r * 0.5f, h * 0.5f + r * 0.866f)
    quadraticTo(0f, h * 0.5f, r * 0.5f, h * 0.5f - r * 0.866f)

    lineTo(w * 0.25f - r * 0.5f, r * 0.866f)
    quadraticTo(w * 0.25f, 0f, w * 0.25f + r, 0f)
    close()
}
