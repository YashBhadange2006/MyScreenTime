package com.example.myscreentime.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarData(
    val label: String,
    val value: Float
)

@Composable
fun ActivityBarChartCard(
    title: String,
    data: List<BarData>,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Text(
                text = "Last 7 days (minutes)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActivityBarChart(
                data = data,
                gradientColors = gradientColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

@Composable
fun ActivityBarChart(
    data: List<BarData>,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val textColor = Color.Gray
    val gridColor = Color.LightGray.copy(alpha = 0.3f)

    // Animate the maximum value for smooth grid transitions
    val maxValTarget = data.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    val animatedMaxVal by animateFloatAsState(
        targetValue = maxValTarget,
        animationSpec = tween(durationMillis = 500),
        label = "MaxValAnimation"
    )

    // Create animated states for each bar's value
    val animatedValues = data.map { barData ->
        animateFloatAsState(
            targetValue = barData.value,
            animationSpec = tween(durationMillis = 500),
            label = "BarValueAnimation"
        )
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        
        val bottomPadding = 40.dp.toPx()
        val topPadding = 40.dp.toPx()
        val sidePadding = 40.dp.toPx()
        
        val chartHeight = height - bottomPadding - topPadding
        val chartWidth = width - sidePadding
        
        val barWidth = (chartWidth / barCount) * 0.6f
        val spacing = (chartWidth / barCount) * 0.4f

        // Draw Y-axis grid lines
        val gridLevels = 4
        for (i in 0..gridLevels) {
            val y = height - bottomPadding - (chartHeight * i / gridLevels)
            val gridVal = (animatedMaxVal * i / gridLevels).toInt()
            
            drawLine(
                color = gridColor,
                start = Offset(sidePadding, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            
            drawContext.canvas.nativeCanvas.drawText(
                gridVal.toString(),
                5f,
                y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = 10.sp.toPx()
                }
            )
        }

        // Draw Bars
        data.forEachIndexed { index, barData ->
            val x = sidePadding + (index * (barWidth + spacing)) + spacing / 2
            val animatedValue = animatedValues[index].value
            val barHeight = if (animatedMaxVal > 0) (animatedValue / animatedMaxVal) * chartHeight else 0f
            val y = height - bottomPadding - barHeight
            
            // Draw gray background bar
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.1f),
                topLeft = Offset(x, height - bottomPadding - chartHeight),
                size = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )

            drawRoundRect(
                brush = Brush.verticalGradient(gradientColors),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            
            // Value on top
            if (animatedValue > 0.5f) { // Only show if value is significant
                drawContext.canvas.nativeCanvas.drawText(
                    animatedValue.toInt().toString(),
                    x + barWidth / 2,
                    y - 12.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = Color.Black.toArgb()
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                )
            }
            
            // X-axis label
            drawContext.canvas.nativeCanvas.drawText(
                barData.label,
                x + barWidth / 2,
                height - 10.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textColor.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
