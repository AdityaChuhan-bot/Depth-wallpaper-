package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DepthLockDatabase
import com.example.data.model.DepthWallpaper
import com.example.data.repository.WallpaperRepository
import com.example.domain.compositor.WallpaperCompositor
import com.example.domain.segmentation.SegmentationEngine
import com.example.domain.wallpaper.ImageExportHelper
import com.example.domain.wallpaper.WallpaperApplyResult
import com.example.domain.wallpaper.WallpaperManagerHelper
import com.example.domain.wallpaper.WallpaperTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class DepthLockViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DepthLockDatabase.getDatabase(application)
    private val repository = WallpaperRepository(application, database.wallpaperDao())
    private val segmentationEngine = SegmentationEngine()
    private val wallpaperCompositor = WallpaperCompositor(application)
    private val wallpaperManagerHelper = WallpaperManagerHelper(application)
    private val imageExportHelper = ImageExportHelper(application)

    val savedWallpapers: StateFlow<List<DepthWallpaper>> = repository.allWallpapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _storageSize = MutableStateFlow("0.0 MB")
    val storageSize: StateFlow<String> = _storageSize.asStateFlow()

    // Current Editing Session State
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _croppedBitmap = MutableStateFlow<Bitmap?>(null)
    val croppedBitmap: StateFlow<Bitmap?> = _croppedBitmap.asStateFlow()

    private val _maskBitmap = MutableStateFlow<Bitmap?>(null)
    val maskBitmap: StateFlow<Bitmap?> = _maskBitmap.asStateFlow()

    private val _foregroundCutout = MutableStateFlow<Bitmap?>(null)
    val foregroundCutout: StateFlow<Bitmap?> = _foregroundCutout.asStateFlow()

    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap.asStateFlow()

    private val _currentConfig = MutableStateFlow(
        DepthWallpaper(
            title = "My Depth Wallpaper",
            imagePath = "",
            maskPath = ""
        )
    )
    val currentConfig: StateFlow<DepthWallpaper> = _currentConfig.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingStatus = MutableStateFlow("")
    val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

    private val _detectedSubjectType = MutableStateFlow("Subject")
    val detectedSubjectType: StateFlow<String> = _detectedSubjectType.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportSuccessUri = MutableStateFlow<Uri?>(null)
    val exportSuccessUri: StateFlow<Uri?> = _exportSuccessUri.asStateFlow()

    private val _wallpaperApplyMessage = MutableStateFlow<String?>(null)
    val wallpaperApplyMessage: StateFlow<String?> = _wallpaperApplyMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleWallpapersIfEmpty()
            refreshStorageSize()
        }
    }

    fun refreshStorageSize() {
        viewModelScope.launch {
            _storageSize.value = repository.getStorageSizeFormatted()
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingStatus.value = "Loading image..."
            try {
                val bitmap = loadOptimizedBitmap(uri)
                if (bitmap != null) {
                    _originalBitmap.value?.recycle()
                    _originalBitmap.value = bitmap
                    _croppedBitmap.value?.recycle()
                    _croppedBitmap.value = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    _errorMessage.value = "Could not decode the selected image."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load image: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun onCropCompleted(cropped: Bitmap) {
        _croppedBitmap.value?.let { if (it != cropped && !it.isRecycled) it.recycle() }
        _croppedBitmap.value = cropped
        startSegmentation(cropped)
    }

    fun startSegmentation(target: Bitmap? = null) {
        val targetBitmap = target ?: _croppedBitmap.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingStatus.value = "Analyzing image with on-device AI..."
            try {
                val result = segmentationEngine.segmentImage(targetBitmap)
                _maskBitmap.value?.let { if (!it.isRecycled) it.recycle() }
                _maskBitmap.value = result.maskBitmap

                _foregroundCutout.value?.let { if (!it.isRecycled) it.recycle() }
                _foregroundCutout.value = result.foregroundCutout

                _backgroundBitmap.value?.let { if (!it.isRecycled) it.recycle() }
                _backgroundBitmap.value = result.backgroundBitmap

                _detectedSubjectType.value = result.detectedSubjectType
            } catch (e: Exception) {
                _errorMessage.value = "Segmentation encountered an issue: ${e.localizedMessage}. Manual mask can still be used."
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun onManualMaskRefined(newMask: Bitmap) {
        viewModelScope.launch {
            _maskBitmap.value = newMask
            val base = _croppedBitmap.value ?: _backgroundBitmap.value
            if (base != null) {
                val updatedFg = segmentationEngine.extractForeground(base, newMask)
                _foregroundCutout.value?.let { if (!it.isRecycled) it.recycle() }
                _foregroundCutout.value = updatedFg
            }
        }
    }

    fun updateConfig(updater: (DepthWallpaper) -> DepthWallpaper) {
        _currentConfig.value = updater(_currentConfig.value)
    }

    fun loadSavedWallpaper(wallpaper: DepthWallpaper) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _processingStatus.value = "Loading wallpaper project..."
            try {
                val imageFile = File(wallpaper.imagePath)
                val maskFile = File(wallpaper.maskPath)

                if (imageFile.exists() && maskFile.exists()) {
                    val baseBm = BitmapFactory.decodeFile(imageFile.absolutePath)
                    val maskBm = BitmapFactory.decodeFile(maskFile.absolutePath)

                    if (baseBm != null && maskBm != null) {
                        val fgBm = segmentationEngine.extractForeground(baseBm, maskBm)

                        withContext(Dispatchers.Main) {
                            _croppedBitmap.value = baseBm
                            _backgroundBitmap.value = baseBm.copy(Bitmap.Config.ARGB_8888, true)
                            _maskBitmap.value = maskBm
                            _foregroundCutout.value = fgBm
                            _currentConfig.value = wallpaper
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Original project files not found on device storage."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to load project: ${e.localizedMessage}"
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveCurrentProject(title: String, onFinished: (Long) -> Unit) {
        viewModelScope.launch {
            val bg = _backgroundBitmap.value ?: _croppedBitmap.value ?: return@launch
            val mask = _maskBitmap.value ?: return@launch

            _isProcessing.value = true
            _processingStatus.value = "Saving project..."

            try {
                val imgPath = repository.saveBitmapToInternalStorage(bg, "wp_base")
                val maskPath = repository.saveBitmapToInternalStorage(mask, "wp_mask")

                val updatedConfig = _currentConfig.value.copy(
                    title = title.ifBlank { "Depth Wallpaper" },
                    imagePath = imgPath,
                    maskPath = maskPath,
                    createdAt = System.currentTimeMillis()
                )

                val id = repository.saveWallpaper(updatedConfig)
                _currentConfig.value = updatedConfig.copy(id = id)
                refreshStorageSize()
                onFinished(id)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save project: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteSavedWallpaper(wallpaper: DepthWallpaper) {
        viewModelScope.launch {
            repository.deleteWallpaper(wallpaper)
            refreshStorageSize()
        }
    }

    fun exportWallpaperToGallery(onSuccess: (Uri) -> Unit) {
        viewModelScope.launch {
            val bg = _backgroundBitmap.value ?: return@launch
            val fg = _foregroundCutout.value ?: return@launch

            _isExporting.value = true
            try {
                // Render at full Moto G85 target resolution (1080 x 2400) or high res
                val targetW = 1080
                val targetH = 2400
                val composite = wallpaperCompositor.compositeWallpaper(
                    background = bg,
                    foreground = fg,
                    config = _currentConfig.value,
                    outputWidth = targetW,
                    outputHeight = targetH
                )

                val uri = imageExportHelper.saveToGallery(composite, _currentConfig.value.title)
                composite.recycle()

                if (uri != null) {
                    _exportSuccessUri.value = uri
                    onSuccess(uri)
                } else {
                    _errorMessage.value = "Failed to write image to gallery."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Export failed: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun applyAsSystemWallpaper(target: WallpaperTarget, onComplete: () -> Unit) {
        viewModelScope.launch {
            val bg = _backgroundBitmap.value ?: return@launch
            val fg = _foregroundCutout.value ?: return@launch

            _isExporting.value = true
            try {
                val targetW = 1080
                val targetH = 2400
                val composite = wallpaperCompositor.compositeWallpaper(
                    background = bg,
                    foreground = fg,
                    config = _currentConfig.value,
                    outputWidth = targetW,
                    outputHeight = targetH
                )

                val result = wallpaperManagerHelper.setWallpaper(composite, target)
                composite.recycle()

                when (result) {
                    is WallpaperApplyResult.Success -> {
                        val targetDesc = when (target) {
                            WallpaperTarget.HOME_SCREEN -> "Home Screen"
                            WallpaperTarget.LOCK_SCREEN -> "Lock Screen"
                            WallpaperTarget.BOTH -> "Home & Lock Screen"
                        }
                        _wallpaperApplyMessage.value = "Depth Wallpaper successfully applied to $targetDesc!"
                        onComplete()
                    }
                    is WallpaperApplyResult.Error -> {
                        _errorMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Could not apply wallpaper: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            refreshStorageSize()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearApplyMessage() {
        _wallpaperApplyMessage.value = null
    }

    private suspend fun loadOptimizedBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver

        // First pass: decode bounds
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return@withContext null

        // Calculate sample size: target max dimension ~2160 to prevent OutOfMemory
        val maxTargetDimension = 2160
        var sampleSize = 1
        val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
        while (maxDim / (sampleSize * 2) >= maxTargetDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _originalBitmap.value?.recycle()
        _croppedBitmap.value?.recycle()
        _maskBitmap.value?.recycle()
        _foregroundCutout.value?.recycle()
        _backgroundBitmap.value?.recycle()
    }
}
