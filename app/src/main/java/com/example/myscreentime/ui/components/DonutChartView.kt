package com.example.myscreentime.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.GRAY
    }

    private var data: List<Segment> = emptyList()
    private var centerText: String = ""
    private var subText: String = "Screen time"
    private var comparisonText: String = ""
    private var comparisonColor: Int = Color.GRAY

    private val rect = RectF()
    private val strokeWidthRatio = 0.1f

    data class Segment(val proportion: Float, val color: Int)

    fun setData(segments: List<Segment>, totalTime: String, comparison: String, compColor: Int) {
        this.data = segments
        this.centerText = totalTime
        this.comparisonText = comparison
        this.comparisonColor = compColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val size = min(width, height).toFloat()
        val stroke = size * strokeWidthRatio
        paint.strokeWidth = stroke
        
        val margin = stroke / 2f + 4f
        rect.set(margin, margin, size - margin, size - margin)

        // Draw background track
        paint.color = Color.parseColor("#F5F7FA")
        canvas.drawArc(rect, 0f, 360f, false, paint)

        var startAngle = -90f
        data.forEach { segment ->
            paint.color = segment.color
            val sweepAngle = segment.proportion * 360f
            if (sweepAngle > 0.5f) { // Only draw visible segments
                canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
                startAngle += sweepAngle
            }
        }

        // Draw Center Text
        textPaint.textSize = size * 0.15f
        textPaint.isFakeBoldText = true
        canvas.drawText(centerText, width / 2f, height / 2.2f, textPaint)

        subTextPaint.textSize = size * 0.055f
        canvas.drawText(subText, width / 2f, height / 2.2f + (size * 0.07f), subTextPaint)

        subTextPaint.textSize = size * 0.07f
        subTextPaint.color = comparisonColor
        canvas.drawText(comparisonText, width / 2f, height / 2.2f + (size * 0.16f), subTextPaint)
        
        subTextPaint.color = Color.parseColor("#8A94A6")
        subTextPaint.textSize = size * 0.05f
        canvas.drawText("vs yesterday", width / 2f, height / 2.2f + (size * 0.22f), subTextPaint)
    }
}
