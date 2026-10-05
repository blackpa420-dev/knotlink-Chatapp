package com.example.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfilePhotoCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (Uri) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    var isProcessing by remember { mutableStateOf(false) }

    val viewportSizeDp = 240.dp
    val viewportSizePx = with(density) { viewportSizeDp.toPx().toInt() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = !isProcessing
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0F172A)),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Crop Profile Photo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Drag & zoom photo to fit inside circle",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Crop Viewport Frame with Dark Cutout Overlay
                Box(
                    modifier = Modifier
                        .size(viewportSizeDp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(width = 3.dp, color = Color(0xFF3B82F6), shape = CircleShape)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.8f, 4.0f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Raw Transformable Image Layer
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Cropping Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                rotationZ = rotationAngle,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay Circular Viewport Grid Lines & Rule of Thirds
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val lineW = 1.dp.toPx()
                        val gridColor = Color.White.copy(alpha = 0.25f)

                        // Rule of Thirds Grid Lines
                        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), lineW)
                        drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), lineW)
                        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), lineW)
                        drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), lineW)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Adjust Toolbar: Zoom Out / Rotate / Reset / Zoom In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.25f).coerceAtLeast(0.8f) },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            scale = 1.0f
                            offsetX = 0f
                            offsetY = 0f
                            rotationAngle = 0f
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { scale = (scale + 0.25f).coerceAtMost(4.0f) },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action CTA Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                val croppedUri = withContext(Dispatchers.IO) {
                                    cropBitmapFromUri(
                                        context = context,
                                        imageUri = imageUri,
                                        viewportSizePx = viewportSizePx,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        rotationDegrees = rotationAngle
                                    )
                                }
                                isProcessing = false
                                if (croppedUri != null) {
                                    onCropConfirmed(croppedUri)
                                } else {
                                    // Fallback to original image uri if bitmap crop failed
                                    onCropConfirmed(imageUri)
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Crop & Save",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Perform real Android Bitmap cropping based on viewport, transform matrix, scale, and pan offsets.
 */
private fun cropBitmapFromUri(
    context: android.content.Context,
    imageUri: Uri,
    viewportSizePx: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDegrees: Float
): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        var originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (originalBitmap == null) return null

        // Apply rotation if needed
        if (rotationDegrees != 0f) {
            val matrix = android.graphics.Matrix().apply {
                postRotate(rotationDegrees)
            }
            val rotated = android.graphics.Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
            )
            if (rotated != originalBitmap) {
                originalBitmap.recycle()
                originalBitmap = rotated
            }
        }

        val origW = originalBitmap.width
        val origH = originalBitmap.height

        // Calculate aspect fill scale
        val baseScale = maxOf(viewportSizePx.toFloat() / origW, viewportSizePx.toFloat() / origH)
        val totalScale = baseScale * scale

        val scaledW = origW * totalScale
        val scaledH = origH * totalScale

        // Center position + user pan offset
        val centerX = (scaledW / 2f) - offsetX
        val centerY = (scaledH / 2f) - offsetY

        // Crop rect in scaled coordinate space
        val leftScaled = (centerX - viewportSizePx / 2f).coerceIn(0f, maxOf(0f, scaledW - viewportSizePx))
        val topScaled = (centerY - viewportSizePx / 2f).coerceIn(0f, maxOf(0f, scaledH - viewportSizePx))

        // Convert back to original bitmap pixel coordinates
        val cropX = (leftScaled / totalScale).toInt().coerceIn(0, origW - 1)
        val cropY = (topScaled / totalScale).toInt().coerceIn(0, origH - 1)
        val cropW = (viewportSizePx / totalScale).toInt().coerceAtMost(origW - cropX).coerceAtLeast(1)
        val cropH = (viewportSizePx / totalScale).toInt().coerceAtMost(origH - cropY).coerceAtLeast(1)

        val cropSquareSize = minOf(cropW, cropH)

        val croppedBitmap = android.graphics.Bitmap.createBitmap(
            originalBitmap,
            cropX,
            cropY,
            cropSquareSize,
            cropSquareSize
        )

        // Scale to a crisp 500x500 output avatar bitmap
        val outputAvatar = android.graphics.Bitmap.createScaledBitmap(croppedBitmap, 500, 500, true)

        val cacheFile = java.io.File(context.cacheDir, "cropped_avatar_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(cacheFile).use { out ->
            outputAvatar.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
        }

        if (originalBitmap != outputAvatar) originalBitmap.recycle()
        if (croppedBitmap != outputAvatar) croppedBitmap.recycle()

        Uri.fromFile(cacheFile)
    } catch (e: Exception) {
        Log.e("ProfilePhotoCropDialog", "Error cropping bitmap: ${e.message}", e)
        null
    }
}
