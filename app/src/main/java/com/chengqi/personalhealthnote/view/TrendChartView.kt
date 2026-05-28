package com.chengqi.personalhealthnote.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.chengqi.personalhealthnote.R

/**
 * 折线趋势图自定义View
 * 支持单条线或双条线（如血压收缩/舒张）
 */
class TrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>()
    private val dataPoints2 = mutableListOf<Float>()
    private val labels = mutableListOf<String>()
    private var hasSecondLine = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#2196F3")
    }

    private val linePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#F44336")
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#E0E0E0")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.parseColor("#999999")
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2196F3")
    }

    private val dotPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F44336")
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#152196F3")
    }

    private var minVal = 0f
    private var maxVal = 100f

    fun setData(labels: List<String>, values: List<Float>) {
        this.labels.clear()
        this.labels.addAll(labels)
        this.dataPoints.clear()
        this.dataPoints.addAll(values)
        this.dataPoints2.clear()
        this.hasSecondLine = false
        calcRange()
        invalidate()
    }

    fun setDualData(labels: List<String>, values1: List<Float>, values2: List<Float>) {
        this.labels.clear()
        this.labels.addAll(labels)
        this.dataPoints.clear()
        this.dataPoints.addAll(values1)
        this.dataPoints2.clear()
        this.dataPoints2.addAll(values2)
        this.hasSecondLine = true
        calcRange()
        invalidate()
    }

    private fun calcRange() {
        val allValues = dataPoints + dataPoints2
        if (allValues.isEmpty()) {
            minVal = 0f
            maxVal = 100f
            return
        }
        minVal = allValues.min()
        maxVal = allValues.max()
        val range = maxVal - minVal
        if (range < 1f) {
            minVal -= 5f
            maxVal += 5f
        } else {
            minVal -= range * 0.1f
            maxVal += range * 0.1f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingLeft = 80f
        val paddingRight = 30f
        val paddingTop = 30f
        val paddingBottom = 60f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (chartWidth <= 0 || chartHeight <= 0) return

        // 绘制网格线和Y轴标签
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = paddingTop + chartHeight * i / gridCount
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)

            val value = maxVal - (maxVal - minVal) * i / gridCount
            canvas.drawText(String.format("%.1f", value), 0f, y + 10f, textPaint)
        }

        if (dataPoints.isEmpty()) return

        // 绘制折线
        val path = Path()
        val path2 = Path()
        val stepX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth

        for (i in dataPoints.indices) {
            val x = paddingLeft + stepX * i
            val y = paddingTop + chartHeight * (1 - (dataPoints[i] - minVal) / (maxVal - minVal))

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            canvas.drawCircle(x, y, 6f, dotPaint)

            // X轴标签（间隔显示避免重叠）
            if (dataPoints.size <= 6 || i % ((dataPoints.size / 5).coerceAtLeast(1)) == 0) {
                val label = if (i < labels.size) labels[i] else ""
                val shortLabel = if (label.length > 5) label.substring(5) else label
                canvas.drawText(shortLabel, x - 20f, height - 15f, textPaint)
            }
        }

        // 填充区域
        if (dataPoints.size > 1) {
            val fillPath = Path(path)
            fillPath.lineTo(paddingLeft + stepX * (dataPoints.size - 1), paddingTop + chartHeight)
            fillPath.lineTo(paddingLeft, paddingTop + chartHeight)
            fillPath.close()
            canvas.drawPath(fillPath, fillPaint)
        }

        canvas.drawPath(path, linePaint)

        // 第二条线
        if (hasSecondLine && dataPoints2.isNotEmpty()) {
            for (i in dataPoints2.indices) {
                val x = paddingLeft + stepX * i
                val y = paddingTop + chartHeight * (1 - (dataPoints2[i] - minVal) / (maxVal - minVal))

                if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                canvas.drawCircle(x, y, 6f, dotPaint2)
            }
            canvas.drawPath(path2, linePaint2)
        }
    }
}
