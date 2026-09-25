package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassPanel
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitError
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.viewmodel.BitChatViewModel

@Composable
fun EmailAuthScreen(
    viewModel: BitChatViewModel,
    onBackClick: () -> Unit,
    onOtpSent: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onNextClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLoginMode by viewModel.isLoginMode.collectAsState()
    val enteredEmail by viewModel.enteredEmail.collectAsState()
    val enteredPassword by viewModel.enteredPassword.collectAsState()
    val enteredConfirmPassword by viewModel.enteredConfirmPassword.collectAsState()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsState()
    val isConfirmPasswordVisible by viewModel.isConfirmPasswordVisible.collectAsState()
    val isSendingOtp by viewModel.isSendingOtp.collectAsState()
    val emailAuthError by viewModel.emailAuthError.collectAsState()

    val isEmailValid = viewModel.isEmailValid(enteredEmail)
    val isLengthValid = viewModel.isPasswordLengthValid(enteredPassword)
    val isUpperValid = viewModel.isPasswordHasUpper(enteredPassword)
    val isLowerValid = viewModel.isPasswordHasLower(enteredPassword)
    val isNumberValid = viewModel.isPasswordHasNumber(enteredPassword)
    val isMatchValid = viewModel.isPasswordMatching()

    val canSubmit = if (isLoginMode) {
        isEmailValid && enteredPassword.isNotBlank()
    } else {
        isEmailValid && isLengthValid && isUpperValid && isLowerValid && isNumberValid && isMatchValid
    }

    val focusManager = LocalFocusManager.current

    val handleAuthSubmit: () -> Unit = {
        focusManager.clearFocus()
        if (isLoginMode) {
            viewModel.loginWithEmailAndPassword(onSuccess = {
                onLoginSuccess()
                onNextClick()
            })
        } else {
            viewModel.registerWithEmailAndSendOtp(onSuccess = {
                onOtpSent()
                onNextClick()
            })
        }
    }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BitBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .clickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BitOnSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KnotLink",
                                color = BitPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(BitSecondary)
                            )
                        }
                    }

                    // Mode Switcher Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x9916161A))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (!isLoginMode) Color.White.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable { viewModel.setLoginMode(false) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Register",
                                color = if (!isLoginMode) BitPrimary else BitOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (!isLoginMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isLoginMode) Color.White.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable { viewModel.setLoginMode(true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Log In",
                                color = if (isLoginMode) BitPrimary else BitOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isLoginMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Description
                Text(
                    text = if (isLoginMode) "Log In to KnotLink" else "Create Account",
                    color = BitOnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isLoginMode) {
                        "Enter your email & password to access your encrypted chats and contacts."
                    } else {
                        "Enter your email & set a password with at least 8 characters (including upper, lower & numbers)."
                    },
                    color = BitOnSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Email Address Field
                Text(
                    text = "EMAIL ADDRESS",
                    color = BitOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                var isEmailFocused by remember { mutableStateOf(false) }
                val emailBorderColor by animateColorAsState(
                    targetValue = when {
                        isEmailValid -> Color(0xFF10B981)
                        isEmailFocused -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.12f)
                    },
                    animationSpec = tween(200),
                    label = "emailBorder"
                )

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    borderColor = emailBorderColor,
                    backgroundColor = Color(0x9916161A)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Email",
                            tint = if (isEmailValid) Color(0xFF10B981) else BitOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (enteredEmail.isEmpty()) {
                                Text(
                                    text = "name@example.com",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = enteredEmail,
                                onValueChange = { viewModel.updateEmail(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = BitOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                cursorBrush = SolidColor(BitSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isEmailFocused = it.isFocused }
                            )
                        }

                        if (enteredEmail.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateEmail("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = BitOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Password Field
                Text(
                    text = "PASSWORD",
                    color = BitOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                var isPassFocused by remember { mutableStateOf(false) }
                val passBorderColor by animateColorAsState(
                    targetValue = when {
                        !isLoginMode && isLengthValid && isUpperValid && isLowerValid && isNumberValid -> Color(0xFF10B981)
                        isPassFocused -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.12f)
                    },
                    animationSpec = tween(200),
                    label = "passBorder"
                )

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    borderColor = passBorderColor,
                    backgroundColor = Color(0x9916161A)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password",
                            tint = if (!isLoginMode && isLengthValid && isUpperValid && isLowerValid && isNumberValid) Color(0xFF10B981) else BitOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (enteredPassword.isEmpty()) {
                                Text(
                                    text = if (isLoginMode) "Enter your password" else "Create a password",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = enteredPassword,
                                onValueChange = { viewModel.updatePassword(it) },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(
                                    color = BitOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (isLoginMode && canSubmit && !isSendingOtp) {
                                            handleAuthSubmit()
                                        }
                                    }
                                ),
                                cursorBrush = SolidColor(BitSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isPassFocused = it.isFocused }
                            )
                        }

                        IconButton(
                            onClick = { viewModel.togglePasswordVisibility() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = BitOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Password Requirements (Only for Create Account mode)
                if (!isLoginMode) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PasswordRequirementChip(
                            label = "8+ chars",
                            isMet = isLengthValid,
                            modifier = Modifier.weight(1f)
                        )
                        PasswordRequirementChip(
                            label = "Upper (A-Z)",
                            isMet = isUpperValid,
                            modifier = Modifier.weight(1f)
                        )
                        PasswordRequirementChip(
                            label = "Lower (a-z)",
                            isMet = isLowerValid,
                            modifier = Modifier.weight(1f)
                        )
                        PasswordRequirementChip(
                            label = "Number (0-9)",
                            isMet = isNumberValid,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. Confirm Password Field
                    Text(
                        text = "CONFIRM PASSWORD",
                        color = BitOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var isConfirmFocused by remember { mutableStateOf(false) }
                    val confirmBorderColor by animateColorAsState(
                        targetValue = when {
                            isMatchValid -> Color(0xFF10B981)
                            enteredConfirmPassword.isNotEmpty() && !isMatchValid -> BitError
                            isConfirmFocused -> Color.White.copy(alpha = 0.5f)
                            else -> Color.White.copy(alpha = 0.12f)
                        },
                        animationSpec = tween(200),
                        label = "confirmBorder"
                    )

                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        borderColor = confirmBorderColor,
                        backgroundColor = Color(0x9916161A)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Confirm Password",
                                tint = if (isMatchValid) Color(0xFF10B981) else BitOnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (enteredConfirmPassword.isEmpty()) {
                                    Text(
                                        text = "Confirm your password",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 15.sp
                                    )
                                }
                                BasicTextField(
                                    value = enteredConfirmPassword,
                                    onValueChange = { viewModel.updateConfirmPassword(it) },
                                    singleLine = true,
                                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    textStyle = TextStyle(
                                        color = BitOnSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (canSubmit) {
                                                focusManager.clearFocus()
                                                viewModel.validateEmailAndSendOtp(onNextClick)
                                            }
                                        }
                                    ),
                                    cursorBrush = SolidColor(BitSecondary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isConfirmFocused = it.isFocused }
                                    )
                            }

                            IconButton(
                                onClick = { viewModel.toggleConfirmPasswordVisibility() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isConfirmPasswordVisible) "Hide Password" else "Show Password",
                                    tint = BitOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Error Message Display
                AnimatedVisibility(
                    visible = emailAuthError != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BitError.copy(alpha = 0.12f))
                            .border(1.dp, BitError.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = BitError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = emailAuthError ?: "",
                                color = BitError,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Actions & Submit Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val buttonInteractionSource = remember { MutableInteractionSource() }
                val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonPressed && canSubmit) 0.97f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "btnScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (canSubmit) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0x33FFFFFF), Color(0x22FFFFFF))
                                )
                            }
                        )
                        .clickable(
                            enabled = canSubmit && !isSendingOtp,
                            interactionSource = buttonInteractionSource,
                            indication = ripple(color = Color.Black),
                            onClick = handleAuthSubmit
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSendingOtp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF07080B),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isLoginMode) "Logging in..." else "Sending Code...",
                                color = Color(0xFF07080B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isLoginMode) "Log In" else "Verify OTP",
                                color = if (canSubmit) Color(0xFF07080B) else Color.White.copy(alpha = 0.4f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (canSubmit) Color(0xFF07080B) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Switch Mode Helper Text
                Text(
                    text = buildAnnotatedString {
                        if (isLoginMode) {
                            append("Don't have an account? ")
                            withStyle(
                                SpanStyle(
                                    color = BitPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Create Account")
                            }
                        } else {
                            append("Already have an account? ")
                            withStyle(
                                SpanStyle(
                                    color = BitPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Log In")
                            }
                        }
                    },
                    color = BitOnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.setLoginMode(!isLoginMode)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PasswordRequirementChip(
    label: String,
    isMet: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isMet) Color(0xFF10B981).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
        animationSpec = tween(200),
        label = "chipBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isMet) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0x6616161A),
        animationSpec = tween(200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isMet) Color(0xFF10B981) else BitOnSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "chipText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isMet) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
