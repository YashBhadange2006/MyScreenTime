package com.example.myscreentime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin

data class RadarData(
    val label: String,
    val value: Float,
    val maxValue: Float
)

@Composable
fun RadarChart(
    data: List<RadarData>,
    modifier: Modifier = Modifier,
    fillColor: Color = Color(0xFF8E24AA).copy(alpha = 0.2f),
    strokeColor: Color = Color(0xFF8E24AA),
    gridColor: Color = Color.LightGray.copy(alpha = 0.5f),
    textColor: Color = Color.Gray
) {
    if (data.size < 3) return

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.8f
            val angleStep = (2 * Math.PI / data.size).toFloat()

            // Draw grid (rings)
            val gridLevels = 5
            for (i in 1..gridLevels) {
                val currentRadius = radius * (i.toFloat() / gridLevels)
                val gridPath = Path()
                for (j in data.indices) {
                    val angle = j * angleStep - Math.PI.toFloat() / 2
                    val x = center.x + currentRadius * cos(angle)
                    val y = center.y + currentRadius * sin(angle)
                    if (j == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()
                drawPath(gridPath, gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // Draw axes and labels
            data.forEachIndexed { index, radarData ->
                val angle = index * angleStep - Math.PI.toFloat() / 2
                val lineEnd = Offset(
                    center.x + radius * cos(angle),
                    center.y + radius * sin(angle)
                )
                drawLine(gridColor, center, lineEnd, strokeWidth = 1.dp.toPx())

                // Draw labels
                val labelRadius = radius + 20.dp.toPx()
                val labelX = center.x + labelRadius * cos(angle)
                val labelY = center.y + labelRadius * sin(angle)

                drawContext.canvas.nativeCanvas.drawText(
                    radarData.label,
                    labelX,
                    labelY,
                    android.graphics.Paint().apply {
                        color = textColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // Draw data polygon
            val dataPath = Path()
            data.forEachIndexed { index, radarData ->
                val angle = index * angleStep - Math.PI.toFloat() / 2
                val normalizedValue = if (radarData.maxValue > 0) radarData.value / radarData.maxValue else 0f
                val valueRadius = radius * normalizedValue
                val x = center.x + valueRadius * cos(angle)
                val y = center.y + valueRadius * sin(angle)
                if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            drawPath(dataPath, fillColor)
            drawPath(dataPath, strokeColor, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
