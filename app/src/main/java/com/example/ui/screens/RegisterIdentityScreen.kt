package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.components.GlassPanel
import com.example.ui.components.ImageCropDialog
import com.example.ui.theme.BitBackground
import com.example.ui.theme.BitError
import com.example.ui.theme.BitOnPrimary
import com.example.ui.theme.BitOnSurface
import com.example.ui.theme.BitOnSurfaceVariant
import com.example.ui.theme.BitPrimary
import com.example.ui.theme.BitSecondary
import com.example.ui.viewmodel.BitChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RegisterIdentityScreen(
    viewModel: BitChatViewModel,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fullName by viewModel.enteredFullName.collectAsState()
    val avatarPath by viewModel.enteredAvatarPath.collectAsState()
    val username by viewModel.enteredUsername.collectAsState()
    val isChecking by viewModel.isCheckingUsername.collectAsState()
    val isAvailable by viewModel.usernameAvailability.collectAsState()
    val domainSuffix = ".link"

    var showPhotoPickerOptions by remember { mutableStateOf(false) }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var accountCreationStep by remember { mutableStateOf(0) }
    var isArrowLaunching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (username.length >= 3 && isAvailable == null) {
            viewModel.updateUsername(username)
        }
    }

    LaunchedEffect(isCreatingAccount) {
        if (isCreatingAccount) {
            accountCreationStep = 0
            delay(900)
            accountCreationStep = 1
            delay(900)
            accountCreationStep = 2
            delay(700)
            viewModel.completeRegistration(
                onSuccess = onContinueClick,
                onError = {
                    isCreatingAccount = false
                    isArrowLaunching = false
                }
            )
        }
    }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoToCropPath by remember { mutableStateOf<String?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraFile != null && cameraFile!!.exists()) {
            try {
                photoToCropPath = cameraFile!!.absolutePath
                showCropDialog = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val launchCameraAction = {
        try {
            val tempFile = File(context.cacheDir, "temp_reg_cam_${System.currentTimeMillis()}.jpg")
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "raw_gallery_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                photoToCropPath = file.absolutePath
                showCropDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BitBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = BitPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KnotLink",
                        color = BitPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { /* Help */ }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = BitOnSurfaceVariant
                    )
                }
            }

            // Main Glass Card Container (Scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    cornerRadius = 28.dp,
                    backgroundColor = Color(0x9916161A),
                    borderColor = Color.White.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Centered Profile Photo Selection Circle (Enhanced with Gradient Outline & Overlapping Camera Badge)
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .clickable { showPhotoPickerOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val photoBorderColor = if (!avatarPath.isNullOrBlank()) {
                                Color.White.copy(alpha = 0.3f)
                            } else {
                                Color(0xFF38BDF8)
                            }

                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(BitPrimary, BitSecondary)
                                        )
                                    )
                                    .border(2.5.dp, photoBorderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!avatarPath.isNullOrBlank()) {
                                    AsyncImage(
                                        model = File(avatarPath!!),
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
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            // Camera Badge Overlay (Half inside, half outside bottom-right edge - Blue)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                                    .border(2.dp, Color(0xFF16161A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (avatarPath.isNullOrBlank()) {
                            Text(
                                text = "Photo required *",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            text = "Setup Your Digital Footprint For KnotLink",
                            color = BitOnSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "These identities will be used to interact with you on KnotLink.",
                            color = BitOnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Full Name Input Box
                        Text(
                            text = "Full Name",
                            color = BitOnSurface.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp, start = 4.dp)
                        )

                        val nameFocusRequester = remember { FocusRequester() }
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nameFocusRequester.requestFocus() },
                            cornerRadius = 16.dp,
                            borderColor = Color.White.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val hasNameText = fullName.isNotBlank()
                                val iconScale by animateFloatAsState(
                                    targetValue = if (hasNameText) 1.15f else 1f,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "FullNameIconScale"
                                )
                                val iconTint by animateColorAsState(
                                    targetValue = if (hasNameText) BitSecondary else BitOnSurfaceVariant,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "FullNameIconTint"
                                )

                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Name",
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(iconScale)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (fullName.isEmpty()) {
                                        Text(
                                            text = "e.g. Abdullah Al Mamun",
                                            color = Color.White.copy(alpha = 0.25f),
                                            fontSize = 15.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = fullName,
                                        onValueChange = { viewModel.updateFullName(it) },
                                        modifier = Modifier.focusRequester(nameFocusRequester),
                                        textStyle = TextStyle(
                                            color = BitOnSurface,
                                            fontSize = 15.sp
                                        ),
                                        cursorBrush = SolidColor(BitSecondary),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Username Input Box
                        Text(
                            text = "Username Handle",
                            color = BitOnSurface.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp, start = 4.dp)
                        )

                        var showUsernameStatus by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        val usernameBorderColor = when {
                            isAvailable == true -> BitSecondary
                            isAvailable == false -> BitError
                            else -> Color.White.copy(alpha = 0.12f)
                        }

                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            borderColor = usernameBorderColor
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val atSignTint = when {
                                    isAvailable == true -> BitSecondary
                                    isAvailable == false -> BitError
                                    else -> BitOnSurfaceVariant
                                }

                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = "@",
                                    tint = atSignTint,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            showUsernameStatus = true
                                            coroutineScope.launch {
                                                delay(3000)
                                                showUsernameStatus = false
                                            }
                                        }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (username.isEmpty()) {
                                        Text(
                                            text = "yourname",
                                            color = Color.White.copy(alpha = 0.25f),
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicTextField(
                                            value = username,
                                            onValueChange = { viewModel.updateUsername(it) },
                                            textStyle = TextStyle(
                                                color = BitOnSurface,
                                                fontSize = 15.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            cursorBrush = SolidColor(BitSecondary),
                                            singleLine = true
                                        )

                                        Text(
                                            text = domainSuffix,
                                            color = BitSecondary,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Character Count & Real-time Availability Status Indicator below input box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (isChecking) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = BitSecondary,
                                            strokeWidth = 1.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Checking availability...",
                                            color = BitOnSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else if (username.isNotBlank()) {
                                    if (username.length < 3) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = BitError,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Minimum 3 alphanumeric characters",
                                                color = BitError,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (isAvailable == true) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = BitSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "@$username$domainSuffix is available",
                                                color = BitSecondary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (isAvailable == false) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = BitError,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "@$username$domainSuffix is already taken",
                                                color = BitError,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.updateUsername(username) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Retry",
                                                tint = BitOnSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tap to verify availability",
                                                color = BitOnSurfaceVariant,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "${username.length}/20",
                                color = BitOnSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Continue Button with Arrow Launch Animation
                        val arrowOffsetDx by animateDpAsState(
                            targetValue = if (isArrowLaunching) 50.dp else 0.dp,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "RegisterArrowLaunch"
                        )

                        val isFormValid = isAvailable == true && !isChecking && username.length >= 3 && fullName.isNotBlank() && !avatarPath.isNullOrBlank()

                        Button(
                            onClick = {
                                if (!isArrowLaunching && isFormValid) {
                                    coroutineScope.launch {
                                        isArrowLaunching = true
                                        delay(200)
                                        isCreatingAccount = true
                                    }
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BitPrimary,
                                contentColor = BitOnPrimary,
                                disabledContainerColor = BitPrimary.copy(alpha = 0.3f),
                                disabledContentColor = BitOnPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.offset(x = arrowOffsetDx)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "By continuing, you agree to KnotLink Protocols and decentralized identity guidelines.",
                            color = BitOnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Choose Photo Source Dialog (Camera vs Gallery) - Equal Sized Columns
    if (showPhotoPickerOptions) {
        Dialog(onDismissRequest = { showPhotoPickerOptions = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF181924),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Profile Photo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Camera Option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF222332))
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
                            Text("Take Photo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Gallery Option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF222332))
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
                            Text("Gallery", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (!avatarPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable {
                                    showPhotoPickerOptions = false
                                    photoToCropPath = avatarPath
                                    showCropDialog = true
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Re-crop current photo",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(
                        onClick = { showPhotoPickerOptions = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = BitOnSurfaceVariant)
                    }
                }
            }
        }
    }

    // Image Cropping Dialog
    if (showCropDialog && photoToCropPath != null) {
        ImageCropDialog(
            imageSource = photoToCropPath,
            isCircularMask = true,
            onDismiss = {
                showCropDialog = false
                photoToCropPath = null
            },
            onCropSuccess = { croppedFilePath ->
                showCropDialog = false
                photoToCropPath = null
                viewModel.updateAvatarPath(croppedFilePath)
                Toast.makeText(context, "Profile photo cropped and updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Account Creation Interactive Loading Overlay
    if (isCreatingAccount) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090A0F).copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF161722),
                shadowElevation = 24.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BitSecondary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "LoadingPulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.92f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "PulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(BitSecondary.copy(alpha = 0.15f))
                            .border(2.dp, BitSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (accountCreationStep) {
                                0 -> Icons.Default.Shield
                                1 -> Icons.Default.LockPerson
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (accountCreationStep) {
                                2 -> Color(0xFF10B981)
                                else -> BitSecondary
                            },
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when (accountCreationStep) {
                            0 -> "Observing Network..."
                            1 -> "Processing Cryptographic Keys..."
                            else -> "Done! Welcome to KnotLink"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (accountCreationStep) {
                            0 -> "Connecting to decentralized secure relay nodes"
                            1 -> "Encrypting identity and generating secure tokens"
                            else -> "Identity setup completed successfully"
                        },
                        color = BitOnSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..2) {
                            val isActive = i <= accountCreationStep
                            val barColor by animateColorAsState(
                                targetValue = if (isActive) BitSecondary else Color.White.copy(alpha = 0.15f),
                                animationSpec = tween(300),
                                label = "BarColor_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
