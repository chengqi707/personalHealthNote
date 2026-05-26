package com.chengqi.personalhealthnote.widget

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class TouchImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val last = PointF()
    private var mode = Mode.NONE
    private var minScale = 1f
    private var maxScale = 3f
    private var saveScale = 1f
    private var origWidth = 0f
    private var origHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    private enum class Mode { NONE, DRAG, ZOOM }

    init {
        super.setScaleType(ScaleType.MATRIX)
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, DoubleTapListener())
        imageMatrix = matrix
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = Mode.ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = saveScale * scaleFactor
            if (newScale < maxScale && newScale > minScale) {
                saveScale = newScale
                val scale: Float
                if (saveScale > maxScale) {
                    scale = maxScale / saveScale * scaleFactor
                    saveScale = maxScale
                } else if (saveScale < minScale) {
                    scale = minScale / saveScale * scaleFactor
                    saveScale = minScale
                } else {
                    scale = scaleFactor
                }
                matrix.postScale(scale, scale, detector.focusX, detector.focusY)
                fixTranslation()
                imageMatrix = matrix
            }
            return true
        }
    }

    private inner class DoubleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale: Float
            if (saveScale < maxScale / 2) {
                targetScale = maxScale
            } else {
                targetScale = minScale
            }
            val scaleFactor = targetScale / saveScale
            matrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
            saveScale = targetScale
            fixTranslation()
            imageMatrix = matrix
            return true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (saveScale == 1f) {
            fitImageToView()
        }
    }

    private fun fitImageToView() {
        if (drawable == null) return
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        if (drawableWidth == 0 || drawableHeight == 0) return

        origWidth = drawableWidth.toFloat()
        origHeight = drawableHeight.toFloat()

        val scaleX = viewWidth.toFloat() / origWidth
        val scaleY = viewHeight.toFloat() / origHeight
        val scale = scaleX.coerceAtMost(scaleY)

        minScale = scale
        saveScale = scale

        matrix.setScale(scale, scale)
        val redundantYSpace = viewHeight.toFloat() - scale * origHeight
        val redundantXSpace = viewWidth.toFloat() - scale * origWidth
        matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
        origWidth = drawableWidth.toFloat()
        origHeight = drawableHeight.toFloat()
        imageMatrix = matrix
    }

    private fun fixTranslation() {
        matrix.getValues(floatArrayOf())
        val values = FloatArray(9)
        matrix.getValues(values)
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val currentScale = values[Matrix.MSCALE_X]
        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), currentScale * origWidth)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), currentScale * origHeight)
        if (fixTransX != 0f || fixTransY != 0f) {
            matrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        val point = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(point)
                mode = Mode.DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && saveScale > minScale * 1.01f) {
                    val dx = event.x - last.x
                    val dy = event.y - last.y
                    matrix.postTranslate(dx, dy)
                    fixTranslation()
                    imageMatrix = matrix
                    last.set(point)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
            }
        }
        return true
    }
}
