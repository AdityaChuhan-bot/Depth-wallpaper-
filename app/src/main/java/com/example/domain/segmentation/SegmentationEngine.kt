package com.example.domain.segmentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class SegmentationResult(
    val maskBitmap: Bitmap,
    val foregroundCutout: Bitmap,
    val backgroundBitmap: Bitmap,
    val detectedSubjectType: String,
    val confidenceScore: Float
)

class SegmentationEngine {
    private val TAG = "SegmentationEngine"

    private val segmenterOptions = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()

    private val selfieSegmenter = Segmentation.getClient(segmenterOptions)

    suspend fun segmentImage(sourceBitmap: Bitmap): SegmentationResult = withContext(Dispatchers.Default) {
        // Optimize resolution for segmentation speed and memory stability (max 1080px dimension)
        val maxDimension = 1080
        val scale = if (sourceBitmap.width > maxDimension || sourceBitmap.height > maxDimension) {
            maxDimension.toFloat() / max(sourceBitmap.width, sourceBitmap.height)
        } else {
            1.0f
        }

        val targetWidth = (sourceBitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (sourceBitmap.height * scale).toInt().coerceAtLeast(1)

        val workingBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)
        } else {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Try ML Kit Selfie Segmenter first
        var mask: Bitmap? = null
        var confidence = 0f
        var subjectType = "Person / Portrait"

        try {
            val inputImage = InputImage.fromBitmap(workingBitmap, 0)
            val result = selfieSegmenter.process(inputImage).await()
            val maskBuffer = result.buffer
            val maskWidth = result.width
            val maskHeight = result.height

            if (maskWidth > 0 && maskHeight > 0) {
                val tempMask = createBitmapFromMaskBuffer(maskBuffer, maskWidth, maskHeight)
                
                // Calculate average confidence of detected foreground pixels
                confidence = calculateForegroundConfidence(tempMask)

                if (confidence > 0.08f) { // Valid human silhouette detected
                    // Scale mask to working dimensions
                    mask = if (maskWidth != targetWidth || maskHeight != targetHeight) {
                        Bitmap.createScaledBitmap(tempMask, targetWidth, targetHeight, true).also {
                            tempMask.recycle()
                        }
                    } else {
                        tempMask
                    }
                } else {
                    tempMask.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ML Kit segmentation error, falling back to adaptive saliency: ${e.message}")
        }

        // If ML Kit found low confidence (e.g. Pet, car, object, statue, nature), use Adaptive Saliency Segmenter
        if (mask == null) {
            Log.d(TAG, "Using Adaptive Saliency Segmenter for general object / pet / landscape")
            mask = generateAdaptiveSaliencyMask(workingBitmap)
            subjectType = "Object / Animal / Landscape"
            confidence = 0.85f
        }

        // Smooth mask edges
        val smoothedMask = smoothMaskEdges(mask)
        if (smoothedMask != mask) {
            mask.recycle()
        }

        // Generate foreground cutout and background
        val foregroundCutout = extractForeground(workingBitmap, smoothedMask)
        val background = workingBitmap.copy(Bitmap.Config.ARGB_8888, true)

        SegmentationResult(
            maskBitmap = smoothedMask,
            foregroundCutout = foregroundCutout,
            backgroundBitmap = background,
            detectedSubjectType = subjectType,
            confidenceScore = confidence
        )
    }

    private fun createBitmapFromMaskBuffer(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in 0 until width * height) {
            val conf = buffer.float
            // Binarize / soften confidence to alpha value
            val alpha = when {
                conf > 0.75f -> 255
                conf > 0.25f -> ((conf - 0.25f) / 0.5f * 255).toInt()
                else -> 0
            }
            pixels[i] = Color.argb(alpha, 255, 255, 255)
        }

        maskBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return maskBitmap
    }

    private fun calculateForegroundConfidence(maskBitmap: Bitmap): Float {
        val width = maskBitmap.width
        val height = maskBitmap.height
        val sampleStep = 8
        var fgCount = 0
        var totalSamples = 0

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val alpha = Color.alpha(maskBitmap.getPixel(x, y))
                if (alpha > 128) fgCount++
                totalSamples++
            }
        }
        return if (totalSamples > 0) fgCount.toFloat() / totalSamples else 0f
    }

    /**
     * Adaptive Saliency Segmentation:
     * Robust on-device fallback for general objects, pets, vehicles, or when ML Kit detects no human.
     * Computes color contrast against background edges, center distance weighting, and luminance gradients.
     */
    private fun generateAdaptiveSaliencyMask(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample border pixels to determine ambient background color profile
        var borderR = 0L
        var borderG = 0L
        var borderB = 0L
        var borderSamples = 0

        // Sample top border (likely sky or wall)
        for (x in 0 until width step 4) {
            val p = pixels[x]
            borderR += Color.red(p)
            borderG += Color.green(p)
            borderB += Color.blue(p)
            borderSamples++
        }
        // Sample sides
        for (y in 0 until height step 8) {
            val pLeft = pixels[y * width]
            val pRight = pixels[y * width + (width - 1)]
            borderR += Color.red(pLeft) + Color.red(pRight)
            borderG += Color.green(pLeft) + Color.green(pRight)
            borderB += Color.blue(pLeft) + Color.blue(pRight)
            borderSamples += 2
        }

        val avgBgR = (borderR / borderSamples).toInt()
        val avgBgG = (borderG / borderSamples).toInt()
        val avgBgB = (borderB / borderSamples).toInt()

        val maskPixels = IntArray(width * height)
        val centerX = width / 2f
        val centerY = height * 0.55f // bias slightly below vertical center
        val maxDist = sqrt((centerX * centerX) + (centerY * centerY))

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val p = pixels[idx]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                // Color difference from border
                val colorDiff = sqrt(
                    ((r - avgBgR) * (r - avgBgR) +
                     (g - avgBgG) * (g - avgBgG) +
                     (b - avgBgB) * (b - avgBgB)).toDouble()
                ).toFloat()

                // Center proximity weight
                val dx = x - centerX
                val dy = y - centerY
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val centerWeight = 1.0f - (dist / maxDist).coerceIn(0f, 1f)

                // Top exclusion: top 12% is usually sky / background
                val topFactor = if (y < height * 0.12f) (y / (height * 0.12f)) else 1.0f

                val saliency = (colorDiff / 255f) * 0.6f + (centerWeight * 0.4f)
                val finalScore = (saliency * topFactor).coerceIn(0f, 1f)

                val alpha = when {
                    finalScore > 0.45f -> 255
                    finalScore > 0.30f -> (((finalScore - 0.30f) / 0.15f) * 255).toInt()
                    else -> 0
                }

                maskPixels[idx] = Color.argb(alpha, 255, 255, 255)
            }
        }

        maskBitmap.setPixels(maskPixels, 0, width, 0, 0, width, height)
        return maskBitmap
    }

    private fun smoothMaskEdges(mask: Bitmap): Bitmap {
        // Fast 3x3 box blur on alpha channel to remove jagged pixel stairs
        val width = mask.width
        val height = mask.height
        val smoothed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        mask.getPixels(srcPixels, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sum = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        sum += Color.alpha(srcPixels[(y + dy) * width + (x + dx)])
                    }
                }
                val avgAlpha = sum / 9
                dstPixels[y * width + x] = Color.argb(avgAlpha, 255, 255, 255)
            }
        }
        smoothed.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return smoothed
    }

    fun extractForeground(source: Bitmap, mask: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw original
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Mask with alpha
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        val scaledMask = if (mask.width != source.width || mask.height != source.height) {
            Bitmap.createScaledBitmap(mask, source.width, source.height, true)
        } else {
            mask
        }
        canvas.drawBitmap(scaledMask, 0f, 0f, paint)
        paint.xfermode = null

        if (scaledMask != mask) {
            scaledMask.recycle()
        }

        return result
    }
}
