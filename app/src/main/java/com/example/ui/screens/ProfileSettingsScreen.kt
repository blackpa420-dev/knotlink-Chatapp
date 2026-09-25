package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.UserIdentityEntity
import com.example.util.QRCodeGenerator
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

val PROFESSION_LIST = listOf(
    "🎓 Student",
    "💼 Job Holder",
    "🏛️ Government Employee",
    "💻 App Developer",
    "🌐 Web Developer",
    "👨‍💻 Software Engineer",
    "🎨 UI/UX Designer",
    "🎨 Graphic Designer",
    "📱 Content Creator",
    "🎥 YouTuber",
    "📸 Photographer",
    "✂️ Video Editor",
    "📈 Digital Marketer",
    "💼 Freelancer",
    "🛒 E-commerce Owner",
    "🛍️ F-Commerce Seller",
    "🚀 Entrepreneur",
    "🏪 Business Owner",
    "👨‍🏫 Teacher",
    "⚕️ Doctor",
    "⚖️ Lawyer",
    "🏗️ Engineer",
    "🏦 Banker",
    "🔍 Looking for Job",
    "✨ Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    user: UserIdentityEntity?,
    isNightMode: Boolean,
    onBack: () -> Unit,
    onSaveProfile: (
        fullName: String,
        avatarPath: String?,
        profession: String,
        email: String,
        isEmailVerified: Boolean,
        birthDate: String
    ) -> Unit
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(user?.fullName ?: "") }
    var currentAvatarPath by remember { mutableStateOf(user?.avatarPath) }
    var profession by remember { mutableStateOf(user?.profession ?: "🎓 Student") }
    var secondaryEmail by remember { mutableStateOf("") }
    var isSecondaryEmailVerified by remember { mutableStateOf(false) }
    var secondaryEmailVerificationSent by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf(user?.birthDate ?: "") }

    LaunchedEffect(user) {
        if (user != null) {
            fullName = user.fullName
            currentAvatarPath = user.avatarPath
            if (!user.profession.isNullOrBlank()) {
                profession = user.profession
            }
            if (!user.birthDate.isNullOrBlank()) {
                birthDate = user.birthDate
            }
        }
    }

    var pendingBitmapForCrop by remember { mutableStateOf<Bitmap?>(null) }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var showPhotoPickerOptions by remember { mutableStateOf(false) }
    var showProfessionSheet by remember { mutableStateOf(false) }

    // Camera Uri state for full-res capture
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    // QR Code State & Memory Caching
    val haptic = LocalHapticFeedback.current
    val publicId = remember(user) { user?.publicId ?: "usr_8921" }

    var isQrVisible by remember { mutableStateOf(false) }
    var qrCountdownSeconds by remember { mutableIntStateOf(10) }

    // Memory cached QR Code Bitmap (regenerates ONLY if publicId changes)
    val cachedQrBitmap = remember(publicId, context) {
        QRCodeGenerator.generateProfileQRCode(publicId = publicId, context = context, size = 512)
    }

    // Auto-hide 10-second timer
    LaunchedEffect(isQrVisible, qrCountdownSeconds) {
        if (isQrVisible && qrCountdownSeconds > 0) {
            delay(1000L)
            qrCountdownSeconds -= 1
        } else if (isQrVisible && qrCountdownSeconds <= 0) {
            isQrVisible = false
        }
    }

    // Cancel timer and reset QR visibility when user leaves screen
    DisposableEffect(Unit) {
        onDispose {
            isQrVisible = false
        }
    }

    // Registered Account Email (Locked)
    val registeredEmail = user?.email?.ifBlank { "user@knotlink.com" } ?: "user@knotlink.com"

    // Username is fixed with .link
    val rawUsername = user?.username ?: "user"
    val cleanName = rawUsername.removePrefix("@").removeSuffix(".chat").removeSuffix(".bit").removeSuffix(".link")
    val formattedHandle = "${cleanName.ifBlank { "user" }}.link"

    val bdDateToday = remember { com.example.data.local.getBdCurrentJoinedDate() }
    val joinedDate = if (user?.joinedDate.isNullOrBlank() || user?.joinedDate == "28 Jul 2026") {
        bdDateToday
    } else {
        user?.joinedDate ?: bdDateToday
    }

    // Date Picker Dialog Setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            birthDate = "$selectedDay ${monthNames[selectedMonth]} $selectedYear"
        },
        calendar.get(Calendar.YEAR) - 20,
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // High-Res Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraFile != null && cameraFile!!.exists()) {
            try {
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bitmap = BitmapFactory.decodeFile(cameraFile!!.absolutePath, options)
                if (bitmap != null) {
                    val (isValid, dims) = checkBitmapResolution(bitmap)
                    if (isValid) {
                        pendingBitmapForCrop = bitmap
                    } else {
                        Toast.makeText(
                            context,
                            "Upload Photo must be over of 480p resulation.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to process photo from camera", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error reading camera picture: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchCameraAction = {
        try {
            val tempFile = File(context.cacheDir, "temp_profile_cam_${System.currentTimeMillis()}.jpg")
            cameraFile = tempFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraAction()
        } else {
            Toast.makeText(context, "Camera permission is required to take photo", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (isValid, dims) = checkUriResolution(context, uri)
            if (isValid) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            pendingBitmapForCrop = bitmap
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load selected image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Upload Photo must be over of 480p resulation.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val pageBg = if (isNightMode) Color(0xFF0D0E15) else Color(0xFFF8FAFC)
    val cardBg = if (isNightMode) Color(0xFF161722) else Color(0xFFFFFFFF)
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        color = pageBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isNightMode) Color(0xFF1E1F2C) else Color(0xFFE2E8F0))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Profile Settings",
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Profile Card Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar Container
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clickable {
                                showPhotoPickerOptions = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner circular avatar container
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2563EB), Color(0xFF06B6D4))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val hasAvatar = !currentAvatarPath.isNullOrBlank()
                            if (hasAvatar) {
                                val model: Any = if (currentAvatarPath!!.startsWith("http://") || currentAvatarPath!!.startsWith("https://") || currentAvatarPath!!.startsWith("content://")) {
                                    currentAvatarPath!!
                                } else {
                                    val clean = currentAvatarPath!!.removePrefix("file://")
                                    val f = File(clean)
                                    if (f.exists()) f else currentAvatarPath!!
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
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        // Camera Edit Badge Overlay (overlapping bottom-right edge of avatar)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                                .border(2.5.dp, cardBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // "Show My QR" Button
                    Button(
                        onClick = {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (e: Exception) {
                                // Ignore if haptics unsupported
                            }

                            if (isQrVisible) {
                                // Restart 10-second timer if already visible
                                qrCountdownSeconds = 10
                            } else {
                                isQrVisible = true
                                qrCountdownSeconds = 10
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isQrVisible) Color(0xFF2563EB).copy(alpha = 0.15f) else Color(0xFF2563EB),
                            contentColor = if (isQrVisible) Color(0xFF2563EB) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Code Scanner",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isQrVisible) "Hide QR (${qrCountdownSeconds}s)" else "Show My QR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. Full Name Input Field (1 to 3 words)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Full Name",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { input ->
                                fullName = input
                                fullNameError = validateFullName(input)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Alex Morgan", color = subTextColor.copy(alpha = 0.5f)) },
                            singleLine = true,
                            isError = fullNameError != null,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedContainerColor = if (isNightMode) Color(0xFF222332) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isNightMode) Color(0xFF1B1C28) else Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isNightMode) Color(0xFF2F3042) else Color(0xFFCBD5E1),
                                errorBorderColor = Color(0xFFEF4444)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (fullNameError != null) Color(0xFFEF4444) else Color(0xFF2563EB)
                                )
                            }
                        )

                        if (fullNameError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fullNameError!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            val wordCount = fullName.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Word count: $wordCount / 3 (1 to 3 words allowed)",
                                color = subTextColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Fixed Username Handle
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Username Handle",
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Fixed",
                                tint = subTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = formattedHandle,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textColor.copy(alpha = 0.85f),
                                disabledContainerColor = if (isNightMode) Color(0xFF141520) else Color(0xFFE2E8F0),
                                disabledBorderColor = if (isNightMode) Color(0xFF262736) else Color(0xFFCBD5E1)
                            ),
                            leadingIcon = {
                                Text(
                                    text = "@",
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Profession Selector Tile
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Your Profession",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showProfessionSheet = true },
                            color = if (isNightMode) Color(0xFF1B1C28) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isNightMode) Color(0xFF2F3042) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = profession,
                                        color = textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Select",
                                    tint = subTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Fixed Account Email (Registered Mail - Locked)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account Email",
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = subTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = registeredEmail,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textColor.copy(alpha = 0.85f),
                                disabledContainerColor = if (isNightMode) Color(0xFF141520) else Color(0xFFE2E8F0),
                                disabledBorderColor = if (isNightMode) Color(0xFF262736) else Color(0xFFCBD5E1),
                                disabledLeadingIconColor = subTextColor
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. Add Secondary Mail Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add secondary mail",
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isSecondaryEmailVerified) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = secondaryEmail,
                            onValueChange = {
                                secondaryEmail = it.lowercase()
                                isSecondaryEmailVerified = false
                                secondaryEmailVerificationSent = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. secondary@gmail.com", color = subTextColor.copy(alpha = 0.5f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedContainerColor = if (isNightMode) Color(0xFF222332) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isNightMode) Color(0xFF1B1C28) else Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isNightMode) Color(0xFF2F3042) else Color(0xFFCBD5E1)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Verify Button underneath
                        Button(
                            onClick = {
                                if (secondaryEmail.contains("@") && secondaryEmail.contains(".")) {
                                    secondaryEmailVerificationSent = true
                                    Toast.makeText(context, "Confirmation link sent to $secondaryEmail", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid secondary email address", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNightMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                contentColor = Color(0xFF2563EB)
                            )
                        ) {
                            Text(
                                text = if (secondaryEmailVerificationSent) "Resend Confirmation Link" else "Verify Secondary Email",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (secondaryEmailVerificationSent) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📧 Confirmation link has been sent to your secondary mail! Please check your inbox and click it to confirm.",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Birthdate Selection with Modern Calendar Icon Box
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Birthdate",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { datePickerDialog.show() },
                            color = if (isNightMode) Color(0xFF1B1C28) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isNightMode) Color(0xFF2F3042) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Calendar",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (birthDate.isNotBlank()) birthDate else "Date of Birth",
                                        color = if (birthDate.isNotBlank()) textColor else subTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Pick Date",
                                    tint = subTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7. Joined Date Display
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Account Joined Date",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isNightMode) Color(0xFF141520) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = joinedDate,
                                    color = textColor.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Save Profile Button
                    Button(
                        onClick = {
                            val err = validateFullName(fullName)
                            if (err != null) {
                                fullNameError = err
                            } else {
                                onSaveProfile(
                                    fullName.trim(),
                                    currentAvatarPath,
                                    profession,
                                    registeredEmail.trim(),
                                    true,
                                    birthDate
                                )
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Profile Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modern Profession Selection Sheet
    if (showProfessionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }

        val filteredList = remember(searchQuery) {
            if (searchQuery.isBlank()) PROFESSION_LIST else PROFESSION_LIST.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { showProfessionSheet = false },
            sheetState = sheetState,
            containerColor = if (isNightMode) Color(0xFF141522) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select Your Profession",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search profession...", color = subTextColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = if (isNightMode) Color(0xFF1E1F2E) else Color(0xFFF1F5F9),
                        unfocusedContainerColor = if (isNightMode) Color(0xFF181926) else Color(0xFFF8FAFC)
                    ),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = subTextColor)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredList) { item ->
                        val isSelected = item == profession
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    profession = item
                                    showProfessionSheet = false
                                },
                            color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.15f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    color = if (isSelected) Color(0xFF2563EB) else textColor,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF2563EB)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Choose Photo Source Dialog (Camera vs Gallery)
    if (showPhotoPickerOptions) {
        Dialog(onDismissRequest = { showPhotoPickerOptions = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isNightMode) Color(0xFF181924) else Color(0xFFFFFFFF),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Profile Photo",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Camera Option via FileProvider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isNightMode) Color(0xFF222332) else Color(0xFFF1F5F9))
                                .clickable {
                                    showPhotoPickerOptions = false
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPermission) {
                                        launchCameraAction()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                                .padding(vertical = 24.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Take Photo", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Gallery Option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isNightMode) Color(0xFF222332) else Color(0xFFF1F5F9))
                                .clickable {
                                    showPhotoPickerOptions = false
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(vertical = 24.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Gallery",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gallery", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(
                        onClick = { showPhotoPickerOptions = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = subTextColor)
                    }
                }
            }
        }
    }

    // Image Crop Tool Screen
    if (pendingBitmapForCrop != null) {
        ImageCropScreen(
            srcBitmap = pendingBitmapForCrop!!,
            isNightMode = isNightMode,
            onDismiss = { pendingBitmapForCrop = null },
            onCropComplete = { croppedBitmap ->
                val savedPath = saveBitmapToInternalStorage(context, croppedBitmap)
                if (savedPath != null) {
                    currentAvatarPath = savedPath
                    Toast.makeText(context, "Photo cropped & saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error saving picture", Toast.LENGTH_SHORT).show()
                }
                pendingBitmapForCrop = null
            },
            onSkipCrop = { originalBitmap ->
                val savedPath = saveBitmapToInternalStorage(context, originalBitmap)
                if (savedPath != null) {
                    currentAvatarPath = savedPath
                    Toast.makeText(context, "Original photo set!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error saving picture", Toast.LENGTH_SHORT).show()
                }
                pendingBitmapForCrop = null
            }
        )
    }

    // 7. Floating Zoom-In QR Code Dialog Overlay (shows zoomed in with countdown timer)
    if (isQrVisible) {
        Dialog(onDismissRequest = { isQrVisible = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isNightMode) Color(0xFF1E1F2E) else Color.White,
                shadowElevation = 24.dp,
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
                                tint = subTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Zoomed-in Large QR Code
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
                        text = fullName.ifBlank { "KnotLink User" },
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "BITCHAT:USER:$publicId",
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
fun ImageCropScreen(
    srcBitmap: Bitmap,
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onCropComplete: (Bitmap) -> Unit,
    onSkipCrop: (Bitmap) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportPxSize by remember { mutableStateOf(IntSize.Zero) }

    val dialogBg = if (isNightMode) Color(0xFF0F0F14) else Color(0xFF1E293B)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = dialogBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crop Profile Photo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Pinch to zoom & drag to frame inside circle (${srcBitmap.width}x${srcBitmap.height}px)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Crop Viewport Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .onGloballyPositioned { coordinates ->
                            viewportPxSize = coordinates.size
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = srcBitmap.asImageBitmap(),
                        contentDescription = "Crop target",
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // Circular Crop Window Frame Overlay
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(2.5.dp, Color(0xFF2563EB), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onSkipCrop(srcBitmap) }
                    ) {
                        Text(
                            text = "Skip (Use Original)",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            val cropped = generateAccurateCroppedBitmap(
                                src = srcBitmap,
                                viewportPx = viewportPxSize,
                                scale = scale,
                                panOffset = offset
                            )
                            onCropComplete(cropped)
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(imageVector = Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Crop & Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

fun generateAccurateCroppedBitmap(
    src: Bitmap,
    viewportPx: IntSize,
    scale: Float,
    panOffset: Offset
): Bitmap {
    try {
        val vw = if (viewportPx.width > 0) viewportPx.width.toFloat() else 800f
        val vh = if (viewportPx.height > 0) viewportPx.height.toFloat() else 1000f

        val fitScale = minOf(vw / src.width, vh / src.height)
        val totalScale = fitScale * scale

        val imgCenterX = vw / 2f + panOffset.x
        val imgCenterY = vh / 2f + panOffset.y

        val cropRadiusPx = minOf(vw, vh) * 0.35f

        val outputSize = 1080
        val outBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        val matrix = Matrix()
        matrix.postTranslate(-src.width / 2f, -src.height / 2f)
        matrix.postScale(totalScale, totalScale)
        matrix.postTranslate(imgCenterX - (vw / 2f - cropRadiusPx), imgCenterY - (vh / 2f - cropRadiusPx))
        val outScale = outputSize / (cropRadiusPx * 2f)
        matrix.postScale(outScale, outScale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, matrix, paint)

        return outBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        val size = minOf(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return Bitmap.createBitmap(src, x, y, size, size)
    }
}

fun checkUriResolution(context: Context, uri: Uri): Pair<Boolean, Pair<Int, Int>> {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val w = options.outWidth
        val h = options.outHeight
        val isValid = (w >= 480 || h >= 480)
        Pair(isValid, Pair(w, h))
    } catch (e: Exception) {
        Pair(false, Pair(0, 0))
    }
}

fun checkBitmapResolution(bitmap: Bitmap): Pair<Boolean, Pair<Int, Int>> {
    val w = bitmap.width
    val h = bitmap.height
    val isValid = (w >= 480 || h >= 480)
    return Pair(isValid, Pair(w, h))
}

fun validateFullName(fullName: String): String? {
    val trimmed = fullName.trim()
    if (trimmed.isEmpty()) {
        return "Full name cannot be empty (1 to 3 words)"
    }
    val words = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (words.size > 3) {
        return "Full name cannot be over 3 words (Current: ${words.size})"
    }
    return null
}

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
