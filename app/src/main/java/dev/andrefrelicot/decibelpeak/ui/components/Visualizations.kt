package dev.andrefrelicot.decibelpeak.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import dev.andrefrelicot.decibelpeak.model.TimestampedDbValue
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelGreen
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelOrange
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelRed
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelYellow
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.delay
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
        if (waterfallData.isEmpty()) return@Canvas

        val cols = waterfallData[0].size

        // iOS matching: fixed row count based on orientation - no more stretching!
        val isLandscape = size.width > size.height
        val maxDisplayRows = if (isLandscape) 60 else 80

        val cellWidth = size.width / cols
        val cellHeight = size.height / maxDisplayRows  // Always use max, never actual count

        for (rowIndex in 0 until maxDisplayRows) {
            if (rowIndex >= waterfallData.size) break

            val row = waterfallData[rowIndex]
            val y = rowIndex * cellHeight

            row.forEachIndexed { colIndex, amplitude ->
                val x = colIndex * cellWidth
                val colorWithOpacity = getWaterfallColorWithOpacity(colIndex, cols, amplitude)

                drawRect(
                    color = colorWithOpacity,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth, cellHeight)
                )
            }
        }
    }
}

// ... (SpectrumView and FFTCircularView remain unchanged)

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

/**
 * Smooth scrolling dB curve view using time-based positioning (matching iOS implementation)
 */
@Composable
fun DbCurveView(
    timestampedDbHistory: List<TimestampedDbValue>,
    dbHistory: List<Double>, // For color calculation
    modifier: Modifier = Modifier
) {
    // Continuous animation frame for smooth scrolling
    var currentNanoTime by remember { mutableLongStateOf(System.nanoTime()) }

    // Update time continuously for smooth animation (~60 FPS)
    LaunchedEffect(Unit) {
        while (true) {
            currentNanoTime = System.nanoTime()
            delay(16L) // ~60 FPS refresh
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Constants matching iOS
        val timeWindowSeconds = 10.0
        val targetOffscreenSeconds = 0.3
        val pixelsPerSecond = width / timeWindowSeconds.toFloat()
        val startOffset = (targetOffscreenSeconds * pixelsPerSecond).toFloat()

        // Grid lines and labels (matching iOS)
        val dbLevels = listOf(30, 60, 90, 120)

        // Text paint for dB labels
        val textPaint = Paint().apply {
            color = android.graphics.Color.argb(179, 255, 255, 255) // white with 0.7 opacity
            textSize = 10f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        dbLevels.forEach { db ->
            val y = height * (1f - (db - 20f) / 110f)
            // Draw grid line
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5f
            )
            // Draw dB label (matching iOS: x=30, y=yPosition-8)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    "$db dB",
                    30f,
                    y - 8f,
                    textPaint
                )
            }
        }

        if (timestampedDbHistory.size < 2) return@Canvas

        // Collect visible points with time-based x positions
        data class VisiblePoint(val x: Float, val y: Float)
        val visiblePoints = mutableListOf<VisiblePoint>()

        for (dataPoint in timestampedDbHistory) {
            val dataAgeNanos = currentNanoTime - dataPoint.timestamp
            val dataAgeSeconds = dataAgeNanos / 1_000_000_000.0

            // Calculate x position based on time (smooth continuous scrolling)
            val x = width + startOffset - (dataAgeSeconds * pixelsPerSecond).toFloat()

            // Calculate y position
            val normalizedDb = ((dataPoint.value - 20.0) / 110.0).coerceIn(0.0, 1.0)
            val y = height * (1f - normalizedDb.toFloat())

            // Check appearance progress for smooth entry (75ms fade-in)
            val timeSinceAppearance = currentNanoTime - dataPoint.appearanceTime
            val appearanceProgress = (timeSinceAppearance / 75_000_000.0).coerceIn(0.0, 1.0)

            // Only include points that have started appearing and are within bounds
            if (appearanceProgress > 0 && x >= -100f && x <= width + startOffset + 50f) {
                visiblePoints.add(VisiblePoint(x, y))
            }
        }

        if (visiblePoints.isEmpty()) return@Canvas

        val lastDb = dbHistory.lastOrNull() ?: 50.0
        val color = getDecibelColor(lastDb)

        // Clip drawing to canvas bounds for smooth edge transitions
        clipRect {
            // Draw gradient fill
            val fillPath = Path().apply {
                val leftmostX = visiblePoints.minOfOrNull { it.x } ?: 0f
                moveTo(leftmostX, height)

                for (point in visiblePoints) {
                    lineTo(point.x, point.y)
                }

                val rightmostX = visiblePoints.maxOfOrNull { it.x } ?: width
                lineTo(rightmostX, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.1f)),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw curve line
            val linePath = Path().apply {
                visiblePoints.forEachIndexed { index, point ->
                    if (index == 0) {
                        moveTo(point.x, point.y)
                    } else {
                        lineTo(point.x, point.y)
                    }
                }
            }

            drawPath(
                path = linePath,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

private fun getWaterfallColorWithOpacity(index: Int, totalCount: Int, amplitude: Float): Color {
    val ratio = index.toFloat() / max(totalCount - 1, 1)

    // Double-log normalization for brightness (matching iOS exactly)
    // amplitude is already dB-normalized (0-1), apply another log for perceptual uniformity
    val logAmplitude = log10(max(0.001f, amplitude) + 0.001f) + 3.0f
    val normalizedAmplitude = (logAmplitude / 3.0f).coerceIn(0.0f, 1.0f)

    // iOS matching: Hue from Red (360°) -> Yellow-green (~72°) across frequency spectrum
    val hue = (1.0f - ratio * 0.8f) * 360f
    val saturation = 0.8f + normalizedAmplitude * 0.2f
    val brightness = 0.3f + normalizedAmplitude * 0.7f

    // iOS exact opacity formula: uses raw amplitude (already dB-normalized 0-1)
    val opacity = if (amplitude == 0f) 0.02f else (amplitude * 1.2f + 0.1f).coerceIn(0.1f, 1.0f)

    val baseColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    return baseColor.copy(alpha = opacity)
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
