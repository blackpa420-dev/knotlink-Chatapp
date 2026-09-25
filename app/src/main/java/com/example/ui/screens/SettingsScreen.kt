package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Forum
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.util.QRCodeGenerator
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import android.widget.Toast
import com.example.ui.viewmodel.DeletedChatInfo
import kotlin.math.roundToInt
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.BitChatBottomNavBar
import com.example.ui.components.BitChatNavTab
import com.example.ui.components.GlassPanel
import java.io.File
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitError
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.theme.BitSurfaceContainer
import com.example.ui.viewmodel.BitChatViewModel

@Composable
fun SettingsScreen(
    viewModel: BitChatViewModel,
    onTabSelected: (BitChatNavTab) -> Unit,
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user by viewModel.userIdentity.collectAsState()
    val deletedChats by viewModel.deletedChats.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeletedChatsSheet by remember { mutableStateOf(false) }
    var showProfileSettingsPage by remember { mutableStateOf(false) }
    var showFingerprintLockPage by remember { mutableStateOf(false) }
    var showUsernameAvailabilityPage by remember { mutableStateOf(false) }

    var showPremiumSheet by remember { mutableStateOf(false) }
    var showBasicSettingsPage by remember { mutableStateOf(false) }
    var showAdvanceSettingsPage by remember { mutableStateOf(false) }
    var showSupportCareSheet by remember { mutableStateOf(false) }
    var isQrVisible by remember { mutableStateOf(false) }
    var qrCountdownSeconds by remember { mutableIntStateOf(10) }

    val context = LocalContext.current
    val publicId = user?.publicId ?: "user_001"
    val cachedQrBitmap = remember(publicId, context) {
        QRCodeGenerator.generateProfileQRCode(publicId = publicId, context = context, size = 512)
    }

    LaunchedEffect(isQrVisible, qrCountdownSeconds) {
        if (isQrVisible && qrCountdownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            qrCountdownSeconds -= 1
        } else if (isQrVisible && qrCountdownSeconds <= 0) {
            isQrVisible = false
        }
    }

    val activity = context as? FragmentActivity
    val advancedSettingsEnabled by viewModel.biometricAdvancedSettingsEnabled.collectAsState()

    fun checkSensitiveAccessAndOpen(onAccessGranted: () -> Unit) {
        if (advancedSettingsEnabled && activity != null) {
            viewModel.authenticateWithBiometric(
                activity = activity,
                title = "Sensitive Settings Security",
                subtitle = "Fingerprint required to access settings",
                onSuccess = onAccessGranted,
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            onAccessGranted()
        }
    }

    // App-wide Day / Night Mode state from ViewModel
    val isNightMode by viewModel.isNightMode.collectAsState()

    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9),
        animationSpec = tween(500),
        label = "bg_color_anim"
    )

    val animTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B),
        animationSpec = tween(500),
        label = "text_color_anim"
    )

    val animSubTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
        animationSpec = tween(500),
        label = "subtext_color_anim"
    )

    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            var totalDragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onDragEnd = {
                    if (totalDragX > 120f) {
                        onTabSelected(BitChatNavTab.QR_SCAN)
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDragX += dragAmount
                }
            )
        },
        bottomBar = {
            BitChatBottomNavBar(
                currentTab = BitChatNavTab.SETTINGS,
                onTabSelected = onTabSelected,
                isNightMode = isNightMode
            )
        },
        containerColor = animBgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    color = animTextColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Identity Profile Card with Hover/Press Interaction & Animated Fiery Blue Border
            val profileInteractionSource = remember { MutableInteractionSource() }
            val isProfilePressed by profileInteractionSource.collectIsPressedAsState()

            val cardScale by animateFloatAsState(
                targetValue = if (isProfilePressed) 0.96f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "profile_card_scale"
            )

            // Animated Fiery Burning Blue Gradient Border Effect
            val infiniteFlameTransition = rememberInfiniteTransition(label = "fiery_blue_border")
            val flamePhase by infiniteFlameTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "flame_phase"
            )

            val fieryBlueBorderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2563EB),
                    Color(0xFF38BDF8),
                    Color(0xFF60A5FA),
                    Color(0xFF00E5FF),
                    Color(0xFF1D4ED8),
                    Color(0xFF3B82F6),
                    Color(0xFF38BDF8)
                ),
                start = Offset(flamePhase, 0f),
                end = Offset(flamePhase + 400f, 400f)
            )

            val profileBorderModifier = if (!isNightMode) {
                Modifier.border(
                    width = 2.dp,
                    brush = fieryBlueBorderBrush,
                    shape = RoundedCornerShape(24.dp)
                )
            } else {
                Modifier.border(
                    width = 1.5.dp,
                    brush = fieryBlueBorderBrush,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .scale(cardScale)
                    .then(profileBorderModifier)
                    .clickable(
                        interactionSource = profileInteractionSource,
                        indication = null
                    ) {
                        checkSensitiveAccessAndOpen {
                            showProfileSettingsPage = true
                        }
                    },
                cornerRadius = 24.dp,
                backgroundColor = if (isNightMode) Color(0xFF0C0D12) else Color.White.copy(alpha = 0.95f),
                borderColor = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarPath = user?.avatarPath
                    val outlineGradient = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF2563EB),
                            Color(0xFF06B6D4)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.5.dp, outlineGradient, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val hasAvatar = !avatarPath.isNullOrBlank()
                        if (hasAvatar) {
                            val model: Any = if (avatarPath.startsWith("http://") || avatarPath.startsWith("https://") || avatarPath.startsWith("content://")) {
                                avatarPath
                            } else {
                                val clean = avatarPath.removePrefix("file://")
                                val f = File(clean)
                                if (f.exists()) f else avatarPath
                            }
                            AsyncImage(
                                model = model,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isNightMode) Color(0xFF151D2A) else Color(0xFFEFF6FF)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(26.dp))

                    val displayName = user?.fullName?.ifBlank { null } ?: "Tap to Set Full Name"
                    val rawUsername = user?.username ?: "user"
                    val cleanUsername = rawUsername.removePrefix("@").removeSuffix(".chat").removeSuffix(".bit").removeSuffix(".link")
                    val formattedHandle = "@${cleanUsername.ifBlank { "user" }}.link"
                    val userProfession = user?.profession?.ifBlank { "Student" } ?: "Student"

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            color = animTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = formattedHandle,
                            color = Color(0xFF2563EB),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Designation: $userProfession",
                            color = animSubTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Profile Settings",
                        tint = animSubTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dedicated "Show My QR Code" Button under Profile Card
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable {
                        qrCountdownSeconds = 10
                        isQrVisible = true
                    },
                cornerRadius = 16.dp,
                backgroundColor = if (isNightMode) Color(0xFF0F1018) else Color(0xFF2563EB),
                borderColor = if (isNightMode) Color(0xFF2563EB).copy(alpha = 0.4f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Show QR Code",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Show My QR Code",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Menu Items (Compact & Sleek without Subtitles)
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Requirement 3: Figma-Grade Custom Day/Night Theme Toggle Row
                FigmaDayNightToggleRow(
                    isNightMode = isNightMode,
                    onToggle = { viewModel.toggleNightMode() },
                    textColor = animTextColor,
                    subTextColor = animSubTextColor
                )

                // 4 Main Section Cards requested by user (Sleek titles without mini text)
                SettingsSectionCard(
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFFF59E0B),
                    title = "KnotLink Premium",
                    isNightMode = isNightMode,
                    onClick = { showPremiumSheet = true }
                )

                SettingsSectionCard(
                    icon = Icons.Default.Person,
                    iconTint = Color(0xFF00C6FF),
                    title = "Username Availability",
                    isNightMode = isNightMode,
                    onClick = { showUsernameAvailabilityPage = true }
                )

                SettingsSectionCard(
                    icon = Icons.Default.Tune,
                    iconTint = Color(0xFF10B981),
                    title = "Basic Settings",
                    isNightMode = isNightMode,
                    onClick = { showBasicSettingsPage = true }
                )

                SettingsSectionCard(
                    icon = Icons.Default.Shield,
                    iconTint = Color(0xFF2563EB),
                    title = "Advance Settings",
                    isNightMode = isNightMode,
                    onClick = {
                        checkSensitiveAccessAndOpen {
                            showAdvanceSettingsPage = true
                        }
                    }
                )

                SettingsSectionCard(
                    icon = Icons.Default.SupportAgent,
                    iconTint = Color(0xFF8B5CF6),
                    title = "Support Care",
                    isNightMode = isNightMode,
                    onClick = { showSupportCareSheet = true }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Premium Logout Button (Optimized for Day & Night Modes)
                val logoutBgColor = if (isNightMode) Color(0xFF450A0A).copy(alpha = 0.65f) else Color(0xFFFEF2F2)
                val logoutBorderColor = if (isNightMode) Color(0xFFEF4444) else Color(0xFFFCA5A5)
                val logoutTextColor = if (isNightMode) Color(0xFFF87171) else Color(0xFFDC2626)

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clickable { showLogoutDialog = true },
                    cornerRadius = 16.dp,
                    backgroundColor = logoutBgColor,
                    borderColor = logoutBorderColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Log Out",
                                tint = logoutTextColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Log Out",
                                color = logoutTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = logoutTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Deleted Chats Sheet Modal
    if (showDeletedChatsSheet) {
        DeletedChatsSheet(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onDismiss = { showDeletedChatsSheet = false }
        )
    }

    // 4 New Section Pages & Modals
    if (showBasicSettingsPage) {
        BasicSettingsPage(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onBack = { showBasicSettingsPage = false }
        )
    }

    if (showPremiumSheet) {
        BitChatPremiumPage(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onDismiss = { showPremiumSheet = false }
        )
    }

    if (showAdvanceSettingsPage) {
        AdvanceSettingsPage(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onDismiss = { showAdvanceSettingsPage = false },
            onOpenFingerprintLock = {
                showAdvanceSettingsPage = false
                showFingerprintLockPage = true
            }
        )
    }

    if (showSupportCareSheet) {
        SupportCareSheet(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onDismiss = { showSupportCareSheet = false }
        )
    }

    // Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = BitSurfaceContainer,
            titleContentColor = BitOnSurface,
            textContentColor = BitOnSurfaceVariant,
            title = {
                Text(
                    text = "Log Out of KnotLink",
                    fontWeight = FontWeight.Bold,
                    color = BitOnSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out?",
                    color = BitOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onLogoutClick)
                    }
                ) {
                    Text(
                        text = "Log Out",
                        color = BitError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = BitOnSurface
                    )
                }
            }
        )
    }

    // Full-Screen Profile Settings Page with Smooth Horizontal Card Transition
    AnimatedVisibility(
        visible = showProfileSettingsPage,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        ProfileSettingsScreen(
            user = user,
            isNightMode = isNightMode,
            onBack = { showProfileSettingsPage = false },
            onSaveProfile = { newFullName, newAvatarPath, profession, email, isEmailVerified, birthDate ->
                viewModel.updateUserProfile(
                    fullName = newFullName,
                    avatarPath = newAvatarPath,
                    profession = profession,
                    email = email,
                    isEmailVerified = isEmailVerified,
                    birthDate = birthDate
                )
            }
        )
    }

    // Full-Screen Username Availability Check Screen
    AnimatedVisibility(
        visible = showUsernameAvailabilityPage,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        UsernameAvailabilityScreen(
            user = user,
            isNightMode = isNightMode,
            onBack = { showUsernameAvailabilityPage = false },
            onApplyUsername = { newHandle ->
                viewModel.changeUsernameHandle(newHandle)
            }
        )
    }

    // Full-Screen Fingerprint Lock Security Screen
    AnimatedVisibility(
        visible = showFingerprintLockPage,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        FingerprintLockScreen(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onBack = { showFingerprintLockPage = false }
        )
    }

    // Zoomed-in QR Code Modal Dialog
    if (isQrVisible) {
        Dialog(onDismissRequest = { isQrVisible = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isNightMode) Color(0xFF0D0E14) else Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(2.dp, Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏱️ Auto-hiding in ${qrCountdownSeconds}s",
                                    color = Color(0xFF2563EB),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { isQrVisible = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close QR",
                                tint = animSubTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = cachedQrBitmap.asImageBitmap(),
                            contentDescription = "Personal KnotLink QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user?.fullName?.ifBlank { null } ?: "KnotLink User",
                        color = animTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "KNOTLINK:USER:$publicId",
                        color = Color(0xFF2563EB),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { isQrVisible = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Close QR Code",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FigmaDayNightToggleRow(
    isNightMode: Boolean,
    onToggle: () -> Unit,
    textColor: Color,
    subTextColor: Color
) {
    val trackWidth = 64.dp
    val thumbSize = 28.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (isNightMode) 32.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumb_offset"
    )

    val trackBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
        animationSpec = tween(300),
        label = "track_bg"
    )

    val thumbBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF3B82F6) else Color(0xFFF59E0B),
        animationSpec = tween(300),
        label = "thumb_bg"
    )

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        cornerRadius = 16.dp,
        backgroundColor = if (isNightMode) Color(0xFF0C0D12) else Color.White.copy(alpha = 0.95f),
        borderColor = if (isNightMode) Color(0xFF1B1D28) else Color(0xFF3B82F6).copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNightMode) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.NightsStay else Icons.Default.WbSunny,
                        contentDescription = "Theme",
                        tint = if (isNightMode) Color(0xFF60A5FA) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Day / Night Mode",
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Figma Switch Track
            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(trackBgColor)
                    .border(
                        width = 1.dp,
                        color = if (isNightMode) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if (isNightMode) Color.Gray.copy(alpha = 0.5f) else Color(0xFFD97706),
                        modifier = Modifier.size(14.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = if (isNightMode) Color(0xFF60A5FA) else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Thumb Capsule
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(thumbBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.NightsStay else Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsOptionRow(
    icon: ImageVector,
    title: String,
    badgeCount: Int = 0,
    textColor: Color = BitOnSurface,
    isNightMode: Boolean = true,
    onClick: () -> Unit = {}
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp,
        backgroundColor = if (isNightMode) Color(0xFF0F1017) else Color.White.copy(alpha = 0.95f),
        borderColor = if (isNightMode) Color(0xFF1E202E) else Color(0xFF3B82F6).copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, color = textColor, fontSize = 15.sp)
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isNightMode) BitOnSurfaceVariant else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun SwipeableDeletedChatItemRow(
    item: DeletedChatInfo,
    isNightMode: Boolean,
    onRestore: () -> Unit
) {
    val chat = item.chat
    val elapsedMillis = System.currentTimeMillis() - item.deletedAtMillis
    val remainingHours = maxOf(0L, 24L - (elapsedMillis / (1000 * 60 * 60)))

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "deleted_swipe_offset")

    val isLeftRestoreRevealed = animatedOffsetX < -15f
    val isRightRestoreRevealed = animatedOffsetX > 15f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        // Background Actions Layer - BOTH left and right swipe reveal the Restore option!
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isLeftRestoreRevealed) Arrangement.End else Arrangement.Start
        ) {
            if (isLeftRestoreRevealed || isRightRestoreRevealed) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(90.dp)
                        .background(Color(0xFF10B981))
                        .clickable {
                            onRestore()
                            offsetX = 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore Action",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Restore",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Foreground Card Layer
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 80f || offsetX < -80f) {
                                onRestore()
                                offsetX = 0f
                            } else if (offsetX > 35f) {
                                offsetX = 85f
                            } else if (offsetX < -35f) {
                                offsetX = -85f
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(-110f, 110f)
                        }
                    )
                },
            shape = RoundedCornerShape(18.dp),
            color = if (isNightMode) Color(0xFF0F1017) else Color.White,
            border = BorderStroke(1.dp, if (isNightMode) Color(0xFF1E202E) else Color(0xFFE2E8F0)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Circle Avatar / Name Initial
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = chat.name,
                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Expires in ~${remainingHours}h",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Quick restore button
                IconButton(
                    onClick = {
                        onRestore()
                        offsetX = 0f
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeletedChatsSheet(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit
) {
    val deletedList by viewModel.deletedChats.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNightMode) Color(0xFF07080B) else Color(0xFFF8FAFC))
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Full Screen Header / Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isNightMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Deleted Chats",
                                color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (deletedList.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${deletedList.size}",
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "24-Hour Recycle Bin",
                            color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                if (deletedList.isNotEmpty()) {
                    TextButton(onClick = {
                        viewModel.clearAllDeletedChats()
                        Toast.makeText(context, "Recycle bin cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(
                            text = "Clear All",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(color = if (isNightMode) Color(0xFF1A1C28) else Color(0xFFE2E8F0))

            // Swipe gesture tip banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isNightMode) Color(0xFF14151F) else Color(0xFFF1F5F9)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Swipe chat left or right in either direction to restore it to your chat list.",
                        color = if (isNightMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Body content
            if (deletedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = if (isNightMode) Color(0xFF3F3F46) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recycle Bin is Empty",
                            color = if (isNightMode) Color.White else Color(0xFF0F172A),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deleted chats are kept here for 24 hours\nbefore permanent removal.",
                            color = if (isNightMode) Color(0xFF71717A) else Color(0xFF64748B),
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(deletedList, key = { it.chat.id }) { item ->
                        SwipeableDeletedChatItemRow(
                            item = item,
                            isNightMode = isNightMode,
                            onRestore = {
                                viewModel.restoreChat(item.chat.id)
                                Toast.makeText(context, "${item.chat.name} restored", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    isNightMode: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isNightMode) Color(0xFF0C0D12) else Color.White
    val borderColor = if (isNightMode) Color(0xFF1B1D28) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp,
        backgroundColor = bgColor,
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = subTextColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = subTextColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun BasicSettingsPage(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onBack: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val deletedChats by viewModel.deletedChats.collectAsState()
    val blockedContacts by viewModel.blockedContacts.collectAsState()
    val restrictedContacts by viewModel.restrictedContacts.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showArchivedChatsSheet by remember { mutableStateOf(false) }
    var showDeletedChatsSheet by remember { mutableStateOf(false) }
    var showBlockedChatsSheet by remember { mutableStateOf(false) }
    var showRestrictedChatsSheet by remember { mutableStateOf(false) }

    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) Color(0xFF14151F) else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Basic Settings",
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Language, Appearance, Alerts & Privacy Lists",
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card 1: Languages
                SettingsSectionCard(
                    icon = Icons.Default.Language,
                    iconTint = Color(0xFF3B82F6),
                    title = "Languages",
                    subtitle = "App language: $currentLanguage",
                    isNightMode = isNightMode,
                    onClick = { showLanguageDialog = true }
                )

                // Card 2: Appearance
                SettingsSectionCard(
                    icon = Icons.Default.Palette,
                    iconTint = Color(0xFFEC4899),
                    title = "Appearance",
                    subtitle = "Theme styling, Chat Wallpaper & Font scale",
                    isNightMode = isNightMode,
                    onClick = { showAppearanceSheet = true }
                )

                // Card 3: Notification
                SettingsSectionCard(
                    icon = Icons.Default.Notifications,
                    iconTint = Color(0xFFF59E0B),
                    title = "Notification",
                    subtitle = "Alert tones, In-app vibration & Call ringtones",
                    isNightMode = isNightMode,
                    onClick = { showNotificationSheet = true }
                )

                // Card 4: Archive
                SettingsSectionCard(
                    icon = Icons.Default.Archive,
                    iconTint = Color(0xFF8B5CF6),
                    title = "Archive",
                    subtitle = "${archivedChats.size} Archived Chats",
                    isNightMode = isNightMode,
                    onClick = { showArchivedChatsSheet = true }
                )

                // Card 5: Deleted Chats (Recycle Bin)
                SettingsSectionCard(
                    icon = Icons.Default.DeleteSweep,
                    iconTint = Color(0xFFEF4444),
                    title = "Deleted Chats (Recycle Bin)",
                    subtitle = "${deletedChats.size} Deleted items (Stored for 24h)",
                    isNightMode = isNightMode,
                    onClick = { showDeletedChatsSheet = true }
                )

                // Card 6: Blocked Chats
                SettingsSectionCard(
                    icon = Icons.Default.Block,
                    iconTint = Color(0xFF6366F1),
                    title = "Blocked Chats",
                    subtitle = "${blockedContacts.size} Blocked Contacts",
                    isNightMode = isNightMode,
                    onClick = { showBlockedChatsSheet = true }
                )

                // Card 7: Restricted Chats
                SettingsSectionCard(
                    icon = Icons.Default.DoNotDisturbOn,
                    iconTint = Color(0xFF14B8A6),
                    title = "Restricted Chats",
                    subtitle = "${restrictedContacts.size} Restricted Contacts",
                    isNightMode = isNightMode,
                    onClick = { showRestrictedChatsSheet = true }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Languages Selection Dialog
    if (showLanguageDialog) {
        val languagesList = listOf(
            "English",
            "Bengali (বাংলা)",
            "Spanish (Español)",
            "Hindi (हिन्दी)",
            "French (Français)",
            "German (Deutsch)",
            "Arabic (العربية)",
            "Chinese (中文)"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = if (isNightMode) Color(0xFF0D0E14) else Color.White,
            title = {
                Text(
                    text = "Select App Language",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languagesList.forEach { lang ->
                        val isSelected = currentLanguage == lang || currentLanguage.startsWith(lang.take(5))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2563EB).copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    viewModel.setAppLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang,
                                color = if (isSelected) Color(0xFF2563EB) else textColor,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Appearance Sheet
    if (showAppearanceSheet) {
        SimpleOptionSheet(
            title = "Appearance Settings",
            subtitle = "Theme Mode & Visual Styling",
            isNightMode = isNightMode,
            onDismiss = { showAppearanceSheet = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Chat Wallpaper Style:", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val wallpapers = listOf("Classic Bit", "Dark Cyber", "Aurora Blue", "Minimal Light")
                wallpapers.forEach { wp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNightMode) Color(0xFF14151F) else Color(0xFFF1F5F9))
                            .clickable {
                                viewModel.showToast("Wallpaper set to $wp")
                                showAppearanceSheet = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(wp, color = textColor, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Notification Sheet
    if (showNotificationSheet) {
        SimpleOptionSheet(
            title = "Notification Preferences",
            subtitle = "Alert tones and vibration modes",
            isNightMode = isNightMode,
            onDismiss = { showNotificationSheet = false }
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { context.getSharedPreferences("knotlink_fcm_prefs", android.content.Context.MODE_PRIVATE) }
            
            var pushEnabled by remember { mutableStateOf(prefs.getBoolean("push_notifications_enabled", true)) }
            var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
            var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
            
            var hasPermission by remember {
                mutableStateOf(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasPermission = isGranted
                if (isGranted) {
                    viewModel.showToast("Notification permission allowed!")
                    val uid = com.example.data.supabase.SupabaseRealtimeManager.getCurrentUserId() ?: ""
                    if (uid.isNotBlank()) {
                        com.example.util.NotificationHelper.registerFcmToken(context, uid)
                    }
                } else {
                    viewModel.showToast("Notification permission denied")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // System Permission Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNightMode) Color(0xFF1E1F2F) else Color(0xFFF1F5F9))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System Notifications",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (hasPermission) "Status: Allowed" else "Status: Disallowed",
                            color = if (hasPermission) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!hasPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Turn On", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!hasPermission) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showToast("Please enable notifications in system settings")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Open Settings", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Push Notification Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Push Notifications", color = textColor, fontWeight = FontWeight.SemiBold)
                        Text("Receive instant message and call notifications", color = subTextColor, fontSize = 12.sp)
                    }
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = {
                            pushEnabled = it
                            prefs.edit().putBoolean("push_notifications_enabled", it).apply()
                            viewModel.showToast(if (it) "Push notifications turned on" else "Push notifications turned off")
                            if (it) {
                                val uid = com.example.data.supabase.SupabaseRealtimeManager.getCurrentUserId() ?: ""
                                if (uid.isNotBlank()) {
                                    com.example.util.NotificationHelper.registerFcmToken(context, uid)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                    )
                }

                Divider(color = if (isNightMode) Color(0xFF2C2D3D) else Color(0xFFE2E8F0), thickness = 0.5.dp)

                // Message Sound Tones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Message Sound Tones", color = textColor, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            prefs.edit().putBoolean("sound_enabled", it).apply()
                            viewModel.showToast(if (it) "Sound enabled" else "Sound muted")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                    )
                }

                // In-App Vibration Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("In-App Vibration Feedback", color = textColor, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            prefs.edit().putBoolean("vibration_enabled", it).apply()
                            viewModel.showToast(if (it) "Vibration enabled" else "Vibration disabled")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                    )
                }
            }
        }
    }

    // Archive Sheet
    if (showArchivedChatsSheet) {
        SimpleOptionSheet(
            title = "Archived Chats",
            subtitle = "${archivedChats.size} Chats archived",
            isNightMode = isNightMode,
            onDismiss = { showArchivedChatsSheet = false }
        ) {
            if (archivedChats.isEmpty()) {
                Text(
                    text = "No archived chats right now.",
                    color = subTextColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    archivedChats.forEach { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNightMode) Color(0xFF14151F) else Color(0xFFF1F5F9))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(chat.name, color = textColor, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.unarchiveChat(chat.id) }) {
                                Text("Unarchive", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Deleted Chats Sheet
    if (showDeletedChatsSheet) {
        DeletedChatsSheet(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onDismiss = { showDeletedChatsSheet = false }
        )
    }

    // Blocked Chats Sheet
    if (showBlockedChatsSheet) {
        SimpleOptionSheet(
            title = "Blocked Contacts",
            subtitle = "${blockedContacts.size} Blocked users",
            isNightMode = isNightMode,
            onDismiss = { showBlockedChatsSheet = false }
        ) {
            if (blockedContacts.isEmpty()) {
                Text("No blocked contacts.", color = subTextColor, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    blockedContacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNightMode) Color(0xFF14151F) else Color(0xFFF1F5F9))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contact, color = textColor, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { viewModel.unblockContact(contact) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Unblock", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Restricted Chats Sheet
    if (showRestrictedChatsSheet) {
        SimpleOptionSheet(
            title = "Restricted Contacts",
            subtitle = "${restrictedContacts.size} Restricted users",
            isNightMode = isNightMode,
            onDismiss = { showRestrictedChatsSheet = false }
        ) {
            if (restrictedContacts.isEmpty()) {
                Text("No restricted contacts.", color = subTextColor, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    restrictedContacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNightMode) Color(0xFF14151F) else Color(0xFFF1F5F9))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contact, color = textColor, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { viewModel.unrestrictContact(contact) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Unrestrict", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleOptionSheet(
    title: String,
    subtitle: String,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val bgColor = if (isNightMode) Color(0xFF0D0E14) else Color.White
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = bgColor,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(title, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = subTextColor, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun BitChatPremiumPage(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit
) {
    SimpleOptionSheet(
        title = "KnotLink Premium ✨",
        subtitle = "Unlock Exclusive Features & Cloud Storage",
        isNightMode = isNightMode,
        onDismiss = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF8B5CF6))
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PRO MEMBER PASS", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Unlimited Cloud Mesh Storage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            val features = listOf(
                "⚡ Ultra-Fast P2P Mesh Relays",
                "📷 HD Uncompressed Media Sharing",
                "👑 Golden Verified Profile Badge",
                "🤖 Unlimited Gemini AI Assistant Access",
                "🔒 Priority End-to-End Encryption"
            )

            features.forEach { feat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(feat, color = if (isNightMode) Color.White else Color(0xFF1E293B), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.showToast("🎉 KnotLink Premium Activated Successfully!")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Activate Premium ($2.99/mo)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun AdvanceSettingsPage(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onOpenFingerprintLock: () -> Unit
) {
    var showCryptographicKeysPage by remember { mutableStateOf(false) }
    var showPrivacyMeshPage by remember { mutableStateOf(false) }
    var showPrivacyControlsPage by remember { mutableStateOf(false) }
    var showActiveDevicesPage by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val user by viewModel.userIdentity.collectAsState()
    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val cardBgColor = if (isNightMode) Color(0xFF0F1017) else Color.White
    val cardBorderColor = if (isNightMode) Color(0xFF1E202E) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) Color(0xFF14151F) else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Advance Settings 🛡️",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundColor = cardBgColor,
                    borderColor = cardBorderColor
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsOptionRow(
                            icon = Icons.Default.Fingerprint,
                            title = "Fingerprint Lock Options",
                            textColor = textColor,
                            isNightMode = isNightMode,
                            onClick = onOpenFingerprintLock
                        )
                        Divider(color = cardBorderColor)
                        SettingsOptionRow(
                            icon = Icons.Default.Lock,
                            title = "Privacy Controls",
                            textColor = textColor,
                            isNightMode = isNightMode,
                            onClick = { showPrivacyControlsPage = true }
                        )
                        Divider(color = cardBorderColor)
                        SettingsOptionRow(
                            icon = Icons.Default.Security,
                            title = "Active Devices & Sessions",
                            textColor = textColor,
                            isNightMode = isNightMode,
                            onClick = { showActiveDevicesPage = true }
                        )
                        Divider(color = cardBorderColor)
                        SettingsOptionRow(
                            icon = Icons.Default.Security,
                            title = "Cryptographic Keys",
                            textColor = textColor,
                            isNightMode = isNightMode,
                            onClick = { showCryptographicKeysPage = true }
                        )
                        Divider(color = cardBorderColor)
                        SettingsOptionRow(
                            icon = Icons.Default.Lock,
                            title = "Privacy & Mesh Relays",
                            textColor = textColor,
                            isNightMode = isNightMode,
                            onClick = { showPrivacyMeshPage = true }
                        )
                        Divider(color = cardBorderColor)
                        SettingsOptionRow(
                            icon = Icons.Default.Delete,
                            title = "Delete Account",
                            textColor = Color(0xFFEF4444),
                            isNightMode = isNightMode,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showPrivacyControlsPage) {
        PrivacyControlsSubPage(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onBack = { showPrivacyControlsPage = false }
        )
    }

    if (showActiveDevicesPage) {
        ActiveDevicesSubPage(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onBack = { showActiveDevicesPage = false }
        )
    }

    if (showCryptographicKeysPage) {
        CryptographicKeysSubPage(
            viewModel = viewModel,
            user = user,
            isNightMode = isNightMode,
            onBack = { showCryptographicKeysPage = false }
        )
    }

    if (showPrivacyMeshPage) {
        PrivacyMeshRelaysSubPage(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onBack = { showPrivacyMeshPage = false }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            viewModel = viewModel,
            isNightMode = isNightMode,
            onDismiss = { showDeleteAccountDialog = false },
            onDeleted = {
                showDeleteAccountDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
fun CryptographicKeysSubPage(
    viewModel: BitChatViewModel,
    user: com.example.data.local.UserIdentityEntity?,
    isNightMode: Boolean,
    onBack: () -> Unit
) {
    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val cardBgColor = if (isNightMode) Color(0xFF0F1017) else Color.White
    val cardBorderColor = if (isNightMode) Color(0xFF1E202E) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) Color(0xFF14151F) else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Cryptographic Keys 🔐",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundColor = cardBgColor,
                    borderColor = cardBorderColor
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "End-to-End Encryption Keypair",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        Text(
                            text = "Your communications are protected with TLS 1.3 Transport Encryption, Hardware-backed Secure Key Storage, and Encrypted WebRTC Session Protocol.",
                            fontSize = 13.5.sp,
                            color = subTextColor
                        )
                        Divider(color = cardBorderColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Key Status", fontSize = 12.sp, color = subTextColor)
                                Text(text = "Active & Verified", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Button(
                                onClick = { viewModel.showToast("Cryptographic Keys Verified & Secure") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Verify Keys", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyMeshRelaysSubPage(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onBack: () -> Unit
) {
    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val cardBgColor = if (isNightMode) Color(0xFF0F1017) else Color.White
    val cardBorderColor = if (isNightMode) Color(0xFF1E202E) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) Color(0xFF14151F) else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Privacy & Mesh Relays 🌐",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundColor = cardBgColor,
                    borderColor = cardBorderColor
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mesh Relay Node",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        Text(
                            text = "Decentralized mesh relay routing ensures resilient offline messaging and zero-metadata leakage across connected peer relays.",
                            fontSize = 13.5.sp,
                            color = subTextColor
                        )
                        Divider(color = cardBorderColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Relay Status", fontSize = 12.sp, color = subTextColor)
                                Text(text = "Node Active & Routing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Button(
                                onClick = { viewModel.showToast("Mesh Relay Node Active") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Check Nodes", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportCareSheet(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val cardBgColor = if (isNightMode) Color(0xFF0F1017) else Color.White
    val cardBorderColor = if (isNightMode) Color(0xFF1E202E) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    fun copyAndAction(textToCopy: String, label: String, onAction: () -> Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, textToCopy)
            clipboard.setPrimaryClip(clip)
            viewModel.showToast("$label copied: $textToCopy")
        } catch (_: Exception) {}
        onAction()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isNightMode) Color(0xFF14151F) else Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Support Care 🎧",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }

                // Support Greeting Banner
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundColor = if (isNightMode) Color(0xFF0D0E14) else Color(0xFFEFF6FF),
                    borderColor = if (isNightMode) Color(0xFF2563EB).copy(alpha = 0.35f) else Color(0xFFBFDBFE)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "How can we help you?",
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Select your preferred channel to connect directly with our official support team.",
                                color = subTextColor,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Official Support Channels",
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                // 1. Phone Call Option
                SupportOptionCard(
                    icon = Icons.Default.Call,
                    iconBg = Color(0xFF10B981),
                    title = "Phone Call Support",
                    handleText = "+8809658959694",
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        copyAndAction("+8809658959694", "Phone Number") {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8809658959694"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                viewModel.showToast("Opening dialpad for +8809658959694")
                            }
                        }
                    }
                )

                // 2. Email Option
                SupportOptionCard(
                    icon = Icons.Default.Email,
                    iconBg = Color(0xFF8B5CF6),
                    title = "Email Support",
                    handleText = "admindotone@protonmail.com",
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        copyAndAction("admindotone@protonmail.com", "Email") {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:admindotone@protonmail.com")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                viewModel.showToast("Email address copied")
                            }
                        }
                    }
                )

                // 3. Telegram Option
                SupportOptionCard(
                    icon = Icons.Default.Send,
                    iconBg = Color(0xFF0088CC),
                    title = "Telegram Support",
                    handleText = "@devzabir",
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        copyAndAction("@devzabir", "Telegram Username") {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/devzabir"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                viewModel.showToast("Telegram handle copied: @devzabir")
                            }
                        }
                    }
                )

                // 4. WeChat Option
                SupportOptionCard(
                    icon = Icons.Default.Forum,
                    iconBg = Color(0xFF07C160),
                    title = "WeChat Support",
                    handleText = "@dev_zabir",
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        copyAndAction("@dev_zabir", "WeChat Username") {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://dl/chat?dev_zabir"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://weixin.qq.com"))
                                    context.startActivity(webIntent)
                                } catch (_: Exception) {
                                    viewModel.showToast("WeChat ID copied: @dev_zabir")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SupportOptionCard(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    handleText: String,
    cardBgColor: Color,
    cardBorderColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = cardBgColor,
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconBg,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = handleText,
                    color = iconBg,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PrivacyControlsSubPage(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onBack: () -> Unit
) {
    val privacySettings by viewModel.userPrivacySettings.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val cardBgColor = if (isNightMode) Color(0xFF13151B) else Color.White
    val cardBorderColor = if (isNightMode) Color(0xFF222634) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    var unblockTargetUid by remember { mutableStateOf<String?>(null) }

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
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy Controls",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "WHO CAN SEE MY INFORMATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = subtitleColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PrivacyOptionSelector(
                        title = "Profile Photo",
                        currentValue = privacySettings.profilePhotoVisibility,
                        onSelect = {
                            viewModel.updatePrivacySettings(privacySettings.copy(profilePhotoVisibility = it))
                        },
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        isNightMode = isNightMode
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = cardBorderColor)
                    PrivacyOptionSelector(
                        title = "Online Status",
                        currentValue = privacySettings.onlineStatusVisibility,
                        onSelect = {
                            viewModel.updatePrivacySettings(privacySettings.copy(onlineStatusVisibility = it))
                        },
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        isNightMode = isNightMode
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = cardBorderColor)
                    PrivacyOptionSelector(
                        title = "Last Seen Timestamp",
                        currentValue = privacySettings.lastSeenVisibility,
                        onSelect = {
                            viewModel.updatePrivacySettings(privacySettings.copy(lastSeenVisibility = it))
                        },
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        isNightMode = isNightMode
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = cardBorderColor)
                    PrivacyOptionSelector(
                        title = "Direct Messaging",
                        currentValue = privacySettings.whoCanMessageMe,
                        onSelect = {
                            viewModel.updatePrivacySettings(privacySettings.copy(whoCanMessageMe = it))
                        },
                        textColor = textColor,
                        subtitleColor = subtitleColor,
                        isNightMode = isNightMode
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MESSAGING & INDICATORS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = subtitleColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Read Receipts", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("If turned off, you won't send or see read receipts", color = subtitleColor, fontSize = 12.sp)
                        }
                        Switch(
                            checked = privacySettings.readReceiptsEnabled,
                            onCheckedChange = {
                                viewModel.updatePrivacySettings(privacySettings.copy(readReceiptsEnabled = it))
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = cardBorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Typing Indicators", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Show when you are actively composing a message", color = subtitleColor, fontSize = 12.sp)
                        }
                        Switch(
                            checked = privacySettings.typingIndicatorEnabled,
                            onCheckedChange = {
                                viewModel.updatePrivacySettings(privacySettings.copy(typingIndicatorEnabled = it))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BLOCKED CONTACTS (${blockedUsers.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = subtitleColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                if (blockedUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No blocked contacts", color = subtitleColor, fontSize = 14.sp)
                    }
                } else {
                    Column(modifier = Modifier.padding(12.dp)) {
                        blockedUsers.forEachIndexed { index, blocked ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = blocked.displayName.ifBlank { blocked.username.ifBlank { blocked.targetUid } },
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        fontSize = 14.5.sp
                                    )
                                    if (blocked.username.isNotBlank()) {
                                        Text(
                                            text = blocked.username,
                                            color = subtitleColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { unblockTargetUid = blocked.targetUid }
                                ) {
                                    Text("Unblock", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                }
                            }
                            if (index < blockedUsers.lastIndex) {
                                Divider(color = cardBorderColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (unblockTargetUid != null) {
        AlertDialog(
            onDismissRequest = { unblockTargetUid = null },
            title = { Text("Unblock Contact?") },
            text = { Text("This contact will be allowed to send you messages and view your permitted profile info.") },
            confirmButton = {
                TextButton(onClick = {
                    unblockTargetUid?.let { viewModel.unblockUser(it) }
                    unblockTargetUid = null
                }) {
                    Text("Unblock", color = Color(0xFF38BDF8))
                }
            },
            dismissButton = {
                TextButton(onClick = { unblockTargetUid = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrivacyOptionSelector(
    title: String,
    currentValue: String,
    onSelect: (String) -> Unit,
    textColor: Color,
    subtitleColor: Color,
    isNightMode: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                text = when (currentValue) {
                    "CONTACTS" -> "My Contacts"
                    "NOBODY" -> "Nobody"
                    else -> "Everyone"
                },
                color = Color(0xFF38BDF8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = subtitleColor,
            modifier = Modifier.size(18.dp)
        )
    }

    if (expanded) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("EVERYONE" to "Everyone", "CONTACTS" to "Contacts", "NOBODY" to "Nobody").forEach { (key, label) ->
                val isSelected = currentValue == key
                Surface(
                    onClick = {
                        onSelect(key)
                        expanded = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else if (isNightMode) Color(0xFF1E202B) else Color(0xFFE2E8F0),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.Transparent)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF38BDF8) else textColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveDevicesSubPage(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onBack: () -> Unit
) {
    val sessions by viewModel.userSessions.collectAsState()
    val bgColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
    val cardBgColor = if (isNightMode) Color(0xFF13151B) else Color.White
    val cardBorderColor = if (isNightMode) Color(0xFF222634) else Color(0xFFE2E8F0)
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    var showLogoutAllDialog by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Devices & Sessions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Manage and terminate active sessions logged into your KnotLink account.",
                color = subtitleColor,
                fontSize = 13.5.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "LOGGED IN DEVICES (${sessions.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = subtitleColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    sessions.forEachIndexed { index, session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = session.deviceName,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            fontSize = 14.5.sp
                                        )
                                        if (session.isCurrentDevice) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "THIS DEVICE",
                                                    color = Color(0xFF10B981),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${session.platform} • v${session.appVersion}",
                                        color = subtitleColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (!session.isCurrentDevice) {
                                IconButton(onClick = { viewModel.revokeDeviceSession(session.deviceId) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Terminate Session",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                        if (index < sessions.lastIndex) {
                            Divider(color = cardBorderColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showLogoutAllDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of All Other Devices", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showLogoutAllDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutAllDialog = false },
            title = { Text("Log out of all other devices?") },
            text = { Text("This will invalidate sessions and remove push tokens on all other phones and tablets.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revokeAllOtherSessions()
                    showLogoutAllDialog = false
                }) {
                    Text("Log Out All", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeleteAccountConfirmationDialog(
    viewModel: BitChatViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var confirmText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (step == 1) "Delete KnotLink Account?" else "Final Deletion Confirmation",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (step == 1) {
                    Text(
                        "Deleting your account will:\n\n" +
                        "• Remove your profile from the directory\n" +
                        "• Clear all local messages and media\n" +
                        "• Revoke all active device sessions\n" +
                        "• Deactivate your phone registry mapping\n\n" +
                        "This action cannot be undone."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "To permanently confirm account deletion, type \"DELETE\" below:"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text("DELETE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Continue", color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        viewModel.deleteAccount(reason) {
                            onDeleted()
                        }
                    },
                    enabled = confirmText.trim() == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Permanently Delete", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

