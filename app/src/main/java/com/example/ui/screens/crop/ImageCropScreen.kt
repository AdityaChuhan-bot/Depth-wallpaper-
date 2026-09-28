package com.example.ui.screens.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.DepthLockViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    viewModel: DepthLockViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSegment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originalBitmap by viewModel.originalBitmap.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    // Aspect ratio: Moto G85 5G is 20:9 (0.45), standard phone is 9:16 (0.5625), or Free
    var selectedAspectRatio by remember { mutableStateOf(20f / 9f) } // height / width = 20/9 -> aspect = 9/20
    val targetAspect = 9f / 20f // 0.45 portrait

    BackHandler {
        onNavigateBack()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            rotationDegrees = 0f
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crop & Frame Photo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Replace Photo",
                            tint = CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position your subject inside the phone lock screen frame. Pinch to zoom, drag to align.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Framing Container
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val frameHeight = maxHeight
                val frameWidth = frameHeight * targetAspect

                Box(
                    modifier = Modifier
                        .size(frameWidth, frameHeight)
                        .clip(RoundedCornerShape(28.dp))
                        .border(2.dp, IndigoPrimary, RoundedCornerShape(28.dp))
                        .background(Color.Black)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (originalBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = originalBitmap!!.asImageBitmap(),
                            contentDescription = "Original Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                    rotationZ = rotationDegrees
                                )
                        )

                        // Subtle clock placement guide to show user where the clock will sit!
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 40.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "10:08 (Depth Overlap Zone)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = IndigoPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transform quick controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Rotate 90°", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        rotationDegrees = 0f
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reset", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Continue to AI Segmentation
            Button(
                onClick = {
                    val src = originalBitmap ?: return@Button
                    val cropped = renderCroppedBitmap(
                        src = src,
                        scale = scale,
                        transX = offsetX,
                        transY = offsetY,
                        rotation = rotationDegrees,
                        targetWidth = 720,
                        targetHeight = 1600
                    )
                    viewModel.onCropCompleted(cropped)
                    onNavigateToSegment()
                },
                enabled = originalBitmap != null && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_to_segment_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndigoPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "Next: AI Depth Separation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

/**
 * Creates a high quality cropped bitmap based on user scale, pan, and rotation transforms.
 */
fun renderCroppedBitmap(
    src: Bitmap,
    scale: Float,
    transX: Float,
    transY: Float,
    rotation: Float,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val matrix = Matrix()
    // Center the source image onto canvas
    val initialScale = max(targetWidth.toFloat() / src.width, targetHeight.toFloat() / src.height)
    val scaledSrcW = src.width * initialScale
    val scaledSrcH = src.height * initialScale

    matrix.postScale(initialScale, initialScale)
    matrix.postTranslate((targetWidth - scaledSrcW) / 2f, (targetHeight - scaledSrcH) / 2f)

    // User transformations
    matrix.postTranslate(transX, transY)
    matrix.postScale(scale, scale, targetWidth / 2f, targetHeight / 2f)
    matrix.postRotate(rotation, targetWidth / 2f, targetHeight / 2f)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(src, matrix, paint)
    return result
}
