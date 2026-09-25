package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BitSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * ImageCropDialog provides a full-featured interactive photo cropper.
 * Uses EvenOdd Path mask so the photo is crystal-clear in the circle viewport.
 */
@Composable
fun ImageCropDialog(
    imageSource: Any?,
    onDismiss: () -> Unit,
    onCropSuccess: (croppedFilePath: String) -> Unit,
    isCircularMask: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    var isProcessingCrop by remember { mutableStateOf(false) }

    // Transformation states
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Display box calculations for saving
    var currentCropBoxPx by remember { mutableFloatStateOf(0f) }

    // Load and prepare bitmap on launch
    LaunchedEffect(imageSource) {
        isLoadingImage = true
        withContext(Dispatchers.IO) {
            try {
                val loaded = loadBitmapWithCorrectOrientation(context, imageSource)
                sourceBitmap = loaded
            } catch (e: Exception) {
                Log.e("ImageCropDialog", "Failed to load image for cropping", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not load image: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            } finally {
                isLoadingImage = false
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessingCrop) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessingCrop,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0D14))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (!isProcessingCrop) onDismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Crop Profile Photo",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scale = 1f
                            panOffset = Offset.Zero
                            rotationDegrees = 0
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Interactive Crop Viewport (Takes all remaining flexible space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingImage) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = BitSecondary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading photo...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                } else if (sourceBitmap != null) {
                    val currentBmp = sourceBitmap!!

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val containerWidthPx = constraints.maxWidth.toFloat()
                        val containerHeightPx = constraints.maxHeight.toFloat()
                        val cropBoxSizePx = min(containerWidthPx * 0.80f, containerHeightPx * 0.78f)
                        currentCropBoxPx = cropBoxSizePx

                            // Gesture area
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.8f, 5.0f)
                                            panOffset += pan
                                        }
                                    }
                            ) {
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val canvasCenter = Offset(size.width / 2f, size.height / 2f)

                                    // 1. Draw the transformed bitmap underneath
                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(canvasCenter.x + panOffset.x, canvasCenter.y + panOffset.y)
                                    drawContext.canvas.rotate(rotationDegrees.toFloat())
                                    drawContext.canvas.scale(scale, scale)

                                    val bmpWidth = currentBmp.width.toFloat()
                                    val bmpHeight = currentBmp.height.toFloat()

                                    // Fill the crop box nicely
                                    val baseFitScale = max(cropBoxSizePx / bmpWidth, cropBoxSizePx / bmpHeight)
                                    val drawW = bmpWidth * baseFitScale
                                    val drawH = bmpHeight * baseFitScale

                                    drawImage(
                                        image = currentBmp.asImageBitmap(),
                                        dstOffset = IntOffset(
                                            (-drawW / 2).toInt(),
                                            (-drawH / 2).toInt()
                                        ),
                                        dstSize = IntSize(
                                            drawW.toInt(),
                                            drawH.toInt()
                                        )
                                    )
                                    drawContext.canvas.restore()

                                    // 2. Draw dark semi-transparent mask OUTSIDE the crop circle using EvenOdd Path
                                    val cropRadius = cropBoxSizePx / 2f
                                    val maskPath = Path().apply {
                                        fillType = PathFillType.EvenOdd
                                        // Outer fullscreen rectangle
                                        addRect(Rect(0f, 0f, size.width, size.height))
                                        // Inner cutout (Circle or RoundRect)
                                        if (isCircularMask) {
                                            addOval(
                                                Rect(
                                                    canvasCenter.x - cropRadius,
                                                    canvasCenter.y - cropRadius,
                                                    canvasCenter.x + cropRadius,
                                                    canvasCenter.y + cropRadius
                                                )
                                            )
                                        } else {
                                            addRoundRect(
                                                RoundRect(
                                                    left = canvasCenter.x - cropRadius,
                                                    top = canvasCenter.y - cropRadius,
                                                    right = canvasCenter.x + cropRadius,
                                                    bottom = canvasCenter.y + cropRadius,
                                                    radiusX = 24.dp.toPx(),
                                                    radiusY = 24.dp.toPx()
                                                )
                                            )
                                        }
                                    }
                                    drawPath(
                                        path = maskPath,
                                        color = Color.Black.copy(alpha = 0.72f)
                                    )

                                    // 3. Draw boundary guide rings
                                    if (isCircularMask) {
                                        drawCircle(
                                            color = Color(0xFF38BDF8),
                                            radius = cropRadius,
                                            center = canvasCenter,
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.35f),
                                            radius = cropRadius - 1.5.dp.toPx(),
                                            center = canvasCenter,
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    } else {
                                        drawRoundRect(
                                            color = Color(0xFF38BDF8),
                                            topLeft = Offset(canvasCenter.x - cropRadius, canvasCenter.y - cropRadius),
                                            size = Size(cropBoxSizePx, cropBoxSizePx),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )
                                    }

                                    // 4. Draw 3x3 Rule-of-thirds grid inside crop circle
                                    val left = canvasCenter.x - cropRadius
                                    val top = canvasCenter.y - cropRadius
                                    val step = cropBoxSizePx / 3f

                                    for (i in 1..2) {
                                        val lineX = left + step * i
                                        val lineY = top + step * i

                                        // Vertical grid line
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.22f),
                                            start = Offset(lineX, top),
                                            end = Offset(lineX, top + cropBoxSizePx),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        // Horizontal grid line
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.22f),
                                            start = Offset(left, lineY),
                                            end = Offset(left + cropBoxSizePx, lineY),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Pinned Bottom Control Panel (Pinned cleanly at the bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF13141E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom Slider Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.2f).coerceAtLeast(0.8f) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.8f..4.5f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF2563EB),
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    IconButton(
                        onClick = { scale = (scale + 0.2f).coerceAtMost(4.5f) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons Row: [ Rotate 90° ] & [ Apply Crop Button ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate 90° button
                    OutlinedButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rotate",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Apply Crop Button
                    Button(
                        onClick = {
                            if (sourceBitmap == null || isProcessingCrop) return@Button
                            isProcessingCrop = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val croppedPath = cropAndSaveBitmap(
                                        context = context,
                                        source = sourceBitmap!!,
                                        scale = scale,
                                        panOffset = panOffset,
                                        rotationDegrees = rotationDegrees,
                                        cropBoxPx = currentCropBoxPx
                                    )
                                    withContext(Dispatchers.Main) {
                                        isProcessingCrop = false
                                        if (croppedPath != null) {
                                            onCropSuccess(croppedPath)
                                        } else {
                                            Toast.makeText(context, "Could not crop image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ImageCropDialog", "Crop execution error", e)
                                    withContext(Dispatchers.Main) {
                                        isProcessingCrop = false
                                        Toast.makeText(context, "Crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isProcessingCrop && sourceBitmap != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isProcessingCrop) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cropping...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Crop",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Set Profile Photo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Loads a bitmap from File, Uri or path, applying EXIF rotation and downsampling if necessary.
 */
private suspend fun loadBitmapWithCorrectOrientation(context: Context, source: Any?): Bitmap? = withContext(Dispatchers.IO) {
    if (source == null) return@withContext null

    var inputStream: InputStream? = null
    var orientation = ExifInterface.ORIENTATION_NORMAL

    try {
        when (source) {
            is File -> {
                try {
                    val exif = ExifInterface(source.absolutePath)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } catch (e: Exception) {
                    Log.w("ImageCrop", "EXIF read error: ${e.message}")
                }
                inputStream = source.inputStream()
            }
            is String -> {
                if (source.startsWith("content://") || source.startsWith("file://")) {
                    val uri = Uri.parse(source)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val exif = ExifInterface(stream)
                            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        }
                    } catch (e: Exception) {
                        Log.w("ImageCrop", "EXIF read error: ${e.message}")
                    }
                    inputStream = context.contentResolver.openInputStream(uri)
                } else {
                    val f = File(source)
                    try {
                        val exif = ExifInterface(f.absolutePath)
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    } catch (e: Exception) {
                        Log.w("ImageCrop", "EXIF read error: ${e.message}")
                    }
                    inputStream = f.inputStream()
                }
            }
            is Uri -> {
                try {
                    context.contentResolver.openInputStream(source)?.use { stream ->
                        val exif = ExifInterface(stream)
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    }
                } catch (e: Exception) {
                    Log.w("ImageCrop", "EXIF read error: ${e.message}")
                }
                inputStream = context.contentResolver.openInputStream(source)
            }
        }

        if (inputStream == null) return@withContext null

        val bytes = inputStream.use { it.readBytes() }
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        var inSampleSize = 1
        val maxDim = 2048
        while (options.outWidth / inSampleSize > maxDim || options.outHeight / inSampleSize > maxDim) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return@withContext null

        val exifRotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (exifRotation != 0f) {
            val matrix = Matrix().apply { postRotate(exifRotation) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) decoded.recycle()
            return@withContext rotated
        }

        return@withContext decoded
    } catch (e: Exception) {
        Log.e("ImageCrop", "Error loading bitmap", e)
        return@withContext null
    }
}

/**
 * Performs actual bitmap crop and transformation matching the screen viewport and saves it to a file.
 */
private suspend fun cropAndSaveBitmap(
    context: Context,
    source: Bitmap,
    scale: Float,
    panOffset: Offset,
    rotationDegrees: Int,
    cropBoxPx: Float
): String? = withContext(Dispatchers.IO) {
    try {
        val targetSize = 600 // Output square avatar resolution

        // Apply rotation
        val rotatedSource = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val bmpW = rotatedSource.width.toFloat()
        val bmpH = rotatedSource.height.toFloat()

        val activeCropBox = if (cropBoxPx > 0f) cropBoxPx else min(bmpW, bmpH)
        val baseFitScale = max(activeCropBox / bmpW, activeCropBox / bmpH)
        val effectiveScale = max(0.1f, scale * baseFitScale)

        // The size of crop window in original bitmap pixels
        val cropWindowDimInBmp = activeCropBox / effectiveScale

        // Center of the crop relative to bitmap center
        val centerOffsetXInBmp = panOffset.x / effectiveScale
        val centerOffsetY = panOffset.y / effectiveScale

        var cropLeft = (bmpW / 2f) - centerOffsetXInBmp - (cropWindowDimInBmp / 2f)
        var cropTop = (bmpH / 2f) - centerOffsetY - (cropWindowDimInBmp / 2f)

        // Clamp to bitmap boundaries
        if (cropLeft < 0f) cropLeft = 0f
        if (cropTop < 0f) cropTop = 0f
        if (cropLeft + cropWindowDimInBmp > bmpW) cropLeft = max(0f, bmpW - cropWindowDimInBmp)
        if (cropTop + cropWindowDimInBmp > bmpH) cropTop = max(0f, bmpH - cropWindowDimInBmp)

        val cropWidth = min(cropWindowDimInBmp, bmpW - cropLeft).toInt().coerceAtLeast(1)
        val cropHeight = min(cropWindowDimInBmp, bmpH - cropTop).toInt().coerceAtLeast(1)
        val finalCropDim = min(cropWidth, cropHeight)

        val croppedBitmap = Bitmap.createBitmap(
            rotatedSource,
            cropLeft.toInt().coerceIn(0, max(0, rotatedSource.width - finalCropDim)),
            cropTop.toInt().coerceIn(0, max(0, rotatedSource.height - finalCropDim)),
            finalCropDim,
            finalCropDim
        )

        // Scale to clean profile photo size
        val scaledOutput = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true)

        val outputFile = File(context.cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            scaledOutput.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }

        if (rotatedSource != source) rotatedSource.recycle()
        if (croppedBitmap != scaledOutput) croppedBitmap.recycle()
        scaledOutput.recycle()

        return@withContext outputFile.absolutePath
    } catch (e: Exception) {
        Log.e("ImageCrop", "Failed to crop and save bitmap", e)
        return@withContext null
    }
}
