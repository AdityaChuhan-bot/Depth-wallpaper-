package com.example.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.compositor.ClockStyles
import com.example.domain.wallpaper.WallpaperTarget
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EditorTab {
    CLOCK,
    DEPTH,
    BACKGROUND,
    DATE_WIDGET
}

val ClockColorPalette = listOf(
    "#FFFFFF" to "White",
    "#E0F2FE" to "Ice Blue",
    "#C7D2FE" to "Lavender",
    "#FFE4E6" to "Soft Rose",
    "#FEF3C7" to "Warm Gold",
    "#D1FAE5" to "Mint",
    "#F43F5E" to "Cyber Red",
    "#38BDF8" to "Sky Cyan",
    "#1E293B" to "Obsidian"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthEditorScreen(
    viewModel: DepthLockViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSegment: () -> Unit,
    onNavigateToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBitmap by viewModel.backgroundBitmap.collectAsStateWithLifecycle()
    val foregroundCutout by viewModel.foregroundCutout.collectAsStateWithLifecycle()
    val config by viewModel.currentConfig.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val applyMessage by viewModel.wallpaperApplyMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(EditorTab.CLOCK) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var projectTitleInput by remember { mutableStateOf(config.title) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(applyMessage) {
        applyMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearApplyMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Depth Wallpaper Editor",
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
                    // Fullscreen lockscreen preview simulation
                    IconButton(
                        onClick = onNavigateToPreview,
                        modifier = Modifier.testTag("preview_lockscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Preview Lock Screen",
                            tint = CyanAccent
                        )
                    }

                    // Save Project to Room
                    IconButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.testTag("save_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Project",
                            tint = IndigoLight
                        )
                    }

                    // Export / Apply
                    Button(
                        onClick = { showExportSheet = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_apply_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Apply", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Lock Screen Depth Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val canvasH = maxHeight
                val canvasW = canvasH * (9f / 20f)

                Box(
                    modifier = Modifier
                        .size(canvasW, canvasH)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.5.dp, DarkCardBorder, RoundedCornerShape(26.dp))
                        .background(Color.Black)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                // Drag to reposition subject
                                viewModel.updateConfig {
                                    it.copy(
                                        subjectOffsetX = it.subjectOffsetX + dragAmount.x,
                                        subjectOffsetY = it.subjectOffsetY + dragAmount.y
                                    )
                                }
                            }
                        }
                ) {
                    // LAYER 1: Background Bitmap
                    if (backgroundBitmap != null) {
                        val blurMod = if (config.bgBlurRadius > 0.5f) {
                            Modifier.blur(config.bgBlurRadius.dp)
                        } else {
                            Modifier
                        }

                        androidx.compose.foundation.Image(
                            bitmap = backgroundBitmap!!.asImageBitmap(),
                            contentDescription = "Background Layer",
                            modifier = Modifier
                                .fillMaxSize()
                                .then(blurMod)
                        )

                        // Background Dimming Layer
                        if (config.bgDimPercent > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = config.bgDimPercent))
                            )
                        }
                    }

                    // LAYER 2: Clock & Date (When depth overlap is enabled, clock is behind subject!)
                    if (config.depthOverlapEnabled) {
                        EditorClockWidget(
                            config = config,
                            canvasHeight = canvasH.value,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // LAYER 3: Depth Shadow & Foreground Cutout
                    if (foregroundCutout != null) {
                        // Depth Shadow
                        if (config.depthOverlapEnabled && config.depthShadowIntensity > 0.05f) {
                            androidx.compose.foundation.Image(
                                bitmap = foregroundCutout!!.asImageBitmap(),
                                contentDescription = "Subject Depth Shadow",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = config.subjectScale,
                                        scaleY = config.subjectScale,
                                        translationX = config.subjectOffsetX + 4f,
                                        translationY = config.subjectOffsetY + 8f,
                                        alpha = config.depthShadowIntensity * 0.7f
                                    )
                                    .blur(16.dp)
                            )
                        }

                        // Foreground Subject Layer (Overlaps the clock numbers!)
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

                    // If Depth Overlap is disabled, clock renders on top of subject
                    if (!config.depthOverlapEnabled) {
                        EditorClockWidget(
                            config = config,
                            canvasHeight = canvasH.value,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Touch Drag Guide Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Drag subject to position over clock",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Controls Drawer Tabs
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                        color = IndigoPrimary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == EditorTab.CLOCK,
                    onClick = { activeTab = EditorTab.CLOCK },
                    icon = { Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Clock", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == EditorTab.DEPTH,
                    onClick = { activeTab = EditorTab.DEPTH },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Depth 3D", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == EditorTab.BACKGROUND,
                    onClick = { activeTab = EditorTab.BACKGROUND },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Backdrop", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == EditorTab.DATE_WIDGET,
                    onClick = { activeTab = EditorTab.DATE_WIDGET },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Widgets", fontSize = 11.sp) }
                )
            }

            // Controls Scrollable Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.4f))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    EditorTab.CLOCK -> {
                        // Clock Typography Styles
                        Text(
                            text = "Clock Typography Style",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ClockStyles.getStyles()) { style ->
                                FilterChip(
                                    selected = config.clockStyleId == style.id,
                                    onClick = {
                                        viewModel.updateConfig { it.copy(clockStyleId = style.id) }
                                    },
                                    label = { Text(style.name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Clock Size Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Size: ${config.clockSizeSp.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(70.dp)
                            )
                            Slider(
                                value = config.clockSizeSp,
                                onValueChange = { sizeVal ->
                                    viewModel.updateConfig { it.copy(clockSizeSp = sizeVal) }
                                },
                                valueRange = 64f..130f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Clock Vertical Position
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Height: ${config.clockVerticalOffset.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(70.dp)
                            )
                            Slider(
                                value = config.clockVerticalOffset,
                                onValueChange = { vOffset ->
                                    viewModel.updateConfig { it.copy(clockVerticalOffset = vOffset) }
                                },
                                valueRange = -120f..140f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Clock Colors
                        Text(
                            text = "Clock Tint",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(ClockColorPalette) { (hex, name) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (config.clockColorHex.equals(hex, true)) 3.dp else 1.dp,
                                            color = if (config.clockColorHex.equals(hex, true)) IndigoPrimary else DarkCardBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateConfig { it.copy(clockColorHex = hex) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (config.clockColorHex.equals(hex, true)) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    EditorTab.DEPTH -> {
                        // Depth Overlap Switch
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "3D Depth Overlap",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Subject overlaps in front of the clock numbers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = config.depthOverlapEnabled,
                                    onCheckedChange = { isEnabled ->
                                        viewModel.updateConfig { it.copy(depthOverlapEnabled = isEnabled) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IndigoPrimary
                                    )
                                )
                            }
                        }

                        // Depth Drop Shadow Intensity
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Depth Shadow: ${(config.depthShadowIntensity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.depthShadowIntensity,
                                onValueChange = { shadowVal ->
                                    viewModel.updateConfig { it.copy(depthShadowIntensity = shadowVal) }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Subject Scale
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Subject Scale: ${String.format("%.2f", config.subjectScale)}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.subjectScale,
                                onValueChange = { sVal ->
                                    viewModel.updateConfig { it.copy(subjectScale = sVal) }
                                },
                                valueRange = 0.7f..1.5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Quick action: Touch-up mask
                        OutlinedButton(
                            onClick = onNavigateToSegment,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Refine Subject Cutout Mask")
                        }
                    }

                    EditorTab.BACKGROUND -> {
                        // Background Blur Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Backdrop Blur: ${config.bgBlurRadius.toInt()}px",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.bgBlurRadius,
                                onValueChange = { blurVal ->
                                    viewModel.updateConfig { it.copy(bgBlurRadius = blurVal) }
                                },
                                valueRange = 0f..25f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Background Dimming Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dimming: ${(config.bgDimPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(110.dp)
                            )
                            Slider(
                                value = config.bgDimPercent,
                                onValueChange = { dimVal ->
                                    viewModel.updateConfig { it.copy(bgDimPercent = dimVal) }
                                },
                                valueRange = 0f..0.65f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Reset button
                        OutlinedButton(
                            onClick = {
                                viewModel.updateConfig {
                                    it.copy(
                                        bgBlurRadius = 0f,
                                        bgDimPercent = 0.15f,
                                        subjectOffsetX = 0f,
                                        subjectOffsetY = 0f,
                                        subjectScale = 1.0f
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Reset Background & Position")
                        }
                    }

                    EditorTab.DATE_WIDGET -> {
                        Text(
                            text = "Date Position",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = config.datePosition == "above_clock",
                                onClick = {
                                    viewModel.updateConfig { it.copy(datePosition = "above_clock") }
                                },
                                label = { Text("Above Clock") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = config.datePosition == "below_clock",
                                onClick = {
                                    viewModel.updateConfig { it.copy(datePosition = "below_clock") }
                                },
                                label = { Text("Below Clock") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Text(
                            text = "Lock Screen Widget",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val widgets = listOf(
                                "weather" to "☀️ Weather",
                                "battery" to "🔋 Battery",
                                "calendar" to "📅 Calendar",
                                "steps" to "👟 Steps",
                                "none" to "None"
                            )
                            items(widgets) { (wType, wLabel) ->
                                FilterChip(
                                    selected = config.widgetType == wType,
                                    onClick = {
                                        viewModel.updateConfig { it.copy(widgetType = wType) }
                                    },
                                    label = { Text(wLabel, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Project Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Wallpaper Project") },
            text = {
                Column {
                    Text(
                        text = "Enter a title for this depth wallpaper:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = projectTitleInput,
                        onValueChange = { projectTitleInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentProject(projectTitleInput) {
                            showSaveDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Project saved successfully!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export & Apply Bottom Sheet
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Apply & Export Depth Wallpaper",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                // Android lock screen explanation banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IndigoPrimary.copy(alpha = 0.3f)))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Standard Android lock screens allow setting custom wallpaper backgrounds. DepthLock exports the clock depth composition so it appears seamlessly on your lock screen!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }

                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = IndigoPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Rendering high-resolution depth wallpaper...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // Option 1: Set as Lock Screen Wallpaper
                    Button(
                        onClick = {
                            viewModel.applyAsSystemWallpaper(WallpaperTarget.LOCK_SCREEN) {
                                showExportSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("apply_lock_screen_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Set as Lock Screen Wallpaper", fontWeight = FontWeight.SemiBold)
                    }

                    // Option 2: Set as Home Screen
                    OutlinedButton(
                        onClick = {
                            viewModel.applyAsSystemWallpaper(WallpaperTarget.HOME_SCREEN) {
                                showExportSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("apply_home_screen_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Set as Home Screen Wallpaper", fontWeight = FontWeight.SemiBold)
                    }

                    // Option 3: Set as Both Home & Lock Screen
                    OutlinedButton(
                        onClick = {
                            viewModel.applyAsSystemWallpaper(WallpaperTarget.BOTH) {
                                showExportSheet = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Set on Both Screens", fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = DarkCardBorder)

                    // Option 4: Save to Photos Gallery
                    OutlinedButton(
                        onClick = {
                            viewModel.exportWallpaperToGallery { uri ->
                                showExportSheet = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Wallpaper saved to Gallery (Pictures/DepthLock)!")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_to_gallery_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Save High-Res Image to Gallery", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditorClockWidget(
    config: com.example.data.model.DepthWallpaper,
    canvasHeight: Float,
    modifier: Modifier = Modifier
) {
    val style = remember(config.clockStyleId) { ClockStyles.getStyleById(config.clockStyleId) }
    val clockColor = remember(config.clockColorHex) {
        try {
            Color(android.graphics.Color.parseColor(config.clockColorHex))
        } catch (_: Exception) {
            Color.White
        }
    }

    val date = remember { Date() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember { timeFormat.format(date) }
    val hoursString = remember { SimpleDateFormat("HH", Locale.getDefault()).format(date) }
    val minutesString = remember { SimpleDateFormat("mm", Locale.getDefault()).format(date) }

    val dateFormat = remember(config.dateFormatPattern) { SimpleDateFormat(config.dateFormatPattern, Locale.getDefault()) }
    val dateString = remember { dateFormat.format(date).uppercase(Locale.getDefault()) }

    val widgetText = when (config.widgetType) {
        "weather" -> "☀️ 72° Sunny"
        "battery" -> "🔋 88% Charged"
        "calendar" -> "📅 2:30 PM Meeting"
        "steps" -> "👟 6,420 Steps"
        else -> ""
    }
    val dateDisplay = if (widgetText.isNotEmpty()) "$dateString • $widgetText" else dateString

    // Relative vertical base for clock
    val topPadding = (canvasHeight * 0.16f) + (config.clockVerticalOffset * 0.5f)

    Column(
        modifier = modifier
            .padding(top = topPadding.coerceAtLeast(10f).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (config.datePosition == "above_clock") {
            Text(
                text = dateDisplay,
                color = clockColor.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (config.clockSizeSp * 0.13f).sp,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (style.isStacked) {
            Text(
                text = hoursString,
                color = clockColor,
                fontSize = (config.clockSizeSp * 0.72f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                lineHeight = (config.clockSizeSp * 0.65f).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = minutesString,
                color = clockColor,
                fontSize = (config.clockSizeSp * 0.72f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                lineHeight = (config.clockSizeSp * 0.65f).sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = timeString,
                color = clockColor,
                fontSize = (config.clockSizeSp * 0.72f).sp,
                fontWeight = when (config.clockStyleId) {
                    "heavy" -> FontWeight.Black
                    "thin" -> FontWeight.Light
                    "classic" -> FontWeight.Bold
                    else -> FontWeight.Bold
                },
                fontFamily = when (config.clockStyleId) {
                    "classic" -> FontFamily.Serif
                    "neon" -> FontFamily.Monospace
                    else -> FontFamily.SansSerif
                },
                textAlign = TextAlign.Center
            )
        }

        if (config.datePosition == "below_clock") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateDisplay,
                color = clockColor.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (config.clockSizeSp * 0.13f).sp,
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
