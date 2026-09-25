package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricStatus
import com.example.security.LockTimeout
import com.example.ui.components.GlassPanel
import com.example.ui.viewmodel.BitChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintLockScreen(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val appUnlockEnabled by viewModel.biometricAppUnlockEnabled.collectAsState()
    val chatLockEnabled by viewModel.biometricChatLockEnabled.collectAsState()
    val advancedSettingsEnabled by viewModel.biometricAdvancedSettingsEnabled.collectAsState()
    val currentTimeout by viewModel.lockTimeout.collectAsState()
    val lockedChatIds by viewModel.lockedChatIds.collectAsState()
    val biometricStatus by viewModel.biometricStatus.collectAsState()
    val allChats by viewModel.filteredChats.collectAsState()

    var showManageLockedChatsSheet by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    val bgColor = if (isNightMode) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isNightMode) Color(0xFF18181B) else Color(0xFFE2E8F0))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Fingerprint Lock",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Biometric Security & Privacy",
                        fontSize = 12.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Biometric Availability Status Card
            val statusColor = when (biometricStatus) {
                BiometricStatus.AVAILABLE -> Color(0xFF10B981)
                BiometricStatus.NOT_ENROLLED -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                backgroundColor = if (isNightMode) Color(0xFF18181B).copy(alpha = 0.8f) else Color.White,
                borderColor = statusColor.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (biometricStatus) {
                                BiometricStatus.AVAILABLE -> Icons.Default.Fingerprint
                                BiometricStatus.NOT_ENROLLED -> Icons.Default.Warning
                                else -> Icons.Default.Security
                            },
                            contentDescription = "Status Icon",
                            tint = statusColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hardware Status",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                        Text(
                            text = biometricStatus.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        val statusDesc = when (biometricStatus) {
                            BiometricStatus.AVAILABLE -> "Fingerprint & PIN security ready"
                            BiometricStatus.NOT_ENROLLED -> "Add fingerprint in Android settings"
                            else -> "Hardware unavailable on this device"
                        }
                        Text(
                            text = statusDesc,
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SECURITY CONTROLS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. App Unlock Protection
            BiometricControlCard(
                icon = Icons.Default.Shield,
                title = "App Unlock Protection",
                subtitle = "Require fingerprint or PIN authentication whenever you open KnotLink.",
                isChecked = appUnlockEnabled,
                isNightMode = isNightMode,
                textColor = textColor,
                subTextColor = subTextColor,
                onCheckedChange = { targetState ->
                    if (activity == null) {
                        viewModel.setBiometricAppUnlockEnabled(targetState)
                        return@BiometricControlCard
                    }

                    if (biometricStatus != BiometricStatus.AVAILABLE) {
                        Toast.makeText(context, biometricStatus.label, Toast.LENGTH_SHORT).show()
                        return@BiometricControlCard
                    }

                    val titleText = if (targetState) "Enable App Unlock" else "Disable App Unlock"
                    viewModel.authenticateWithBiometric(
                        activity = activity,
                        title = titleText,
                        subtitle = "Authenticate to confirm changes",
                        onSuccess = {
                            viewModel.setBiometricAppUnlockEnabled(targetState)
                            Toast.makeText(
                                context,
                                if (targetState) "App Unlock Protection Enabled" else "App Unlock Disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Chat Lock Protection
            BiometricControlCard(
                icon = Icons.Default.Lock,
                title = "Chat Lock Protection",
                subtitle = "Lock private chats individually. Fingerprint required to view protected messages.",
                isChecked = chatLockEnabled,
                isNightMode = isNightMode,
                textColor = textColor,
                subTextColor = subTextColor,
                onCheckedChange = { targetState ->
                    if (activity == null) {
                        viewModel.setBiometricChatLockEnabled(targetState)
                        return@BiometricControlCard
                    }

                    if (biometricStatus != BiometricStatus.AVAILABLE) {
                        Toast.makeText(context, biometricStatus.label, Toast.LENGTH_SHORT).show()
                        return@BiometricControlCard
                    }

                    viewModel.authenticateWithBiometric(
                        activity = activity,
                        title = if (targetState) "Enable Chat Lock" else "Disable Chat Lock",
                        subtitle = "Authenticate to update Chat Lock status",
                        onSuccess = {
                            viewModel.setBiometricChatLockEnabled(targetState)
                            Toast.makeText(
                                context,
                                if (targetState) "Chat Lock Protection Enabled" else "Chat Lock Disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            ) {
                // Manage Locked Chats Button
                AnimatedVisibility(visible = chatLockEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNightMode) Color(0xFF27272A) else Color(0xFFF1F5F9))
                                .clickable { showManageLockedChatsSheet = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Manage",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Manage Locked Chats (${lockedChatIds.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Manage",
                                tint = subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Advanced Settings Protection
            BiometricControlCard(
                icon = Icons.Default.Security,
                title = "Advanced Settings Protection",
                subtitle = "Require fingerprint authentication before entering sensitive settings (Account, Privacy, Security, Export).",
                isChecked = advancedSettingsEnabled,
                isNightMode = isNightMode,
                textColor = textColor,
                subTextColor = subTextColor,
                onCheckedChange = { targetState ->
                    if (activity == null) {
                        viewModel.setBiometricAdvancedSettingsEnabled(targetState)
                        return@BiometricControlCard
                    }

                    if (biometricStatus != BiometricStatus.AVAILABLE) {
                        Toast.makeText(context, biometricStatus.label, Toast.LENGTH_SHORT).show()
                        return@BiometricControlCard
                    }

                    viewModel.authenticateWithBiometric(
                        activity = activity,
                        title = if (targetState) "Enable Advanced Settings Protection" else "Disable Settings Protection",
                        subtitle = "Authenticate to confirm settings security",
                        onSuccess = {
                            viewModel.setBiometricAdvancedSettingsEnabled(targetState)
                            Toast.makeText(
                                context,
                                if (targetState) "Advanced Settings Protection Enabled" else "Disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Lock Timeout Option
            Text(
                text = "LOCK TIMEOUT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                backgroundColor = if (isNightMode) Color(0xFF18181B).copy(alpha = 0.8f) else Color.White,
                borderColor = if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timeout",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Require fingerprint after",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(
                        LockTimeout.IMMEDIATELY,
                        LockTimeout.ONE_MINUTE,
                        LockTimeout.FIVE_MINUTES,
                        LockTimeout.FIFTEEN_MINUTES
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { opt ->
                            val isSelected = (currentTimeout == opt.displayName)
                            val chipBg = if (isSelected) {
                                Color(0xFF2563EB).copy(alpha = 0.15f)
                            } else {
                                if (isNightMode) Color(0xFF27272A) else Color(0xFFF1F5F9)
                            }
                            val chipBorder = if (isSelected) Color(0xFF2563EB) else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .clickable { viewModel.setLockTimeout(opt.displayName) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF2563EB) else textColor
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Guarantee Info Box
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                backgroundColor = if (isNightMode) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFEFF6FF),
                borderColor = Color(0xFF3B82F6).copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zero-Knowledge Biometric Guarantee",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Biometric credentials are authenticated directly by Android's Secure Enclave hardware. KnotLink never sees, accesses, or stores raw fingerprint data.",
                            fontSize = 11.sp,
                            color = subTextColor,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Sheet to Manage Locked Chats
        if (showManageLockedChatsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showManageLockedChatsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = if (isNightMode) Color(0xFF18181B) else Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Select Chats to Lock",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Checked chats will require fingerprint authentication before opening.",
                        fontSize = 12.sp,
                        color = subTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (allChats.isEmpty()) {
                        Text(
                            text = "No active chats available.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allChats) { chat ->
                                val isLocked = lockedChatIds.contains(chat.id)
                                val toggleLockAction = {
                                    if (isLocked) {
                                        if (activity != null) {
                                            viewModel.authenticateWithBiometric(
                                                activity = activity,
                                                title = "Unlock Chat Protection",
                                                subtitle = "Scan fingerprint to unlock ${chat.name}",
                                                onSuccess = {
                                                    viewModel.toggleChatLock(chat.id)
                                                    Toast.makeText(context, "${chat.name} unlocked", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                            )
                                        } else {
                                            viewModel.toggleChatLock(chat.id)
                                        }
                                    } else {
                                        viewModel.toggleChatLock(chat.id)
                                        Toast.makeText(context, "${chat.name} locked with fingerprint", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isNightMode) Color(0xFF27272A) else Color(0xFFF8FAFC))
                                        .clickable { toggleLockAction() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Lock",
                                            tint = if (isLocked) Color(0xFFEF4444) else subTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = chat.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                            Text(
                                                text = chat.category,
                                                fontSize = 11.sp,
                                                color = subTextColor
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isLocked,
                                        onCheckedChange = { toggleLockAction() },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF2563EB)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showManageLockedChatsSheet = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricControlCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    isNightMode: Boolean,
    textColor: Color,
    subTextColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    extraContent: @Composable (() -> Unit)? = null
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        backgroundColor = if (isNightMode) Color(0xFF18181B).copy(alpha = 0.8f) else Color.White,
        borderColor = if (isChecked) Color(0xFF2563EB).copy(alpha = 0.4f) else if (isNightMode) Color(0xFF27272A) else Color.Black.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isChecked) Color(0xFF2563EB).copy(alpha = 0.15f) else (if (isNightMode) Color(0xFF27272A) else Color(0xFFF1F5F9))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isChecked) Color(0xFF2563EB) else subTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2563EB),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = if (isNightMode) Color(0xFF27272A) else Color(0xFFE2E8F0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = subTextColor,
                lineHeight = 16.sp
            )

            extraContent?.invoke()
        }
    }
}
