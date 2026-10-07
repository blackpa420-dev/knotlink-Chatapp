package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.components.BitChatBottomNavBar
import com.example.ui.components.BitChatNavTab
import com.example.ui.viewmodel.BitChatViewModel
import com.example.ui.viewmodel.ScannedUser
import com.example.ui.viewmodel.ScannedUserResult
import com.example.util.QRCodeGenerator
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.EnumMap
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    viewModel: BitChatViewModel,
    onBackClick: () -> Unit,
    onChatCreated: (String, String) -> Unit,
    onTabSelected: (BitChatNavTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val isNightMode by viewModel.isNightMode.collectAsState(initial = true)
    val user by viewModel.userIdentity.collectAsState()

    // Top Mode: "SCAN" (Live Camera) or "MY_QR" (Display Personal QR Code)
    var activeQrTab by remember { mutableStateOf("SCAN") } // "SCAN" or "MY_QR"

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isScanningActive by remember { mutableStateOf(true) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }

    var scannedUser by remember { mutableStateOf<ScannedUser?>(null) }
    var resolvedQrUuid by remember { mutableStateOf("") }

    // Resolve R2 avatar object keys before Coil renders the My QR profile.
    var resolvedAvatarPath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(user?.avatarPath, user?.supabaseUid) {
        resolvedAvatarPath = withContext(Dispatchers.IO) {
            val value = user?.avatarPath?.trim().orEmpty()
            when {
                value.isBlank() -> null
                value.startsWith("http://", ignoreCase = true) ||
                    value.startsWith("https://", ignoreCase = true) ||
                    value.startsWith("content://", ignoreCase = true) ||
                    value.startsWith("file://", ignoreCase = true) ||
                    value.startsWith("/") -> value
                value.startsWith("users/") -> {
                    // Retry once after auth/session readiness; this is common on
                    // a freshly opened QR screen.
                    var url = com.example.data.cloudflare.CloudflareR2Service
                        .getDownloadUrl(value, "image/jpeg").getOrNull()
                    if (url.isNullOrBlank()) {
                        kotlinx.coroutines.delay(350L)
                        url = com.example.data.cloudflare.CloudflareR2Service
                            .getDownloadUrl(value, "image/jpeg").getOrNull()
                    }
                    url
                }
                else -> value
            }
        }
    }

    // Personal QR Code Bitmap Generation
    // QR identity is the stable Supabase Auth/profile UID, never a local placeholder username.
    // The same canonical search flow can resolve this UID directly.
    val displayUsername = user?.username
        ?.trim()
        ?.let { if (it.startsWith("@")) it else "@$it" }
        ?.takeIf { it != "@" }
        ?: "@username"

    LaunchedEffect(user?.supabaseUid) {
        val localUuid = user?.supabaseUid?.trim().orEmpty()
        if (localUuid.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
            resolvedQrUuid = localUuid
        } else {
            resolvedQrUuid = com.example.data.supabase.SupabaseService
                .getAuthenticatedUserId()
                .orEmpty()
        }
    }

    // Never fall back to the legacy qrIdentifier here. A non-UUID fallback
    // produces KNOTLINK:INVALID, which the scanner correctly rejects.
    val qrIdentifier = resolvedQrUuid
    val qrBitmap = remember(qrIdentifier) {
        if (qrIdentifier.isBlank()) {
            null
        } else {
            try {
                QRCodeGenerator.generateProfileQRCode(userId = qrIdentifier, context = context, size = 512)
            } catch (e: Exception) {
                null
            }
        }
    }

    var isCopiedId by remember { mutableStateOf(false) }

    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF0A0B10) else Color(0xFFF8FAFC),
        animationSpec = tween(400),
        label = "qr_bg"
    )

    Scaffold(
        modifier = modifier.pointerInput(Unit) {
            var totalDragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onDragEnd = {
                    if (totalDragX < -120f) {
                        onTabSelected(BitChatNavTab.SETTINGS)
                    } else if (totalDragX > 120f) {
                        onTabSelected(BitChatNavTab.CALLS)
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    totalDragX += dragAmount
                }
            )
        },
        bottomBar = {
            BitChatBottomNavBar(
                currentTab = BitChatNavTab.QR_SCAN,
                onTabSelected = onTabSelected,
                isNightMode = isNightMode
            )
        },
        containerColor = if (activeQrTab == "SCAN") Color.Black else animBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeQrTab == "SCAN") {
                // ================== SCANNER MODE ==================
                if (hasCameraPermission) {
                    // CameraX Live Preview View
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val cameraExecutor = Executors.newSingleThreadExecutor()
                            var analyzedFrameCount = 0

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val reader = MultiFormatReader().apply {
                                    val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                                        put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
                                        put(DecodeHintType.TRY_HARDER, true)
                                    }
                                    setHints(hints)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setTargetResolution(android.util.Size(1280, 720))
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!isScanningActive) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    analyzedFrameCount++
                                    if (analyzedFrameCount == 1 || analyzedFrameCount % 30 == 0) {
                                        Log.d("KnotLinkQR", "Analyzer frame=$analyzedFrameCount format=${imageProxy.format} size=${imageProxy.width}x${imageProxy.height} rotation=${imageProxy.imageInfo.rotationDegrees}")
                                    }
                                    val qrResult = processImageProxy(imageProxy, reader)
                                    if (qrResult != null && isScanningActive) {
                                        Log.i("KnotLinkQR", "QR decoded payloadLength=${qrResult.length} payload=${qrResult.take(120)}")
                                        isScanningActive = false
                                        scope.launch(Dispatchers.Main) {
                                            handleQrCodeText(
                                                rawPayload = qrResult,
                                                viewModel = viewModel,
                                                onSuccess = { userResult ->
                                                    scannedUser = userResult
                                                },
                                                onError = { errorMsg ->
                                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(2000)
                                                        isScanningActive = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    imageProxy.close()
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Permission Request Fallback View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Required",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "KnotLink needs camera access to scan QR codes for adding contacts and starting chats.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(text = "Grant Camera Permission")
                        }
                    }
                }

                // Overlay & Corner Scanning Frame UI
                ScannerOverlayFrame(isScanningActive = isScanningActive)

            } else {
                // ================== MY QR CODE MODE ==================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isNightMode) 16.dp else 8.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = if (isNightMode) Color.Black else Color(0x1F000000),
                                spotColor = if (isNightMode) Color.Black else Color(0x1F000000)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNightMode) Color(0xFF161822) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isNightMode) Color(0xFF2E3348) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar with gradient border
                            // Render only the resolved avatar URL/path. Never pass an R2
                    // object key directly to Coil.
                    val avatarPath = resolvedAvatarPath
                            val hasAvatar = !avatarPath.isNullOrBlank()
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF8B5CF6))
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasAvatar) {
                                    val model: Any = if (avatarPath!!.startsWith("http://") || avatarPath.startsWith("https://") || avatarPath.startsWith("content://")) {
                                        avatarPath
                                    } else {
                                        val clean = avatarPath.removePrefix("file://")
                                        val f = File(clean)
                                        if (f.exists()) f else avatarPath
                                    }
                                    AsyncImage(
                                        model = model,
                                        contentDescription = "User Avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val initials = remember(user?.fullName, user?.username) {
                                        val name = user?.fullName?.ifBlank { user?.username } ?: "User"
                                        if (name.length >= 2) name.take(2).uppercase() else name.uppercase()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF2563EB), Color(0xFF8B5CF6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials.ifBlank { "U" },
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = user?.fullName?.ifBlank { user?.username } ?: "KnotLink User",
                                color = if (isNightMode) Color.White else Color(0xFF0F172A),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Sharp QR Code Display Container
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "My KnotLink QR Code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "Generating QR Code...",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Username shown below the QR. The UUID remains
                            // the internal/copyable identity, but is not exposed here.
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isNightMode) Color(0xFF212536) else Color(0xFFF1F5F9))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(displayUsername))
                                        isCopiedId = true
                                        Toast.makeText(context, "KnotLink username copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayUsername,
                                    color = if (isNightMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isCopiedId) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = if (isCopiedId) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row: Share QR & Copy Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("knotlink://user/$qrIdentifier"))
                                Toast.makeText(context, "KnotLink profile link copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNightMode) Color(0xFF212536) else Color(0xFFE2E8F0),
                                contentColor = if (isNightMode) Color.White else Color(0xFF0F172A)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Copy Link", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Connect with me on KnotLink!\n$displayUsername\nLink: knotlink://user/$qrIdentifier"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share KnotLink QR"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Share ID", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ================== TOP HEADER & TAB SWITCHER ==================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeQrTab == "SCAN") Color.Black.copy(alpha = 0.5f)
                                else if (isNightMode) Color(0xFF1E2130) else Color(0xFFE2E8F0)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (activeQrTab == "SCAN" || isNightMode) Color.White else Color(0xFF0F172A)
                        )
                    }

                    // Segmented Top Switcher (Scan QR / My QR Code)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (activeQrTab == "SCAN") Color.Black.copy(alpha = 0.6f)
                                else if (isNightMode) Color(0xFF1E2130) else Color(0xFFE2E8F0)
                            )
                            .border(
                                1.dp,
                                if (activeQrTab == "SCAN") Color.White.copy(alpha = 0.2f)
                                else if (isNightMode) Color(0xFF2E3348) else Color(0xFFCBD5E1),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // "Scan QR" pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (activeQrTab == "SCAN") Color(0xFF2563EB) else Color.Transparent
                                    )
                                    .clickable { activeQrTab = "SCAN" }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan",
                                        tint = if (activeQrTab == "SCAN") Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Scan",
                                        color = if (activeQrTab == "SCAN") Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // "My QR" pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (activeQrTab == "MY_QR") Color(0xFF2563EB) else Color.Transparent
                                    )
                                    .clickable { activeQrTab = "MY_QR" }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "My QR",
                                        tint = if (activeQrTab == "MY_QR") Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "My QR",
                                        color = if (activeQrTab == "MY_QR") Color.White else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Right button: Flashlight toggle in SCAN mode, or balanced spacer in MY_QR
                    if (activeQrTab == "SCAN") {
                        IconButton(
                            onClick = {
                                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                                    isTorchOn = !isTorchOn
                                    camera?.cameraControl?.enableTorch(isTorchOn)
                                } else {
                                    Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTorchOn) Color(0xFF2563EB) else Color.Black.copy(alpha = 0.5f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flashlight",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(42.dp))
                    }
                }
            }

            // Scanned User Profile Bottom Sheet
            val currentScannedUser = scannedUser
            if (currentScannedUser != null) {
                ScannedUserProfileSheet(
                    scannedUser = currentScannedUser,
                    isNightMode = isNightMode,
                    onDismiss = {
                        scannedUser = null
                        isScanningActive = true
                    },
                    onMessageClick = { userObj: ScannedUser ->
                        scope.launch {
                            try {
                                val newChat = viewModel.getOrCreateChatForScannedUser(userObj)
                                scannedUser = null
                                onChatCreated(newChat.id, newChat.name)
                            } catch (e: Exception) {
                                android.util.Log.e("QrScannerScreen", "Failed to start chat from QR: ${e.message}", e)
                            }
                        }
                    }
                )
            }

            // Manual Knot ID Input Dialog
            if (showManualInputDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showManualInputDialog = false
                        manualInputText = ""
                    },
                    title = {
                        Text(
                            text = "Enter KnotLink ID",
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) Color.White else Color(0xFF0F172A)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Enter a KnotLink user public ID to connect directly.",
                                color = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = manualInputText,
                                onValueChange = { manualInputText = it },
                                placeholder = { Text("e.g. KNOTLINK:USER:alex123 or ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = if (isNightMode) Color(0xFF2E3348) else Color(0xFFCBD5E1)
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (manualInputText.isNotBlank()) {
                                    val query = manualInputText.trim()
                                    showManualInputDialog = false
                                    manualInputText = ""
                                    scope.launch {
                                        handleQrCodeText(
                                            rawPayload = query,
                                            viewModel = viewModel,
                                            onSuccess = { userRes ->
                                                scannedUser = userRes
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Find User")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showManualInputDialog = false
                            manualInputText = ""
                        }) {
                            Text("Cancel", color = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    },
                    containerColor = if (isNightMode) Color(0xFF161822) else Color.White
                )
            }
        }
    }
}

@Composable
private fun ScannerOverlayFrame(isScanningActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "laserLine")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffsetY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val boxSizeDp = 270.dp
        val verticalOffsetDp = (-30).dp

        // Darkened Background with transparent square cutout & exact pixel-perfect blue outline border
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.65f))

            val boxSize = boxSizeDp.toPx()
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2f + verticalOffsetDp.toPx()

            // 1. Transparent Cutout
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // 2. Exact Blue Outline Border aligned with the cutout edge
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF2563EB),
                        Color(0xFF60A5FA),
                        Color(0xFF2563EB)
                    )
                ),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // Overlay Laser Line & Instruction Label aligned with the cutout
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = verticalOffsetDp)
            ) {
                Box(
                    modifier = Modifier.size(boxSizeDp)
                ) {
                    // Animated Scanning Laser Line
                    if (isScanningActive) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineY = size.height * laserOffsetY
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF38BDF8),
                                        Color(0xFF2563EB),
                                        Color(0xFF38BDF8),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(16.dp.toPx(), lineY),
                                end = Offset(size.width - 16.dp.toPx(), lineY),
                                strokeWidth = 3.5.dp.toPx()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanning",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Align KnotLink QR code within frame",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun processImageProxy(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    if (imageProxy.format != ImageFormat.YUV_420_888) {
        Log.w("KnotLinkQR", "Unsupported image format=${imageProxy.format}")
        return null
    }

    val plane = imageProxy.planes.firstOrNull() ?: return null
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val buffer = plane.buffer.duplicate()
    val luma = ByteArray(width * height)
    for (y in 0 until height) {
        val rowStart = y * rowStride
        for (x in 0 until width) {
            val index = rowStart + x * pixelStride
            if (index < buffer.limit()) luma[y * width + x] = buffer.get(index)
        }
    }

    fun decodeWithBinarizer(data: ByteArray, dataWidth: Int, dataHeight: Int, hybrid: Boolean): String? {
        val source = PlanarYUVLuminanceSource(data, dataWidth, dataHeight, 0, 0, dataWidth, dataHeight, false)
        val bitmap = if (hybrid) BinaryBitmap(HybridBinarizer(source))
        else BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
        return try { reader.decodeWithState(bitmap).text }
        catch (_: Exception) { null }
        finally { reader.reset() }
    }

    fun decode(data: ByteArray, dataWidth: Int, dataHeight: Int): String? {
        decodeWithBinarizer(data, dataWidth, dataHeight, true)?.let { return it }
        return decodeWithBinarizer(data, dataWidth, dataHeight, false)
    }

    decode(luma, width, height)?.let { return it }

    val rotation = imageProxy.imageInfo.rotationDegrees
    if (rotation == 90 || rotation == 270) {
        val rotated = ByteArray(width * height)
        if (rotation == 90) {
            for (y in 0 until height) for (x in 0 until width) {
                rotated[x * height + (height - 1 - y)] = luma[y * width + x]
            }
        } else {
            for (y in 0 until height) for (x in 0 until width) {
                rotated[(width - 1 - x) * height + y] = luma[y * width + x]
            }
        }
        decode(rotated, height, width)?.let { return it }
    } else if (rotation == 180) {
        val rotated = ByteArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            rotated[(height - 1 - y) * width + (width - 1 - x)] = luma[y * width + x]
        }
        decode(rotated, width, height)?.let { return it }
    }
    return null
}
private suspend fun handleQrCodeText(
    rawPayload: String,
    viewModel: BitChatViewModel,
    onSuccess: (ScannedUser) -> Unit,
    onError: (String) -> Unit
) {
    val result = withContext(Dispatchers.IO) {
        viewModel.resolveScannedUser(rawPayload)
    }

    when (result) {
        is ScannedUserResult.Success -> onSuccess(result.user)
        is ScannedUserResult.InvalidQr -> onError("Invalid KnotLink QR Code")
        is ScannedUserResult.UserNotFound -> onError("KnotLink user not found")
    }
}
