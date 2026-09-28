package com.example.domain.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class WallpaperTarget {
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH
}

sealed class WallpaperApplyResult {
    data object Success : WallpaperApplyResult()
    data class Error(val message: String) : WallpaperApplyResult()
}

class WallpaperManagerHelper(private val context: Context) {

    suspend fun setWallpaper(bitmap: Bitmap, target: WallpaperTarget): WallpaperApplyResult = withContext(Dispatchers.IO) {
        val wallpaperManager = WallpaperManager.getInstance(context)

        if (!wallpaperManager.isWallpaperSupported) {
            return@withContext WallpaperApplyResult.Error("Wallpaper setting is not supported on this device.")
        }

        try {
            when (target) {
                WallpaperTarget.HOME_SCREEN -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
                WallpaperTarget.LOCK_SCREEN -> {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
                WallpaperTarget.BOTH -> {
                    wallpaperManager.setBitmap(
                        bitmap,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    )
                }
            }
            WallpaperApplyResult.Success
        } catch (e: SecurityException) {
            WallpaperApplyResult.Error("Permission denied: ${e.localizedMessage}")
        } catch (e: IOException) {
            WallpaperApplyResult.Error("I/O error while setting wallpaper: ${e.localizedMessage}")
        } catch (e: Exception) {
            WallpaperApplyResult.Error("Failed to apply wallpaper: ${e.localizedMessage}")
        }
    }
}
