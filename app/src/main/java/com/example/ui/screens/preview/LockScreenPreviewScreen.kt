package com.example.ui.screens.preview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.editor.EditorClockWidget
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.DepthLockViewModel
import kotlinx.coroutines.delay

@Composable
fun LockScreenPreviewScreen(
    viewModel: DepthLockViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBitmap by viewModel.backgroundBitmap.collectAsStateWithLifecycle()
    val foregroundCutout by viewModel.foregroundCutout.collectAsStateWithLifecycle()
    val config by viewModel.currentConfig.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(true) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Ticking clock effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Fullscreen Canvas Area
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val canvasH = maxHeight
            val canvasW = maxWidth

            // 1. Background Layer
            if (backgroundBitmap != null) {
                val blurMod = if (config.bgBlurRadius > 0.5f) {
                    Modifier.blur(config.bgBlurRadius.dp)
                } else {
                    Modifier
                }

                androidx.compose.foundation.Image(
                    bitmap = backgroundBitmap!!.asImageBitmap(),
                    contentDescription = "Lock Screen Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .then(blurMod)
                )

                if (config.bgDimPercent > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = config.bgDimPercent))
                    )
                }
            }

            // 2. Depth Clock & Widgets (Layer behind subject when depth is on)
            if (config.depthOverlapEnabled) {
                EditorClockWidget(
                    config = config,
                    canvasHeight = canvasH.value,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Foreground Subject Layer
            if (foregroundCutout != null) {
                if (config.depthOverlapEnabled && config.depthShadowIntensity > 0.05f) {
                    androidx.compose.foundation.Image(
                        bitmap = foregroundCutout!!.asImageBitmap(),
                        contentDescription = "Subject Shadow",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = config.subjectScale,
                                scaleY = config.subjectScale,
                                translationX = config.subjectOffsetX + 6f,
                                translationY = config.subjectOffsetY + 12f,
                                alpha = config.depthShadowIntensity * 0.75f
                            )
                            .blur(18.dp)
                    )
                }

                androidx.compose.foundation.Image(
                    bitmap = foregroundCutout!!.asImageBitmap(),
                    contentDescription = "Foreground Subject Overlap",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = config.subjectScale,
                            scaleY = config.subjectScale,
                            translationX = config.subjectOffsetX,
                            translationY = config.subjectOffsetY
                        )
                )
            }

            // Clock on top if depth is disabled
            if (!config.depthOverlapEnabled) {
                EditorClockWidget(
                    config = config,
                    canvasHeight = canvasH.value,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Lock Screen UI Header: Padlock icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Icon",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom System Lock Screen Elements: Flashlight, Swipe indicator, Camera
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Swipe up to unlock bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Swipe up to unlock",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight button simulation
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = "Flashlight",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Bottom Home Indicator Bar
                    Box(
                        modifier = Modifier
                            .size(130.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.8f))
                    )

                    // Camera button simulation
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Overlay Banner & Exit Button
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("exit_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Preview",
                            tint = Color.White
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulated Lock Screen Preview",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Tap to hide hint
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Tap screen to toggle preview controls",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
