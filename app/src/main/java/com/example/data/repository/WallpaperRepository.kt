package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.example.data.local.WallpaperDao
import com.example.data.model.DepthWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WallpaperRepository(
    private val context: Context,
    private val wallpaperDao: WallpaperDao
) {
    val allWallpapers: Flow<List<DepthWallpaper>> = wallpaperDao.getAllWallpapers()

    suspend fun getWallpaperById(id: Long): DepthWallpaper? = withContext(Dispatchers.IO) {
        wallpaperDao.getWallpaperById(id)
    }

    suspend fun saveWallpaper(wallpaper: DepthWallpaper): Long = withContext(Dispatchers.IO) {
        wallpaperDao.insertWallpaper(wallpaper)
    }

    suspend fun updateWallpaper(wallpaper: DepthWallpaper) = withContext(Dispatchers.IO) {
        wallpaperDao.updateWallpaper(wallpaper)
    }

    suspend fun deleteWallpaper(wallpaper: DepthWallpaper) = withContext(Dispatchers.IO) {
        // Delete image and mask files from internal storage
        try {
            File(wallpaper.imagePath).takeIf { it.exists() }?.delete()
            File(wallpaper.maskPath).takeIf { it.exists() }?.delete()
            wallpaper.thumbnailPath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
        } catch (_: Exception) {}
        wallpaperDao.deleteWallpaper(wallpaper)
    }

    suspend fun saveBitmapToInternalStorage(bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        file.absolutePath
    }

    suspend fun getStorageSizeFormatted(): String = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val dirs = listOf(
            File(context.filesDir, "wallpapers"),
            context.cacheDir
        )
        dirs.forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) totalBytes += file.length()
                }
            }
        }
        val mb = totalBytes / (1024.0 * 1024.0)
        String.format("%.1f MB", mb)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
    }

    suspend fun seedSampleWallpapersIfEmpty() = withContext(Dispatchers.IO) {
        val currentList = allWallpapers.firstOrNull()
        if (!currentList.isNullOrEmpty()) return@withContext

        // Create 2 beautiful ready-to-test sample depth wallpapers
        // Preset 1: Golden Sunset Mountaineer
        val (sample1Img, sample1Mask) = createMountaineerSample()
        val path1Img = saveBitmapToInternalStorage(sample1Img, "sample_mountain_base")
        val path1Mask = saveBitmapToInternalStorage(sample1Mask, "sample_mountain_mask")
        sample1Img.recycle()
        sample1Mask.recycle()

        wallpaperDao.insertWallpaper(
            DepthWallpaper(
                title = "Golden Peak Hiker",
                imagePath = path1Img,
                maskPath = path1Mask,
                clockStyleId = "rounded",
                clockColorHex = "#FFFFFF",
                clockSizeSp = 100f,
                clockVerticalOffset = 20f,
                clockFontWeight = "bold",
                dateFormatPattern = "EEE, d MMM",
                datePosition = "above_clock",
                widgetType = "weather",
                bgBlurRadius = 0f,
                bgDimPercent = 0.12f,
                depthOverlapEnabled = true,
                depthShadowIntensity = 0.65f,
                subjectScale = 1.0f,
                subjectOffsetX = 0f,
                subjectOffsetY = 0f
            )
        )

        // Preset 2: Cyber City Silhouette
        val (sample2Img, sample2Mask) = createCyberpunkSample()
        val path2Img = saveBitmapToInternalStorage(sample2Img, "sample_cyber_base")
        val path2Mask = saveBitmapToInternalStorage(sample2Mask, "sample_cyber_mask")
        sample2Img.recycle()
        sample2Mask.recycle()

        wallpaperDao.insertWallpaper(
            DepthWallpaper(
                title = "Neon Skyline Wanderer",
                imagePath = path2Img,
                maskPath = path2Mask,
                clockStyleId = "neon",
                clockColorHex = "#E0E7FF",
                clockSizeSp = 92f,
                clockVerticalOffset = 10f,
                clockFontWeight = "heavy",
                dateFormatPattern = "d MMMM",
                datePosition = "above_clock",
                widgetType = "battery",
                bgBlurRadius = 4f,
                bgDimPercent = 0.20f,
                depthOverlapEnabled = true,
                depthShadowIntensity = 0.75f,
                subjectScale = 1.05f,
                subjectOffsetX = 0f,
                subjectOffsetY = 15f
            )
        )
    }

    private fun createMountaineerSample(): Pair<Bitmap, Bitmap> {
        val width = 720
        val height = 1600
        val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(baseBitmap)
        val maskCanvas = Canvas(maskBitmap)

        // Sky sunset gradient
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat() * 0.7f,
                intArrayOf(
                    Color.parseColor("#1E1B4B"),
                    Color.parseColor("#4C1D95"),
                    Color.parseColor("#BE185D"),
                    Color.parseColor("#F97316"),
                    Color.parseColor("#FBBF24")
                ),
                floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)

        // Distant mountains (part of background)
        val mountainPaint = Paint().apply {
            color = Color.parseColor("#311347")
            isAntiAlias = true
        }
        val bgMountainPath = Path().apply {
            moveTo(0f, height * 0.55f)
            lineTo(width * 0.25f, height * 0.42f)
            lineTo(width * 0.5f, height * 0.48f)
            lineTo(width * 0.75f, height * 0.38f)
            lineTo(width.toFloat(), height * 0.52f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(bgMountainPath, mountainPaint)

        // Sun
        val sunPaint = Paint().apply {
            color = Color.parseColor("#FEF08A")
            isAntiAlias = true
        }
        canvas.drawCircle(width * 0.5f, height * 0.36f, 70f, sunPaint)

        // Foreground Subject: Rock crag with Hiker standing proudly, reaching up to Y = 0.28 (overlapping where clock sits!)
        val subjectPaint = Paint().apply {
            color = Color.parseColor("#090D16")
            isAntiAlias = true
        }
        val maskPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }

        val subjectPath = Path().apply {
            // High cliff ledge
            moveTo(0f, height.toFloat())
            lineTo(0f, height * 0.65f)
            lineTo(width * 0.32f, height * 0.55f)
            // Rock outcrop peaking at center
            lineTo(width * 0.42f, height * 0.45f)
            lineTo(width * 0.45f, height * 0.42f)
            // Hiker legs
            lineTo(width * 0.46f, height * 0.35f)
            // Hiker torso & backpack
            lineTo(width * 0.44f, height * 0.28f)
            // Hiker head & cap (at Y = 0.22, directly overlapping the clock!)
            arcTo(width * 0.47f, height * 0.20f, width * 0.53f, height * 0.26f, 180f, 180f, false)
            lineTo(width * 0.54f, height * 0.28f)
            lineTo(width * 0.56f, height * 0.35f)
            lineTo(width * 0.57f, height * 0.44f)
            // Right slope of cliff
            lineTo(width * 0.68f, height * 0.58f)
            lineTo(width.toFloat(), height * 0.70f)
            lineTo(width.toFloat(), height.toFloat())
            close()
        }

        canvas.drawPath(subjectPath, subjectPaint)
        maskCanvas.drawPath(subjectPath, maskPaint)

        return Pair(baseBitmap, maskBitmap)
    }

    private fun createCyberpunkSample(): Pair<Bitmap, Bitmap> {
        val width = 720
        val height = 1600
        val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(baseBitmap)
        val maskCanvas = Canvas(maskBitmap)

        // Moody cyberpunk city night background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#030712"),
                    Color.parseColor("#111827"),
                    Color.parseColor("#1E1B4B"),
                    Color.parseColor("#4C0519")
                ),
                floatArrayOf(0f, 0.35f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Neon grid & bokeh lights
        val neonPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 2f
            alpha = 100
        }
        for (i in 0..10) {
            val y = height * 0.5f + i * 40f
            canvas.drawLine(0f, y, width.toFloat(), y, neonPaint)
        }

        // Foreground Portrait Silhouette
        val subjectPaint = Paint().apply {
            color = Color.parseColor("#020617")
            isAntiAlias = true
        }
        val maskPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }

        val portraitPath = Path().apply {
            // Shoulders and chest rising
            moveTo(width * 0.15f, height.toFloat())
            quadTo(width * 0.22f, height * 0.48f, width * 0.38f, height * 0.38f)
            // Neck
            lineTo(width * 0.40f, height * 0.32f)
            // Head with headphones / hood (reaches Y = 0.20, perfectly overlapping the clock digits)
            arcTo(width * 0.34f, height * 0.18f, width * 0.66f, height * 0.32f, 180f, 180f, false)
            lineTo(width * 0.60f, height * 0.32f)
            lineTo(width * 0.62f, height * 0.38f)
            quadTo(width * 0.78f, height * 0.48f, width * 0.85f, height.toFloat())
            close()
        }

        canvas.drawPath(portraitPath, subjectPaint)
        maskCanvas.drawPath(portraitPath, maskPaint)

        return Pair(baseBitmap, maskBitmap)
    }
}
