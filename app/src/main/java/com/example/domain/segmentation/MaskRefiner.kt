package com.example.domain.segmentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import java.util.Stack

class MaskRefiner(initialMask: Bitmap) {
    private var currentMask: Bitmap = initialMask.copy(Bitmap.Config.ARGB_8888, true)
    private val undoStack = Stack<Bitmap>()
    private val redoStack = Stack<Bitmap>()
    private val maxHistory = 10

    private val brushPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun getMask(): Bitmap = currentMask

    fun pushHistory() {
        if (undoStack.size >= maxHistory) {
            val oldest = undoStack.removeAt(0)
            if (!oldest.isRecycled) oldest.recycle()
        }
        undoStack.push(currentMask.copy(Bitmap.Config.ARGB_8888, true))
        // Clear redo stack on new action
        while (redoStack.isNotEmpty()) {
            val bm = redoStack.pop()
            if (!bm.isRecycled) bm.recycle()
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): Bitmap? {
        if (undoStack.isEmpty()) return null
        redoStack.push(currentMask.copy(Bitmap.Config.ARGB_8888, true))
        val previous = undoStack.pop()
        currentMask.recycle()
        currentMask = previous.copy(Bitmap.Config.ARGB_8888, true)
        return currentMask
    }

    fun redo(): Bitmap? {
        if (redoStack.isEmpty()) return null
        undoStack.push(currentMask.copy(Bitmap.Config.ARGB_8888, true))
        val next = redoStack.pop()
        currentMask.recycle()
        currentMask = next.copy(Bitmap.Config.ARGB_8888, true)
        return currentMask
    }

    fun applyStroke(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        brushRadius: Float,
        isEraser: Boolean
    ) {
        val canvas = Canvas(currentMask)
        brushPaint.strokeWidth = brushRadius * 2f
        if (isEraser) {
            brushPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            brushPaint.color = Color.TRANSPARENT
        } else {
            brushPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            brushPaint.color = Color.WHITE
        }

        canvas.drawLine(fromX, fromY, toX, toY, brushPaint)
        brushPaint.xfermode = null
    }

    fun invertMask(): Bitmap {
        pushHistory()
        val width = currentMask.width
        val height = currentMask.height
        val pixels = IntArray(width * height)
        currentMask.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val a = Color.alpha(pixels[i])
            val invertedA = 255 - a
            pixels[i] = Color.argb(invertedA, 255, 255, 255)
        }
        currentMask.setPixels(pixels, 0, width, 0, 0, width, height)
        return currentMask
    }

    fun adjustThreshold(threshold: Float): Bitmap {
        pushHistory()
        val width = currentMask.width
        val height = currentMask.height
        val pixels = IntArray(width * height)
        currentMask.getPixels(pixels, 0, width, 0, 0, width, height)
        val cutoff = (threshold * 255).toInt().coerceIn(1, 254)

        for (i in pixels.indices) {
            val a = Color.alpha(pixels[i])
            val newA = if (a >= cutoff) 255 else 0
            pixels[i] = Color.argb(newA, 255, 255, 255)
        }
        currentMask.setPixels(pixels, 0, width, 0, 0, width, height)
        return currentMask
    }

    fun resetTo(initialBitmap: Bitmap) {
        pushHistory()
        currentMask.recycle()
        currentMask = initialBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun destroy() {
        if (!currentMask.isRecycled) currentMask.recycle()
        while (undoStack.isNotEmpty()) {
            val bm = undoStack.pop()
            if (!bm.isRecycled) bm.recycle()
        }
        while (redoStack.isNotEmpty()) {
            val bm = redoStack.pop()
            if (!bm.isRecycled) bm.recycle()
        }
    }
}
