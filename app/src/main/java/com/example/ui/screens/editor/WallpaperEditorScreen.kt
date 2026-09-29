package com.example.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.DepthWallpaper
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

enum class WallpaperEditorTab {
    CLOCK,
    DEPTH,
    BACKGROUND,
    DATE_WIDGET
}

// Curated Material Design Color Swatches
val MaterialColorSwatches = listOf(
    "#FFFFFF" to "Pure White",
    "#F8FAFC" to "Off White",
    "#E0F2FE" to "Ice Cyan",
    "#C7D2FE" to "Soft Lavender",
    "#FED7AA" to "Peach Sunset",
    "#FEF08A" to "Warm Gold",
    "#BBF7D0" to "Mint Pastel",
    "#F43F5E" to "Cyber Rose",
    "#38BDF8" to "Electric Sky",
    "#818CF8" to "Vivid Indigo",
    "#A855F7" to "Neon Purple",
    "#1E293B" to "Deep Slate"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperEditorScreen(
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

    var activeTab by remember { mutableStateOf(WallpaperEditorTab.CLOCK) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var projectTitleInput by remember { mutableStateOf(config.title) }

    // Color picker Hue state (0f..360f)
    var customHue by remember { mutableFloatStateOf(210f) }

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
                    Column {
                        Text(
                            text = "Live DepthLock Editor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "iOS-Style 3D Parallax & Depth",
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
                    // Fullscreen preview
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

                    // Save project
                    IconButton(
                        onClick = {
                            projectTitleInput = config.title
                            showSaveDialog = true
                        },
                        modifier = Modifier.testTag("save_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Project",
                            tint = IndigoLight
                        )
                    }

                    // Apply Button
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
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
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
            // Live Real-Time Lock Screen Depth Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val canvasH = maxHeight
                val canvasW = canvasH * (9f / 20f)

                Box(
                    modifier = Modifier
                        .size(canvasW, canvasH)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.5.dp, DarkCardBorder, RoundedCornerShape(28.dp))
                        .background(Color.Black)
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                // Interactive gesture: Drag to reposition subject over clock
                                viewModel.updateConfig {
                                    it.copy(
                                        subjectOffsetX = it.subjectOffsetX + dragAmount.x,
                                        subjectOffsetY = it.subjectOffsetY + dragAmount.y
                                    )
                                }
                            }
                        }
                ) {
                    // LAYER 1: Background Layer with Real-Time Blur & Dimming
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

                        // Background Dimming for Clock Legibility
                        if (config.bgDimPercent > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = config.bgDimPercent))
                            )
                        }
                    }

                    // LAYER 2: Clock & Date (Rendered BEHIND Subject when Depth Overlap is enabled)
                    if (config.depthOverlapEnabled) {
                        LiveClockDisplay(
                            config = config,
                            canvasHeight = canvasH.value,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // LAYER 3: Foreground Subject Layer with Depth Drop Shadow
                    if (foregroundCutout != null) {
                        // Soft 3D Depth Shadow cast onto the Clock numbers
                        if (config.depthOverlapEnabled && config.depthShadowIntensity > 0.05f) {
                            androidx.compose.foundation.Image(
                                bitmap = foregroundCutout!!.asImageBitmap(),
                                contentDescription = "Subject Depth Shadow",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = config.subjectScale,
                                        scaleY = config.subjectScale,
                                        translationX = config.subjectOffsetX + 5f,
                                        translationY = config.subjectOffsetY + 10f,
                                        alpha = config.depthShadowIntensity * 0.75f
                                    )
                                    .blur(16.dp)
                            )
                        }

                        // Isolated Foreground Subject overlapping the Clock!
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

                    // If Depth Overlap is disabled, clock is on top of subject
                    if (!config.depthOverlapEnabled) {
                        LiveClockDisplay(
                            config = config,
                            canvasHeight = canvasH.value,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Drag Hint Overlay Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "Drag subject to adjust depth overlap",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Controls Tabs Header
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
                    selected = activeTab == WallpaperEditorTab.CLOCK,
                    onClick = { activeTab = WallpaperEditorTab.CLOCK },
                    icon = { Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Clock", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == WallpaperEditorTab.DEPTH,
                    onClick = { activeTab = WallpaperEditorTab.DEPTH },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Depth 3D", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == WallpaperEditorTab.BACKGROUND,
                    onClick = { activeTab = WallpaperEditorTab.BACKGROUND },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Backdrop", fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == WallpaperEditorTab.DATE_WIDGET,
                    onClick = { activeTab = WallpaperEditorTab.DATE_WIDGET },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    text = { Text("Widgets", fontSize = 11.sp) }
                )
            }

            // Controls Scrollable Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.45f))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeTab) {
                    WallpaperEditorTab.CLOCK -> {
                        // 1. Clock Font Selection with Fallback Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clock Font Typography",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            Text(
                                text = "7 Styles",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }

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

                        // 2. Font Weight Controls
                        Text(
                            text = "Font Weight",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val weights = listOf(
                                "thin" to "Thin",
                                "normal" to "Regular",
                                "medium" to "Medium",
                                "bold" to "Bold",
                                "heavy" to "Heavy"
                            )
                            weights.forEach { (weightKey, weightLabel) ->
                                FilterChip(
                                    selected = config.clockFontWeight == weightKey,
                                    onClick = {
                                        viewModel.updateConfig { it.copy(clockFontWeight = weightKey) }
                                    },
                                    label = { Text(weightLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // 3. Clock Font Size Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Size: ${config.clockSizeSp.toInt()}sp",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(80.dp)
                            )
                            Slider(
                                value = config.clockSizeSp,
                                onValueChange = { sizeVal ->
                                    viewModel.updateConfig { it.copy(clockSizeSp = sizeVal) }
                                },
                                valueRange = 60f..140f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // 4. Precise Clock Positioning (Vertical & Horizontal)
                        Text(
                            text = "Clock Positioning",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        // Vertical Height Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Vertical: ${config.clockVerticalOffset.toInt()}dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(95.dp)
                            )
                            Slider(
                                value = config.clockVerticalOffset,
                                onValueChange = { vOffset ->
                                    viewModel.updateConfig { it.copy(clockVerticalOffset = vOffset) }
                                },
                                valueRange = -150f..150f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Horizontal Offset Slider & Quick Alignment Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Horiz: ${config.clockHorizontalOffset.toInt()}dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(95.dp)
                            )
                            Slider(
                                value = config.clockHorizontalOffset,
                                onValueChange = { hOffset ->
                                    viewModel.updateConfig { it.copy(clockHorizontalOffset = hOffset) }
                                },
                                valueRange = -100f..100f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Alignment Shortcut Icons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateConfig { it.copy(clockHorizontalOffset = -50f) } },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FormatAlignLeft, contentDescription = "Left", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Left", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.updateConfig { it.copy(clockHorizontalOffset = 0f) } },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Center", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.updateConfig { it.copy(clockHorizontalOffset = 50f) } },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FormatAlignRight, contentDescription = "Right", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Right", fontSize = 11.sp)
                            }
                        }

                        // 5. Material Design Color Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clock Tint & Color",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            TextButton(onClick = { showColorPickerDialog = true }) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Palette", fontSize = 11.sp, color = CyanAccent)
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(MaterialColorSwatches) { (hex, name) ->
                                val swatchColor = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = config.clockColorHex.equals(hex, true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) IndigoPrimary else DarkCardBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateConfig { it.copy(clockColorHex = hex) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (hex == "#FFFFFF" || hex == "#F8FAFC") Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    WallpaperEditorTab.DEPTH -> {
                        // 3D Depth Overlap Switch
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3D Depth Overlap",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Foreground subject sits in front of the clock numbers",
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

                        // Depth Drop Shadow Intensity Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Depth Shadow: ${(config.depthShadowIntensity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(115.dp)
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

                        // Subject Scale Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Subject Scale: ${String.format("%.2f", config.subjectScale)}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(115.dp)
                            )
                            Slider(
                                value = config.subjectScale,
                                onValueChange = { scaleVal ->
                                    viewModel.updateConfig { it.copy(subjectScale = scaleVal) }
                                },
                                valueRange = 0.7f..1.5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Subject Offset X & Y sliders
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Subject X: ${config.subjectOffsetX.toInt()}dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(115.dp)
                            )
                            Slider(
                                value = config.subjectOffsetX,
                                onValueChange = { sx ->
                                    viewModel.updateConfig { it.copy(subjectOffsetX = sx) }
                                },
                                valueRange = -150f..150f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Subject Y: ${config.subjectOffsetY.toInt()}dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(115.dp)
                            )
                            Slider(
                                value = config.subjectOffsetY,
                                onValueChange = { sy ->
                                    viewModel.updateConfig { it.copy(subjectOffsetY = sy) }
                                },
                                valueRange = -150f..150f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Mask Refinement Shortcut
                        OutlinedButton(
                            onClick = onNavigateToSegment,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Refine Subject Mask with Brush / Eraser")
                        }
                    }

                    WallpaperEditorTab.BACKGROUND -> {
                        // Background Blur Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Backdrop Blur: ${config.bgBlurRadius.toInt()}px",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(120.dp)
                            )
                            Slider(
                                value = config.bgBlurRadius,
                                onValueChange = { blurVal ->
                                    viewModel.updateConfig { it.copy(bgBlurRadius = blurVal) }
                                },
                                valueRange = 0f..30f,
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
                                modifier = Modifier.width(120.dp)
                            )
                            Slider(
                                value = config.bgDimPercent,
                                onValueChange = { dimVal ->
                                    viewModel.updateConfig { it.copy(bgDimPercent = dimVal) }
                                },
                                valueRange = 0f..0.70f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoLight)
                            )
                        }

                        // Reset Position & Blur Button
                        OutlinedButton(
                            onClick = {
                                viewModel.updateConfig {
                                    it.copy(
                                        bgBlurRadius = 0f,
                                        bgDimPercent = 0.15f,
                                        subjectOffsetX = 0f,
                                        subjectOffsetY = 0f,
                                        subjectScale = 1.0f,
                                        clockHorizontalOffset = 0f,
                                        clockVerticalOffset = 0f
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Reset Layout & Background Controls")
                        }
                    }

                    WallpaperEditorTab.DATE_WIDGET -> {
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

    // Custom Color Spectrum Picker Dialog
    if (showColorPickerDialog) {
        val hsvColor = remember(customHue) {
            Color.hsv(customHue, 0.85f, 0.95f)
        }
        val hexString = remember(hsvColor) {
            val r = (hsvColor.red * 255).toInt()
            val g = (hsvColor.green * 255).toInt()
            val b = (hsvColor.blue * 255).toInt()
            String.format("#%02X%02X%02X", r, g, b)
        }

        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Material Color Spectrum") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(hsvColor)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Selected: $hexString", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Hue Spectrum", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Slider(
                        value = customHue,
                        onValueChange = { customHue = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = hsvColor,
                            activeTrackColor = IndigoLight
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateConfig { it.copy(clockColorHex = hexString) }
                        showColorPickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Apply Color")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentProject(projectTitleInput) {
                            showSaveDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Project \"$projectTitleInput\" saved!")
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
                            text = "DepthLock composites the customizable clock and overlapping foreground subject into a high-res wallpaper, applying it directly via Android's WallpaperManager API.",
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
fun LiveClockDisplay(
    config: DepthWallpaper,
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

    val topPadding = (canvasHeight * 0.16f) + (config.clockVerticalOffset * 0.5f)
    val horizontalOffset = config.clockHorizontalOffset * 0.5f

    val resolvedWeight = when (config.clockFontWeight) {
        "thin" -> FontWeight.Thin
        "normal" -> FontWeight.Normal
        "medium" -> FontWeight.Medium
        "bold" -> FontWeight.Bold
        "heavy" -> FontWeight.Black
        else -> FontWeight.Bold
    }

    val resolvedFamily = when (config.clockStyleId) {
        "classic" -> FontFamily.Serif
        "neon" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    Column(
        modifier = modifier
            .padding(top = topPadding.coerceAtLeast(10f).dp)
            .graphicsLayer(translationX = horizontalOffset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (config.datePosition == "above_clock") {
            Text(
                text = dateDisplay,
                color = clockColor.copy(alpha = 0.90f),
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
                fontWeight = resolvedWeight,
                fontFamily = resolvedFamily,
                lineHeight = (config.clockSizeSp * 0.65f).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = minutesString,
                color = clockColor,
                fontSize = (config.clockSizeSp * 0.72f).sp,
                fontWeight = resolvedWeight,
                fontFamily = resolvedFamily,
                lineHeight = (config.clockSizeSp * 0.65f).sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = timeString,
                color = clockColor,
                fontSize = (config.clockSizeSp * 0.72f).sp,
                fontWeight = resolvedWeight,
                fontFamily = resolvedFamily,
                textAlign = TextAlign.Center
            )
        }

        if (config.datePosition == "below_clock") {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateDisplay,
                color = clockColor.copy(alpha = 0.90f),
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
