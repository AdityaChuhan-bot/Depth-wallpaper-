package com.example.ui.screens.segment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.segmentation.MaskRefiner
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.viewmodel.DepthLockViewModel

enum class SegmentViewMode {
    CUTOUT,   // Foreground isolated
    MASK,     // Grayscale alpha mask
    OVERLAY   // Red semi-transparent overlay on original
}

enum class BrushMode {
    PAINT_FG, // Add to subject
    ERASE_FG  // Erase from subject
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthSegmentScreen(
    viewModel: DepthLockViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val croppedBitmap by viewModel.croppedBitmap.collectAsStateWithLifecycle()
    val maskBitmap by viewModel.maskBitmap.collectAsStateWithLifecycle()
    val foregroundCutout by viewModel.foregroundCutout.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processingStatus by viewModel.processingStatus.collectAsStateWithLifecycle()
    val subjectType by viewModel.detectedSubjectType.collectAsStateWithLifecycle()

    var viewMode by remember { mutableStateOf(SegmentViewMode.CUTOUT) }
    var brushMode by remember { mutableStateOf(BrushMode.PAINT_FG) }
    var brushRadius by remember { mutableFloatStateOf(24f) }
    var maskRevision by remember { mutableIntStateOf(0) }

    var refiner by remember { mutableStateOf<MaskRefiner?>(null) }

    LaunchedEffect(maskBitmap) {
        if (maskBitmap != null && refiner == null) {
            refiner = MaskRefiner(maskBitmap!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            refiner?.destroy()
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Depth Separation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Detected: $subjectType",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
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
                            viewModel.startSegmentation()
                            refiner = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry AI",
                            tint = IndigoLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        if (isProcessing) {
            // Processing dialog / state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = IndigoPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = processingStatus.ifEmpty { "Separating subject & background..." },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Running local on-device neural network (zero cloud transfer)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Cancel")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // View Mode Tabs
                TabRow(
                    selectedTabIndex = viewMode.ordinal,
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[viewMode.ordinal]),
                            color = IndigoPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = viewMode == SegmentViewMode.CUTOUT,
                        onClick = { viewMode = SegmentViewMode.CUTOUT },
                        text = { Text("Foreground Cutout", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = viewMode == SegmentViewMode.MASK,
                        onClick = { viewMode = SegmentViewMode.MASK },
                        text = { Text("Depth Mask", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = viewMode == SegmentViewMode.OVERLAY,
                        onClick = { viewMode = SegmentViewMode.OVERLAY },
                        text = { Text("Mask Overlay", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canvas Container: Interactive Mask Refinement
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val frameH = maxHeight
                    val frameW = frameH * (9f / 20f)
                    val activeMask = refiner?.getMask() ?: maskBitmap

                    Box(
                        modifier = Modifier
                            .size(frameW, frameH)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp))
                            .background(Color(0xFF030712))
                            .pointerInput(brushMode, brushRadius) {
                                var lastPoint: Offset? = null
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        refiner?.pushHistory()
                                        val mask = refiner?.getMask() ?: return@detectDragGestures
                                        val scaleX = mask.width / size.width.toFloat()
                                        val scaleY = mask.height / size.height.toFloat()
                                        val maskX = offset.x * scaleX
                                        val maskY = offset.y * scaleY
                                        lastPoint = Offset(maskX, maskY)

                                        refiner?.applyStroke(
                                            fromX = maskX,
                                            fromY = maskY,
                                            toX = maskX,
                                            toY = maskY,
                                            brushRadius = brushRadius * scaleX,
                                            isEraser = brushMode == BrushMode.ERASE_FG
                                        )
                                        maskRevision++
                                    },
                                    onDrag = { change, _ ->
                                        val mask = refiner?.getMask() ?: return@detectDragGestures
                                        val scaleX = mask.width / size.width.toFloat()
                                        val scaleY = mask.height / size.height.toFloat()
                                        val currentPoint = Offset(change.position.x * scaleX, change.position.y * scaleY)

                                        lastPoint?.let { start ->
                                            refiner?.applyStroke(
                                                fromX = start.x,
                                                fromY = start.y,
                                                toX = currentPoint.x,
                                                toY = currentPoint.y,
                                                brushRadius = brushRadius * scaleX,
                                                isEraser = brushMode == BrushMode.ERASE_FG
                                            )
                                        }
                                        lastPoint = currentPoint
                                        maskRevision++
                                    },
                                    onDragEnd = {
                                        lastPoint = null
                                        refiner?.getMask()?.let { updated ->
                                            viewModel.onManualMaskRefined(updated)
                                        }
                                    }
                                )
                            }
                    ) {
                        // Suppress unused warning via reading maskRevision
                        if (maskRevision >= 0 && activeMask != null) {
                            when (viewMode) {
                                SegmentViewMode.CUTOUT -> {
                                    if (foregroundCutout != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = foregroundCutout!!.asImageBitmap(),
                                            contentDescription = "Foreground Cutout",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                SegmentViewMode.MASK -> {
                                    androidx.compose.foundation.Image(
                                        bitmap = activeMask.asImageBitmap(),
                                        contentDescription = "Depth Mask",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                SegmentViewMode.OVERLAY -> {
                                    if (croppedBitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = croppedBitmap!!.asImageBitmap(),
                                            contentDescription = "Base Photo",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Red overlay on mask
                                        androidx.compose.foundation.Image(
                                            bitmap = activeMask.asImageBitmap(),
                                            contentDescription = "Overlay Mask",
                                            alpha = 0.5f,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Touch Brush Hint overlay
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = if (brushMode == BrushMode.PAINT_FG) "Touch to paint subject" else "Touch to erase background",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brush Tools Toolbar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = brushMode == BrushMode.PAINT_FG,
                                    onClick = { brushMode = BrushMode.PAINT_FG },
                                    label = { Text("Paint") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )

                                FilterChip(
                                    selected = brushMode == BrushMode.ERASE_FG,
                                    onClick = { brushMode = BrushMode.ERASE_FG },
                                    label = { Text("Eraser") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoseAccent,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        refiner?.undo()?.let {
                                            viewModel.onManualMaskRefined(it)
                                            maskRevision++
                                        }
                                    },
                                    enabled = refiner?.canUndo() == true
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo",
                                        tint = if (refiner?.canUndo() == true) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        refiner?.redo()?.let {
                                            viewModel.onManualMaskRefined(it)
                                            maskRevision++
                                        }
                                    },
                                    enabled = refiner?.canRedo() == true
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Redo",
                                        tint = if (refiner?.canRedo() == true) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        refiner?.invertMask()?.let {
                                            viewModel.onManualMaskRefined(it)
                                            maskRevision++
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = "Invert Mask",
                                        tint = CyanAccent
                                    )
                                }
                            }
                        }

                        // Brush size slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Brush: ${brushRadius.toInt()}px",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(76.dp)
                            )
                            Slider(
                                value = brushRadius,
                                onValueChange = { brushRadius = it },
                                valueRange = 8f..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = IndigoPrimary,
                                    activeTrackColor = IndigoLight
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Continue to Editor Button
                Button(
                    onClick = {
                        refiner?.getMask()?.let { m ->
                            viewModel.onManualMaskRefined(m)
                        }
                        onNavigateToEditor()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("apply_and_open_editor_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Confirm Subject & Open Editor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
