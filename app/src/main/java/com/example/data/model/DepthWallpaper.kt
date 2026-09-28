package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "depth_wallpapers")
data class DepthWallpaper(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String,
    val maskPath: String,
    val clockStyleId: String = "rounded",
    val clockColorHex: String = "#FFFFFF",
    val clockSizeSp: Float = 96f,
    val clockVerticalOffset: Float = 0f,
    val clockFontWeight: String = "bold",
    val dateFormatPattern: String = "EEE, d MMM",
    val datePosition: String = "above_clock",
    val widgetType: String = "weather",
    val bgBlurRadius: Float = 0f,
    val bgDimPercent: Float = 0.15f,
    val depthOverlapEnabled: Boolean = true,
    val depthShadowIntensity: Float = 0.5f,
    val subjectScale: Float = 1.0f,
    val subjectOffsetX: Float = 0f,
    val subjectOffsetY: Float = 0f,
    val exportPath: String? = null,
    val thumbnailPath: String? = null
)
