package com.example.decibelpeak.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.decibelpeak.ui.theme.DecibelGreen
import com.example.decibelpeak.ui.theme.DecibelOrange
import com.example.decibelpeak.ui.theme.DecibelRed
import com.example.decibelpeak.ui.theme.DecibelYellow
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

@Composable
fun WaveformView(
    samples: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val path = Path()
        val stepX = width / (samples.size - 1)
        
        samples.forEachIndexed { index, sample ->
            val x = index * stepX
            val y = centerY + (sample * height / 2) // Scale sample to view height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun FFTSpectrumView(
    frequencyBands: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidth = width / frequencyBands.size
        
        frequencyBands.forEachIndexed { index, amplitude ->
            val barHeight = (amplitude * height * 0.8f).coerceAtLeast(2f)
            val x = index * barWidth
            val y = height - barHeight
            
            val color = getColorForFrequency(index, frequencyBands.size, amplitude)
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.3f)),
                    startY = y,
                    endY = height
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth - 1f, barHeight)
            )
        }
    }
}

@Composable
fun FFTWaterfallView(
    waterfallData: List<List<Float>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val rows = waterfallData.size
        if (rows == 0) return@Canvas
        
        val cols = waterfallData[0].size
        val cellWidth = size.width / cols
        val cellHeight = size.height / rows
        
        waterfallData.forEachIndexed { rowIndex, row ->
            val y = rowIndex * cellHeight
            row.forEachIndexed { colIndex, amplitude ->
                val x = colIndex * cellWidth
                val color = getColorForFrequency(colIndex, cols, amplitude)
                val opacity = if (amplitude == 0f) 0.02f else (amplitude * 1.2f + 0.1f).coerceIn(0.1f, 1.0f)
                
                drawRect(
                    color = color.copy(alpha = opacity),
                    topLeft = Offset(x, y),
                    size = Size(cellWidth, cellHeight)
                )
            }
        }
    }
}

@Composable
fun SpectrumView(
    samples: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barCount = 40
        val barWidth = width / barCount
        
        for (i in 0 until barCount) {
            val sampleIndex = (i * samples.size) / barCount
            val sample = if (sampleIndex < samples.size) kotlin.math.abs(samples[sampleIndex]) else 0f
            val barHeight = min(sample * height, height).coerceAtLeast(4f)
            
            val color = getColorForLevel(sample)
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.6f)),
                    startY = height - barHeight,
                    endY = height
                ),
                topLeft = Offset(i * barWidth, height - barHeight),
                size = Size(barWidth - 2f, barHeight)
            )
        }
    }
}

@Composable
fun FFTCircularView(
    frequencyBands: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(size.width, size.height) / 2
        val count = frequencyBands.size
        
        // Background circles
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = radius * 0.25f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = radius * 0.5f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = radius * 0.75f,
            center = center,
            style = Stroke(width = 1f)
        )

        frequencyBands.forEachIndexed { index, amplitude ->
            val angle = (index.toFloat() / count) * 360f
            val barHeight = (amplitude * radius * 0.7f).coerceAtLeast(2f)
            val color = getColorForFrequency(index, count, amplitude)
            
            rotate(degrees = angle, pivot = center) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.2f)),
                        startY = centerY - radius * 0.6f - barHeight,
                        endY = centerY - radius * 0.6f
                    ),
                    topLeft = Offset(centerX - 1f, centerY - radius * 0.6f - barHeight),
                    size = Size(2f, barHeight)
                )
            }
        }
        
        // Center dot
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f)),
                center = center,
                radius = 4.dp.toPx()
            ),
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun DbCurveView(
    dbHistory: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Grid lines
        val dbLevels = listOf(30, 60, 90, 120)
        dbLevels.forEach { db ->
            val y = height * (1f - (db - 20f) / 110f)
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        if (dbHistory.size < 2) return@Canvas
        
        val path = Path()
        val stepX = width / 100f // Fixed 100 points window
        
        // Start from the right and go backwards
        val startIndex = max(0, dbHistory.size - 100)
        val visibleHistory = dbHistory.subList(startIndex, dbHistory.size)
        
        visibleHistory.forEachIndexed { index, db ->
            val x = width - (visibleHistory.size - 1 - index) * stepX
            val normalizedDb = ((db - 20.0) / 110.0).coerceIn(0.0, 1.0)
            val y = height * (1f - normalizedDb.toFloat())
            
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        val lastDb = dbHistory.lastOrNull() ?: 0.0
        val color = getDecibelColor(lastDb)
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Gradient fill
        path.lineTo(width, height)
        path.lineTo(width - (visibleHistory.size - 1) * stepX, height)
        path.close()
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.1f)),
                startY = 0f,
                endY = height
            )
        )
    }
}

private fun getColorForLevel(level: Float): Color {
    return when {
        level < 0.3f -> DecibelGreen
        level < 0.6f -> DecibelYellow
        level < 0.8f -> DecibelOrange
        else -> DecibelRed
    }
}

private fun getColorForFrequency(index: Int, totalCount: Int, amplitude: Float): Color {
    val ratio = index.toFloat() / totalCount
    return when {
        ratio < 0.2f -> Color.Blue.copy(alpha = 0.6f + amplitude * 0.4f)
        ratio < 0.4f -> Color(0xFF800080).copy(alpha = 0.6f + amplitude * 0.4f) // Purple
        ratio < 0.6f -> DecibelGreen.copy(alpha = 0.6f + amplitude * 0.4f)
        ratio < 0.8f -> DecibelYellow.copy(alpha = 0.6f + amplitude * 0.4f)
        else -> DecibelRed.copy(alpha = 0.6f + amplitude * 0.4f)
    }
}
