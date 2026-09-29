package com.example.domain.compositor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.data.model.DepthWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ClockStyle(
    val id: String,
    val name: String,
    val typeface: Typeface,
    val isStacked: Boolean = false,
    val letterSpacing: Float = 0f
)

object ClockStyles {
    fun getStyles(): List<ClockStyle> = listOf(
        ClockStyle("rounded", "Modern Round", Typeface.create(Typeface.DEFAULT, Typeface.BOLD), letterSpacing = 0.02f),
        ClockStyle("heavy", "Ultra Heavy", Typeface.create("sans-serif-black", Typeface.BOLD), letterSpacing = -0.02f),
        ClockStyle("classic", "Serif Elegant", Typeface.create(Typeface.SERIF, Typeface.BOLD), letterSpacing = 0.04f),
        ClockStyle("thin", "Tech Thin", Typeface.create("sans-serif-light", Typeface.NORMAL), letterSpacing = 0.08f),
        ClockStyle("neon", "Cyber Mono", Typeface.create(Typeface.MONOSPACE, Typeface.BOLD), letterSpacing = 0.05f),
        ClockStyle("condensed", "Compact Bold", Typeface.create("sans-serif-condensed", Typeface.BOLD), letterSpacing = 0.01f),
        ClockStyle("stacked", "Stacked Poster", Typeface.create("sans-serif-black", Typeface.BOLD), isStacked = true)
    )

    fun getStyleById(id: String): ClockStyle {
        return getStyles().firstOrNull { it.id == id } ?: getStyles().first()
    }
}

class WallpaperCompositor(private val context: Context) {

    suspend fun compositeWallpaper(
        background: Bitmap,
        foreground: Bitmap,
        config: DepthWallpaper,
        outputWidth: Int = background.width,
        outputHeight: Int = background.height,
        mockCurrentTime: Long = System.currentTimeMillis()
    ): Bitmap = withContext(Dispatchers.Default) {
        val result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Process and draw Background
        val scaledBg = if (background.width != outputWidth || background.height != outputHeight) {
            Bitmap.createScaledBitmap(background, outputWidth, outputHeight, true)
        } else {
            background
        }

        // Optional background blur
        val blurredBg = if (config.bgBlurRadius > 0.5f) {
            applyFastBlur(scaledBg, config.bgBlurRadius.toInt().coerceIn(1, 25))
        } else {
            scaledBg
        }
        canvas.drawBitmap(blurredBg, 0f, 0f, null)

        if (blurredBg != scaledBg && blurredBg != background) {
            blurredBg.recycle()
        }
        if (scaledBg != background) {
            scaledBg.recycle()
        }

        // Background Dimming overlay
        if (config.bgDimPercent > 0.01f) {
            val dimPaint = Paint().apply {
                color = Color.BLACK
                alpha = (config.bgDimPercent.coerceIn(0f, 1f) * 255).toInt()
            }
            canvas.drawRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat(), dimPaint)
        }

        // Prepare foreground transforms
        val scaledFg = if (foreground.width != outputWidth || foreground.height != outputHeight) {
            Bitmap.createScaledBitmap(foreground, outputWidth, outputHeight, true)
        } else {
            foreground
        }

        // Draw Clock & Subject according to depth overlap preference
        if (config.depthOverlapEnabled) {
            // LAYER 2: Clock behind subject
            drawClockAndWidgets(canvas, config, outputWidth, outputHeight, mockCurrentTime)

            // LAYER 3: Depth shadow under subject onto clock
            if (config.depthShadowIntensity > 0.05f) {
                drawSubjectDepthShadow(canvas, scaledFg, config, outputWidth, outputHeight)
            }

            // LAYER 4: Foreground Subject in front of clock
            drawForegroundSubject(canvas, scaledFg, config, outputWidth, outputHeight)
        } else {
            // Non-depth mode: Subject drawn first, then clock on top
            drawForegroundSubject(canvas, scaledFg, config, outputWidth, outputHeight)
            drawClockAndWidgets(canvas, config, outputWidth, outputHeight, mockCurrentTime)
        }

        if (scaledFg != foreground) {
            scaledFg.recycle()
        }

        result
    }

    private fun drawForegroundSubject(
        canvas: Canvas,
        foreground: Bitmap,
        config: DepthWallpaper,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.save()

        // Apply scale & offset relative to center
        val centerX = targetWidth / 2f
        val centerY = targetHeight / 2f

        canvas.translate(centerX + config.subjectOffsetX, centerY + config.subjectOffsetY)
        canvas.scale(config.subjectScale, config.subjectScale)
        canvas.translate(-centerX, -centerY)

        canvas.drawBitmap(foreground, 0f, 0f, paint)
        canvas.restore()
    }

    private fun drawSubjectDepthShadow(
        canvas: Canvas,
        foreground: Bitmap,
        config: DepthWallpaper,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (config.depthShadowIntensity.coerceIn(0f, 1f) * 160).toInt()
            maskFilter = BlurMaskFilter(24f * config.depthShadowIntensity, BlurMaskFilter.Blur.NORMAL)
        }

        canvas.save()
        val centerX = targetWidth / 2f
        val centerY = targetHeight / 2f

        canvas.translate(centerX + config.subjectOffsetX + 4f, centerY + config.subjectOffsetY + 10f)
        canvas.scale(config.subjectScale, config.subjectScale)
        canvas.translate(-centerX, -centerY)

        // Draw shadow using alpha channel of foreground
        val alphaBitmap = foreground.extractAlpha()
        canvas.drawBitmap(alphaBitmap, 0f, 0f, shadowPaint)
        alphaBitmap.recycle()
        canvas.restore()
    }

    private fun drawClockAndWidgets(
        canvas: Canvas,
        config: DepthWallpaper,
        targetWidth: Int,
        targetHeight: Int,
        currentTime: Long
    ) {
        val style = ClockStyles.getStyleById(config.clockStyleId)
        val clockColor = try {
            Color.parseColor(config.clockColorHex)
        } catch (_: Exception) {
            Color.WHITE
        }

        val date = Date(currentTime)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(date)
        val hoursString = SimpleDateFormat("HH", Locale.getDefault()).format(date)
        val minutesString = SimpleDateFormat("mm", Locale.getDefault()).format(date)

        val dateFormat = SimpleDateFormat(config.dateFormatPattern, Locale.getDefault())
        val dateString = dateFormat.format(date).uppercase(Locale.getDefault())

        val clockBaseY = (targetHeight * 0.22f) + config.clockVerticalOffset
        val density = targetWidth / 360f
        val clockCenterX = (targetWidth / 2f) + (config.clockHorizontalOffset * density)

        val resolvedTypeface = when (config.clockFontWeight) {
            "thin" -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
            "normal" -> Typeface.create(style.typeface, Typeface.NORMAL)
            "medium" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "bold" -> Typeface.create(style.typeface, Typeface.BOLD)
            "heavy" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            else -> style.typeface
        }

        // Clock Text Paint
        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = clockColor
            typeface = resolvedTypeface
            textSize = config.clockSizeSp * density
            textAlign = Paint.Align.CENTER
            letterSpacing = style.letterSpacing
            // Soft text shadow for readability
            setShadowLayer(16f, 0f, 4f, Color.argb(120, 0, 0, 0))
        }

        // Date & Widget Paint
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = clockColor
            alpha = 220
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = (config.clockSizeSp * 0.20f) * density
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
            setShadowLayer(8f, 0f, 2f, Color.argb(100, 0, 0, 0))
        }

        // Widget text string
        val widgetText = when (config.widgetType) {
            "weather" -> "☀️ 72° Sunny"
            "battery" -> "🔋 88% Charged"
            "calendar" -> "📅 2:30 PM Meeting"
            "steps" -> "👟 6,420 Steps"
            else -> ""
        }

        val dateDisplay = if (widgetText.isNotEmpty()) "$dateString • $widgetText" else dateString

        if (style.isStacked) {
            // Stacked Hours and Minutes
            val hoursY = clockBaseY - (clockPaint.textSize * 0.2f)
            val minutesY = clockBaseY + (clockPaint.textSize * 0.75f)

            if (config.datePosition == "above_clock") {
                canvas.drawText(dateDisplay, clockCenterX, hoursY - (clockPaint.textSize * 0.65f), datePaint)
            }
            canvas.drawText(hoursString, clockCenterX, hoursY, clockPaint)
            canvas.drawText(minutesString, clockCenterX, minutesY, clockPaint)

            if (config.datePosition == "below_clock") {
                canvas.drawText(dateDisplay, clockCenterX, minutesY + (datePaint.textSize * 2.2f), datePaint)
            }
        } else {
            // Single line HH:mm
            if (config.datePosition == "above_clock") {
                canvas.drawText(dateDisplay, clockCenterX, clockBaseY - (clockPaint.textSize * 0.82f), datePaint)
            }

            canvas.drawText(timeString, clockCenterX, clockBaseY, clockPaint)

            if (config.datePosition == "below_clock") {
                canvas.drawText(dateDisplay, clockCenterX, clockBaseY + (datePaint.textSize * 2.2f), datePaint)
            }
        }
    }

    private fun applyFastBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)

        val r = radius.coerceIn(1, 25)
        val div = 2 * r + 1

        val temp = IntArray(w * h)

        // Horizontal pass
        for (y in 0 until h) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (i in -r..r) {
                val p = pix[y * w + i.coerceIn(0, w - 1)]
                sumA += Color.alpha(p)
                sumR += Color.red(p)
                sumG += Color.green(p)
                sumB += Color.blue(p)
            }
            for (x in 0 until w) {
                temp[y * w + x] = Color.argb(sumA / div, sumR / div, sumG / div, sumB / div)
                val pOut = pix[y * w + (x - r).coerceIn(0, w - 1)]
                val pIn = pix[y * w + (x + r + 1).coerceIn(0, w - 1)]
                sumA += Color.alpha(pIn) - Color.alpha(pOut)
                sumR += Color.red(pIn) - Color.red(pOut)
                sumG += Color.green(pIn) - Color.green(pOut)
                sumB += Color.blue(pIn) - Color.blue(pOut)
            }
        }

        // Vertical pass
        for (x in 0 until w) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (i in -r..r) {
                val p = temp[i.coerceIn(0, h - 1) * w + x]
                sumA += Color.alpha(p)
                sumR += Color.red(p)
                sumG += Color.green(p)
                sumB += Color.blue(p)
            }
            for (y in 0 until h) {
                pix[y * w + x] = Color.argb(sumA / div, sumR / div, sumG / div, sumB / div)
                val pOut = temp[(y - r).coerceIn(0, h - 1) * w + x]
                val pIn = temp[(y + r + 1).coerceIn(0, h - 1) * w + x]
                sumA += Color.alpha(pIn) - Color.alpha(pOut)
                sumR += Color.red(pIn) - Color.red(pOut)
                sumG += Color.green(pIn) - Color.green(pOut)
                sumB += Color.blue(pIn) - Color.blue(pOut)
            }
        }

        output.setPixels(pix, 0, w, 0, 0, w, h)
        return output
    }
}
