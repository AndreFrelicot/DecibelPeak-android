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
import androidx.compose.ui.graphics.drawscope.Stroke
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
