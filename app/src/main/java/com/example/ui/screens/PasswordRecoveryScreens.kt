package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
fun PasswordRecoveryEmailScreen(
    viewModel: BitChatViewModel,
    onBackClick: () -> Unit,
    onOtpSent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email by viewModel.enteredEmail.collectAsState()
    val error by viewModel.passwordResetError.collectAsState()
    val isSending by viewModel.isSendingOtp.collectAsState()
    val isEmailValid = viewModel.isEmailValid(email)
    val focusManager = LocalFocusManager.current
    var isEmailFocused by remember { mutableStateOf(false) }

    PasswordRecoveryScaffold(
        modifier = modifier,
        title = "Reset your password",
        description = "Enter your email and we'll send a 6-digit code. For your privacy, we won't confirm whether an account exists.",
        onBackClick = onBackClick,
        footer = {
            RecoveryPrimaryButton(
                enabled = isEmailValid && !isSending,
                isLoading = isSending,
                idleLabel = "Send reset code",
                loadingLabel = "Sending code..."
            ) {
                focusManager.clearFocus()
                viewModel.requestPasswordResetOtp(onOtpSent)
            }
        }
    ) {
        RecoveryFieldLabel("EMAIL ADDRESS")
        Spacer(modifier = Modifier.height(8.dp))
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            borderColor = if (isEmailValid) Color(0xFF10B981) else if (isEmailFocused) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
            backgroundColor = Color(0x9916161A)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
                    if (email.isEmpty()) {
                        Text("name@example.com", color = Color.White.copy(alpha = 0.3f), fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = email,
                        onValueChange = viewModel::updateEmail,
                        singleLine = true,
                        textStyle = TextStyle(color = BitOnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        cursorBrush = SolidColor(BitSecondary),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { isEmailFocused = it.isFocused }
                    )
                }
                if (email.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateEmail("") }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                            contentDescription = "Clear email",
                            tint = BitOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        PasswordResetError(error)
    }
}

@Composable
fun SetNewPasswordScreen(
    viewModel: BitChatViewModel,
    onBackClick: () -> Unit,
    onPasswordReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val password by viewModel.enteredPassword.collectAsState()
    val confirmation by viewModel.enteredConfirmPassword.collectAsState()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsState()
    val isConfirmationVisible by viewModel.isConfirmPasswordVisible.collectAsState()
    val isUpdating by viewModel.isUpdatingPassword.collectAsState()
    val error by viewModel.passwordResetError.collectAsState()
    val isLengthValid = viewModel.isPasswordLengthValid(password)
    val isUpperValid = viewModel.isPasswordHasUpper(password)
    val isLowerValid = viewModel.isPasswordHasLower(password)
    val isNumberValid = viewModel.isPasswordHasNumber(password)
    val isMatchValid = viewModel.isPasswordMatching()
    val canSubmit = isLengthValid && isUpperValid && isLowerValid && isNumberValid && isMatchValid
    val focusManager = LocalFocusManager.current

    PasswordRecoveryScaffold(
        modifier = modifier,
        title = "Choose a new password",
        description = "Use at least 8 characters, including uppercase, lowercase, and a number.",
        onBackClick = onBackClick,
        footer = {
            RecoveryPrimaryButton(
                enabled = canSubmit && !isUpdating,
                isLoading = isUpdating,
                idleLabel = "Update password",
                loadingLabel = "Updating..."
            ) {
                focusManager.clearFocus()
                viewModel.resetPassword(onPasswordReset)
            }
        }
    ) {
        RecoveryFieldLabel("NEW PASSWORD")
        Spacer(modifier = Modifier.height(8.dp))
        PasswordInput(
            value = password,
            placeholder = "Create a new password",
            visible = isPasswordVisible,
            valid = isLengthValid && isUpperValid && isLowerValid && isNumberValid,
            onValueChange = viewModel::updatePassword,
            onToggleVisibility = viewModel::togglePasswordVisibility
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PasswordRequirementChip("8+ chars", isLengthValid, Modifier.weight(1f))
            PasswordRequirementChip("Upper", isUpperValid, Modifier.weight(1f))
            PasswordRequirementChip("Lower", isLowerValid, Modifier.weight(1f))
            PasswordRequirementChip("Number", isNumberValid, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))
        RecoveryFieldLabel("CONFIRM NEW PASSWORD")
        Spacer(modifier = Modifier.height(8.dp))
        PasswordInput(
            value = confirmation,
            placeholder = "Repeat your new password",
            visible = isConfirmationVisible,
            valid = isMatchValid,
            showInvalid = confirmation.isNotEmpty() && !isMatchValid,
            onValueChange = viewModel::updateConfirmPassword,
            onToggleVisibility = viewModel::toggleConfirmPasswordVisibility
        )
        PasswordResetError(error)
    }
}

@Composable
private fun PasswordRecoveryScaffold(
    title: String,
    description: String,
    onBackClick: () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BitBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Text(
                        text = "KnotLink",
                        color = BitPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BitSecondary))
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(title, color = BitOnSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(description, color = BitOnSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                content()
                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                footer()
            }
        }
    }
}

@Composable
private fun RecoveryFieldLabel(label: String) {
    Text(
        text = label,
        color = BitOnSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

@Composable
private fun PasswordInput(
    value: String,
    placeholder: String,
    visible: Boolean,
    valid: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    showInvalid: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        valid -> Color(0xFF10B981)
        showInvalid -> BitError
        isFocused -> Color.White.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = borderColor,
        backgroundColor = Color(0x9916161A)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Password",
                tint = if (valid) Color(0xFF10B981) else BitOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 15.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = TextStyle(color = BitOnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    cursorBrush = SolidColor(BitSecondary),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
                )
            }
            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = BitOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PasswordResetError(error: String?) {
    AnimatedVisibility(visible = error != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BitError.copy(alpha = 0.12f))
                .border(1.dp, BitError.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = "Error", tint = BitError, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(error.orEmpty(), color = BitError, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun RecoveryPrimaryButton(
    enabled: Boolean,
    isLoading: Boolean,
    idleLabel: String,
    loadingLabel: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "recoveryButtonScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color.White, Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                else Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF)))
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF07080B), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(loadingLabel, color = Color(0xFF07080B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    idleLabel,
                    color = if (enabled) Color(0xFF07080B) else Color.White.copy(alpha = 0.4f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF07080B) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
