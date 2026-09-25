package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Realtime Square Video Note Recorder
 * Displays real front/back camera feed in a stylish rounded square window with live recording timer,
 * animated gradient border, instant camera flip, and direct send/cancel controls.
 */
@Composable
fun TelegramRoundVideoRecorder(
    isNightMode: Boolean,
    onVideoRecorded: (File, Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val camGranted = perms[Manifest.permission.CAMERA] == true
        val audioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
        hasCameraPermission = camGranted && audioGranted
        if (!hasCameraPermission) {
            Toast.makeText(context, "Camera and Audio permissions are required for video notes", Toast.LENGTH_SHORT).show()
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var recordedVideoFile by remember { mutableStateOf<File?>(null) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var isRecordingStarted by remember { mutableStateOf(false) }

    // Recording seconds timer
    LaunchedEffect(isRecordingStarted) {
        if (isRecordingStarted) {
            recordSeconds = 0
            while (isRecordingStarted) {
                delay(1000L)
                recordSeconds++
            }
        }
    }

    // Border rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "videoRing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeRecording?.stop()
            } catch (_: Exception) {}
            activeRecording = null
        }
    }

    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Modern Rounded Square Camera Preview Box (Size: 270.dp x 270.dp)
        Box(
            modifier = Modifier
                .size(270.dp)
                .clip(shape)
                .background(Color.Black)
                .border(
                    width = 3.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF2563EB),
                            Color(0xFF8B5CF6),
                            Color(0xFFEC4899),
                            Color(0xFF10B981),
                            Color(0xFF2563EB)
                        )
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                key(cameraSelector) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val recorder = Recorder.Builder()
                                        .setQualitySelector(QualitySelector.from(Quality.SD))
                                        .build()
                                    val videoCapture = VideoCapture.withOutput(recorder)

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        videoCapture
                                    )

                                    val outputFile = File(ctx.cacheDir, "video_note_${System.currentTimeMillis()}.mp4")
                                    recordedVideoFile = outputFile
                                    val outputOptions = FileOutputOptions.Builder(outputFile).build()

                                    val recording = videoCapture.output
                                        .prepareRecording(ctx, outputOptions)
                                        .apply {
                                            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                withAudioEnabled()
                                            }
                                        }
                                        .start(ContextCompat.getMainExecutor(ctx)) { event ->
                                            when (event) {
                                                is VideoRecordEvent.Start -> {
                                                    isRecordingStarted = true
                                                    Log.d("TelegramVideo", "Recording started")
                                                }
                                                is VideoRecordEvent.Finalize -> {
                                                    isRecordingStarted = false
                                                    if (event.hasError()) {
                                                        Log.e("TelegramVideo", "Recording error: ${event.error}")
                                                    }
                                                }
                                            }
                                        }
                                    activeRecording = recording
                                } catch (e: Exception) {
                                    Log.e("TelegramVideo", "Camera binding failed: ${e.message}", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Live Timer & Status Overlay at Top
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val mins = recordSeconds / 60
                    val secs = recordSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", mins, secs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Flip Camera Button at Bottom Center
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        try {
                            activeRecording?.stop()
                        } catch (_: Exception) {}
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Controls (Cancel / Send)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel Button
            Surface(
                shape = CircleShape,
                color = if (isNightMode) Color(0xFF2D313E) else Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        try {
                            activeRecording?.stop()
                        } catch (_: Exception) {}
                        recordedVideoFile?.delete()
                        onCancel()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Recording",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Send Video Note Button
            Surface(
                shape = CircleShape,
                color = Color(0xFF2563EB),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable {
                        val duration = recordSeconds
                        if (duration < 1) {
                            Toast.makeText(context, "Video note must be at least 1 second ⏱️", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        try {
                            activeRecording?.stop()
                        } catch (_: Exception) {}

                        coroutineScope.launch {
                            delay(300L) // Wait for finalize flush
                            val file = recordedVideoFile
                            if (file != null && file.exists()) {
                                onVideoRecorded(file, duration)
                            } else {
                                val (recAudio, audSecs) = com.example.util.AudioRecorderManager.stopRecording()
                                if (recAudio != null && recAudio.exists()) {
                                    onVideoRecorded(recAudio, audSecs)
                                } else {
                                    Toast.makeText(context, "Failed to save video note", Toast.LENGTH_SHORT).show()
                                    onCancel()
                                }
                            }
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Video Note",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
