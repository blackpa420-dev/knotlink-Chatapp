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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppFontFamily
import com.example.ui.viewmodel.BitChatViewModel

@Composable
fun CardDoodleBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft translucent blue ambient doodle circles
        drawCircle(
            color = Color(0xFF3B82F6).copy(alpha = 0.05f),
            radius = w * 0.35f,
            center = Offset(w * 0.88f, h * 0.15f)
        )
        drawCircle(
            color = Color(0xFF2563EB).copy(alpha = 0.06f),
            radius = w * 0.28f,
            center = Offset(w * 0.08f, h * 0.82f)
        )

        // Cute vector doodle cross-stitches (+)
        val strokeW = 2.dp.toPx()
        val crossColor = Color(0xFF3B82F6).copy(alpha = 0.14f)

        // Doodle Cross 1
        drawLine(
            color = crossColor,
            start = Offset(w * 0.82f, h * 0.08f),
            end = Offset(w * 0.88f, h * 0.08f),
            strokeWidth = strokeW
        )
        drawLine(
            color = crossColor,
            start = Offset(w * 0.85f, h * 0.05f),
            end = Offset(w * 0.85f, h * 0.11f),
            strokeWidth = strokeW
        )

        // Doodle Cross 2
        drawLine(
            color = crossColor,
            start = Offset(w * 0.12f, h * 0.38f),
            end = Offset(w * 0.18f, h * 0.38f),
            strokeWidth = strokeW
        )
        drawLine(
            color = crossColor,
            start = Offset(w * 0.15f, h * 0.35f),
            end = Offset(w * 0.15f, h * 0.41f),
            strokeWidth = strokeW
        )

        // Doodle Ring
        drawCircle(
            color = Color(0xFF60A5FA).copy(alpha = 0.12f),
            radius = 16.dp.toPx(),
            center = Offset(w * 0.86f, h * 0.65f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    RESET_PASSWORD
}

@Composable
fun EmailAuthScreen(
    bitChatViewModel: BitChatViewModel,
    initialMode: AuthMode = AuthMode.LOGIN,
    onNavigateBack: () -> Unit,
    onNavigateToOtp: (email: String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    // Step 1: Email, Step 2: Password
    var currentSubStep by remember { mutableIntStateOf(1) }

    val context = LocalContext.current

    val enteredEmail by bitChatViewModel.enteredEmail.collectAsState()
    val enteredPassword by bitChatViewModel.enteredPassword.collectAsState()
    val enteredConfirmPassword by bitChatViewModel.enteredConfirmPassword.collectAsState()
    val isSendingOtp by bitChatViewModel.isSendingOtp.collectAsState()
    val emailAuthError by bitChatViewModel.emailAuthError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showEmailInstantError by remember { mutableStateOf(false) }
    var showPasswordInstantError by remember { mutableStateOf(false) }

    val totalSteps = 4
    val currentStepNumber = when (mode) {
        AuthMode.LOGIN -> if (currentSubStep == 1) 1 else 2
        AuthMode.REGISTER -> if (currentSubStep == 1) 1 else 2
        AuthMode.FORGOT_PASSWORD -> 1
        AuthMode.RESET_PASSWORD -> 2
    }

    BackHandler {
        if (currentSubStep == 2) {
            currentSubStep = 1
        } else if (mode != initialMode) {
            mode = initialMode
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(emailAuthError) {
        emailAuthError?.let { err ->
            if (err.isNotBlank()) {
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badge_float_auth")
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
                        Color(0xFFDCEBFE), // Light soft pastel blue top
                        Color(0xFFEBF3FF),
                        Color(0xFFF1F5F9)  // Light grey bottom
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
                // Top Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (currentSubStep == 2) {
                                currentSubStep = 1
                            } else if (mode != initialMode) {
                                mode = initialMode
                            } else {
                                onNavigateBack()
                            }
                        },
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

                    // Step Counter Tag
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Step $currentStepNumber of $totalSteps",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar Segments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..totalSteps) {
                        val isCompletedOrActive = step <= currentStepNumber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompletedOrActive) Color(0xFF2563EB) else Color(0xFFCBD5E1).copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3D Floating Blue Icon Badge (Flat, No Shadows)
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
                        val iconVector = when {
                            currentSubStep == 1 -> Icons.Default.Mail
                            mode == AuthMode.FORGOT_PASSWORD -> Icons.Default.Lock
                            else -> Icons.Default.Key
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = "Badge Icon",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Page Title & Guidance Subtitle
                val pageTitle = when {
                    mode == AuthMode.FORGOT_PASSWORD -> "Forgot Password?"
                    mode == AuthMode.RESET_PASSWORD -> "Reset Password"
                    currentSubStep == 1 -> "Enter Email Address"
                    mode == AuthMode.LOGIN -> "Enter Password"
                    else -> "Set Password"
                }

                val pageSubtitle = when {
                    mode == AuthMode.FORGOT_PASSWORD -> "Enter your email to receive a password reset OTP"
                    mode == AuthMode.RESET_PASSWORD -> "Create a new strong password for your account"
                    currentSubStep == 1 -> "Step 1: Enter your email address to continue"
                    mode == AuthMode.LOGIN -> "Step 2: Enter your account password to log in"
                    else -> "Step 2: Set a strong password for your new account"
                }

                Text(
                    text = pageTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pageSubtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom UI Card Tile (NO Shadow, Flat Clean White-Blue Mix Background with Doodles & 32dp Curved Corners)
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
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentSubStep == 1 || mode == AuthMode.FORGOT_PASSWORD) {
                        // --- STEP 1: EMAIL ENTRY FORM ---
                        Text(
                            text = "Email Address",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, bottom = 6.dp)
                        )

                        OutlinedPillTextField(
                            value = enteredEmail,
                            onValueChange = { input ->
                                bitChatViewModel.updateEmail(input)
                                showEmailInstantError = input.isNotBlank() && !bitChatViewModel.isEmailValid(input)
                            },
                            placeholder = "e.g. alex.morgan@gmail.com",
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            isError = showEmailInstantError
                        )

                        // Instant Real-Time Error Feedback Badge
                        AnimatedVisibility(
                            visible = showEmailInstantError,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, start = 8.dp),
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
                                    text = "Please enter a valid email address (e.g. name@domain.com)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // CTA Button: Continue to Password / Send Reset Code
                        val btnText = if (mode == AuthMode.FORGOT_PASSWORD) "Send Reset Code" else "Continue to Password"

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
                                .clickable(enabled = !isSendingOtp) {
                                    if (enteredEmail.isBlank()) {
                                        showEmailInstantError = true
                                        Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
                                    } else if (!bitChatViewModel.isEmailValid(enteredEmail)) {
                                        showEmailInstantError = true
                                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showEmailInstantError = false
                                        if (mode == AuthMode.FORGOT_PASSWORD) {
                                            bitChatViewModel.sendLoginOtp {
                                                onNavigateToOtp(enteredEmail)
                                            }
                                        } else {
                                            currentSubStep = 2
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = btnText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // --- STEP 2: PASSWORD FORM (Login / Register / Reset) ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Email summary chip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Account Email",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = enteredEmail,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "Change",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.clickable { currentSubStep = 1 }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Password",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            OutlinedPillTextField(
                                value = enteredPassword,
                                onValueChange = { bitChatViewModel.updatePassword(it) },
                                placeholder = "Enter password",
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                imeAction = if (mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next
                            )

                            // Forgot Password Link in Login Mode
                            if (mode == AuthMode.LOGIN) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.clickable {
                                            if (enteredEmail.isBlank()) {
                                                Toast.makeText(context, "Please enter your email first", Toast.LENGTH_SHORT).show()
                                                currentSubStep = 1
                                            } else {
                                                bitChatViewModel.setForgotPasswordMode(true)
                                                bitChatViewModel.sendLoginOtp {
                                                    onNavigateToOtp(enteredEmail)
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Password Suggestion Button for Registration/Reset Mode
                            if (mode == AuthMode.REGISTER || mode == AuthMode.RESET_PASSWORD) {
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Password Suggestion Pill Button
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB).copy(alpha = 0.1f))
                                            .clickable {
                                                val suggested = bitChatViewModel.generateStrongPassword()
                                                passwordVisible = true
                                                confirmPasswordVisible = true
                                                Toast.makeText(context, "Generated Strong Password!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Suggest Password",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Suggest Strong Password",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                    }

                                    // Strength meter text
                                    val is8Plus = bitChatViewModel.isPasswordLengthValid(enteredPassword)
                                    val isUpper = bitChatViewModel.isPasswordHasUpper(enteredPassword)
                                    val isNum = bitChatViewModel.isPasswordHasNumber(enteredPassword)
                                    val validCount = listOf(is8Plus, isUpper, isNum).count { it }

                                    val strengthLabel = when (validCount) {
                                        0, 1 -> "Weak"
                                        2 -> "Medium"
                                        3 -> "Strong 💪"
                                        else -> ""
                                    }
                                    val strengthColor = when (validCount) {
                                        0, 1 -> Color(0xFFEF4444)
                                        2 -> Color(0xFFF59E0B)
                                        3 -> Color(0xFF10B981)
                                        else -> Color.Gray
                                    }

                                    if (enteredPassword.isNotBlank()) {
                                        Text(
                                            text = strengthLabel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = strengthColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Confirm Password
                                Text(
                                    text = "Confirm Password",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                                )

                                OutlinedPillTextField(
                                    value = enteredConfirmPassword,
                                    onValueChange = { input ->
                                        bitChatViewModel.updateConfirmPassword(input)
                                        showPasswordInstantError = enteredPassword.isNotBlank() && input.isNotBlank() && enteredPassword != input
                                    },
                                    placeholder = "Confirm password",
                                    isPassword = true,
                                    passwordVisible = confirmPasswordVisible,
                                    onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                    imeAction = ImeAction.Done,
                                    isError = showPasswordInstantError
                                )

                                // Instant Password Match Error
                                AnimatedVisibility(
                                    visible = showPasswordInstantError,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, start = 8.dp),
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
                                            text = "Passwords do not match!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Password Requirements Tags Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RequirementTag(
                                        label = "8+ chars",
                                        isMet = bitChatViewModel.isPasswordLengthValid(enteredPassword)
                                    )
                                    RequirementTag(
                                        label = "A-Z Upper",
                                        isMet = bitChatViewModel.isPasswordHasUpper(enteredPassword)
                                    )
                                    RequirementTag(
                                        label = "0-9 Number",
                                        isMet = bitChatViewModel.isPasswordHasNumber(enteredPassword)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Primary CTA Button
                            val buttonText = when (mode) {
                                AuthMode.LOGIN -> "Log In"
                                AuthMode.REGISTER -> "Send Verification Code"
                                AuthMode.RESET_PASSWORD -> "Reset & Log In"
                                AuthMode.FORGOT_PASSWORD -> "Continue"
                            }

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
                                    .clickable(enabled = !isSendingOtp) {
                                        when (mode) {
                                            AuthMode.LOGIN -> {
                                                if (enteredPassword.isBlank()) {
                                                    Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    bitChatViewModel.loginWithEmailAndPassword {
                                                        onAuthSuccess()
                                                    }
                                                }
                                            }
                                            AuthMode.REGISTER -> {
                                                if (enteredPassword.length < 6) {
                                                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                                } else if (enteredPassword != enteredConfirmPassword) {
                                                    showPasswordInstantError = true
                                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    bitChatViewModel.registerWithEmailAndSendOtp {
                                                        onNavigateToOtp(enteredEmail)
                                                    }
                                                }
                                            }
                                            AuthMode.RESET_PASSWORD -> {
                                                bitChatViewModel.resetPasswordWithValidation(
                                                    newPass = enteredPassword,
                                                    confirmPass = enteredConfirmPassword,
                                                    onSuccess = {
                                                        mode = AuthMode.LOGIN
                                                        currentSubStep = 1
                                                        Toast.makeText(context, "Password reset successfully! Please log in.", Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { err ->
                                                        showPasswordInstantError = true
                                                    }
                                                )
                                            }
                                            AuthMode.FORGOT_PASSWORD -> {
                                                bitChatViewModel.sendLoginOtp {
                                                    onNavigateToOtp(enteredEmail)
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        text = buttonText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer Switch Modes (with proper bottom clearance for navigation bar)
                    Column(
                        modifier = Modifier.padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (mode) {
                            AuthMode.LOGIN -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.clickable {
                                        bitChatViewModel.setLoginMode(false)
                                        mode = AuthMode.REGISTER
                                        currentSubStep = 1
                                    }
                                ) {
                                    Text(text = "Don't have an account? ", fontSize = 13.5.sp, color = Color(0xFF64748B))
                                    Text(text = "Sign Up", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                            }
                            AuthMode.REGISTER -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.clickable {
                                        bitChatViewModel.setLoginMode(true)
                                        mode = AuthMode.LOGIN
                                        currentSubStep = 1
                                    }
                                ) {
                                    Text(text = "Already have an account? ", fontSize = 13.5.sp, color = Color(0xFF64748B))
                                    Text(text = "Log In", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                            }
                            else -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.clickable {
                                        mode = AuthMode.LOGIN
                                        currentSubStep = 1
                                    }
                                ) {
                                    Text(text = "Know your password? ", fontSize = 13.5.sp, color = Color(0xFF64748B))
                                    Text(text = "Log In", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequirementTag(
    label: String,
    isMet: Boolean
) {
    val bgColor = if (isMet) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFE2E8F0)
    val textColor = if (isMet) Color(0xFF059669) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMet) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun OutlinedPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF94A3B8),
                fontSize = 14.5.sp
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction
        ),
        trailingIcon = {
            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        },
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedBorderColor = if (isError) Color(0xFFEF4444) else Color(0xFF2563EB),
            unfocusedBorderColor = if (isError) Color(0xFFEF4444) else Color(0xFFCBD5E1),
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF0F172A),
            cursorColor = Color(0xFF2563EB)
        )
    )
}
