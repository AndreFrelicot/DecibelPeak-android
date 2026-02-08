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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import dev.andrefrelicot.decibelpeak.model.DbPeakDataPoint
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

        // Gain boost for visibility (matching Spectrum visualization)
        val isPortrait = height > width
        val amplitudeScale = if (isPortrait) 10f else 6.25f

        val path = Path()
        val stepX = width / (samples.size - 1)

        samples.forEachIndexed { index, sample ->
            val x = index * stepX
            val scaledSample = (sample * amplitudeScale).coerceIn(-1f, 1f)
            val y = centerY + (scaledSample * height / 2)
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

        // Gain boost for visibility (higher values = taller bars, more color)
        val isPortrait = height > width
        val amplitudeScale = if (isPortrait) 10f else 6.25f

        for (i in 0 until barCount) {
            val sampleIndex = (i * samples.size) / barCount
            val rawSample = if (sampleIndex < samples.size) kotlin.math.abs(samples[sampleIndex]) else 0f

            // Apply amplitude scaling (matching iOS WaveformView behavior)
            val sample = (rawSample * amplitudeScale).coerceAtMost(1f)
            val barHeight = min(sample * height, height).coerceAtLeast(4f)

            val color = getColorForLevel(sample)

            // Match iOS: solid color at bottom, fading to 60% opacity at top
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.6f), color),
                    startY = height - barHeight,  // Top of bar: 60% opacity
                    endY = height                  // Bottom of bar: solid color
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

/**
 * Static dB peak view showing 60-second window around the session peak (matching iOS)
 */
@Composable
fun DbPeakView(
    dbPeakData: List<DbPeakDataPoint>,
    dbPeakValue: Double,
    dbPeakTimeMillis: Long,
    onNavigatePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleTimeWindowMs = 15_000f  // 15 seconds visible at a time
    val timeLabelHeightFraction = 0.12f  // reserve 12% of height for time labels below chart

    var scrollTimeOffsetMs by remember { mutableFloatStateOf(Float.MIN_VALUE) }  // MIN_VALUE = auto-center
    var dragStartOffsetMs by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var totalDragPx by remember { mutableFloatStateOf(0f) }

    // Auto-center when peak value changes
    LaunchedEffect(dbPeakValue) {
        if (!isDragging) {
            scrollTimeOffsetMs = Float.MIN_VALUE
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(dbPeakData, dbPeakValue) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        totalDragPx = 0f
                        val dataStartTime = dbPeakData.firstOrNull()?.timeMillis ?: 0L
                        val dataDuration = ((dbPeakData.lastOrNull()?.timeMillis ?: 0L) - dataStartTime).toFloat()
                        val maxOffset = max(0f, dataDuration - visibleTimeWindowMs)
                        val peakOffset = (dbPeakTimeMillis - dataStartTime).toFloat()
                        val autoOffset = max(0f, min(maxOffset, peakOffset - visibleTimeWindowMs / 2))
                        dragStartOffsetMs = if (scrollTimeOffsetMs == Float.MIN_VALUE) autoOffset else scrollTimeOffsetMs
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragPx += dragAmount
                        val pixelsPerMs = size.width / visibleTimeWindowMs
                        val timeDelta = -totalDragPx / pixelsPerMs
                        val dataStartTime = dbPeakData.firstOrNull()?.timeMillis ?: 0L
                        val dataDuration = ((dbPeakData.lastOrNull()?.timeMillis ?: 0L) - dataStartTime).toFloat()
                        val maxOffset = max(0f, dataDuration - visibleTimeWindowMs)
                        scrollTimeOffsetMs = max(0f, min(maxOffset, dragStartOffsetMs + timeDelta))
                    },
                    onDragEnd = {
                        isDragging = false
                        // At left edge and swiped right → navigate to previous visualization
                        if (dragStartOffsetMs <= 0.001f && totalDragPx > 150f) {
                            onNavigatePrevious()
                            scrollTimeOffsetMs = Float.MIN_VALUE
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val chartHeight = height * (1f - timeLabelHeightFraction)

        if (dbPeakData.size < 2) return@Canvas

        val dataStartTime = dbPeakData.first().timeMillis
        val dataEndTime = dbPeakData.last().timeMillis
        val dataDurationMs = (dataEndTime - dataStartTime).toFloat()
        val peakOffsetMs = (dbPeakTimeMillis - dataStartTime).toFloat()

        val maxOffset = max(0f, dataDurationMs - visibleTimeWindowMs)
        val autoOffset = max(0f, min(maxOffset, peakOffsetMs - visibleTimeWindowMs / 2))
        val clampedOffset = max(0f, min(maxOffset, if (scrollTimeOffsetMs == Float.MIN_VALUE) autoOffset else scrollTimeOffsetMs))

        val pixelsPerMs = width / visibleTimeWindowMs

        // Text paints
        val dbLabelPaint = Paint().apply {
            color = android.graphics.Color.argb(179, 255, 255, 255)
            textSize = 10f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val timeLabelPaint = Paint().apply {
            color = android.graphics.Color.argb(128, 255, 255, 255)
            textSize = 9f * density
            typeface = Typeface.create("monospace", Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val peakLabelPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Background grid lines
        val dbLevels = listOf(20, 40, 60, 80, 100, 120)
        dbLevels.forEach { db ->
            val y = chartHeight * (1f - (db - 20f) / 110f)
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5f
            )
        }

        // dB level labels
        listOf(30, 60, 90, 120).forEach { db ->
            val y = chartHeight * (1f - (db - 20f) / 110f)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("$db dB", 30f, y - 8f, dbLabelPaint)
            }
        }

        // Collect visible points
        data class PeakPoint(val x: Float, val y: Float)
        val visiblePoints = mutableListOf<PeakPoint>()

        for (point in dbPeakData) {
            val t = (point.timeMillis - dataStartTime).toFloat() - clampedOffset
            val x = t * pixelsPerMs
            if (x < -20f || x > width + 20f) continue
            val normalizedDb = ((point.value - 20.0) / 110.0).coerceIn(0.0, 1.0)
            val y = chartHeight * (1f - normalizedDb.toFloat())
            visiblePoints.add(PeakPoint(x, y))
        }

        if (visiblePoints.isEmpty()) return@Canvas

        val peakColor = getDecibelColor(dbPeakValue)

        clipRect(right = width, bottom = chartHeight) {
            // Gradient fill
            val fillPath = Path().apply {
                moveTo(visiblePoints.first().x, chartHeight)
                for (p in visiblePoints) { lineTo(p.x, p.y) }
                lineTo(visiblePoints.last().x, chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(peakColor.copy(alpha = 0.6f), peakColor.copy(alpha = 0.1f)),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Curve line
            val linePath = Path().apply {
                visiblePoints.forEachIndexed { i, p ->
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
            }
            drawPath(
                path = linePath,
                color = peakColor,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Peak marker vertical dashed line
            val peakX = (peakOffsetMs - clampedOffset) * pixelsPerMs
            if (peakX in 0f..width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(peakX, 0f),
                    end = Offset(peakX, chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )

                // Peak dot
                val peakNormDb = ((dbPeakValue - 20.0) / 110.0).coerceIn(0.0, 1.0)
                val peakY = chartHeight * (1f - peakNormDb.toFloat())
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(peakX, peakY)
                )

                // Peak value label with background
                val peakText = "${dbPeakValue.toInt()} dB"
                val textWidth = peakLabelPaint.measureText(peakText)
                val labelX = peakX.coerceIn(textWidth / 2 + 8f, width - textWidth / 2 - 8f)
                val labelY = 12f * density

                drawRoundRect(
                    color = peakColor.copy(alpha = 0.8f),
                    topLeft = Offset(labelX - textWidth / 2 - 6f, labelY - 10f * density),
                    size = Size(textWidth + 12f, 14f * density),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * density)
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(peakText, labelX, labelY, peakLabelPaint)
                }
            }
        }

        // Time labels below chart (aligned to 5-second intervals)
        val visibleStartMs = dataStartTime + clampedOffset.toLong()
        val visibleEndMs = visibleStartMs + visibleTimeWindowMs.toLong()
        val firstLabelMs = ((visibleStartMs / 5000L) + 1) * 5000L  // ceil to next 5s

        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        var labelMs = firstLabelMs
        while (labelMs <= visibleEndMs) {
            val x = (labelMs - visibleStartMs).toFloat() * pixelsPerMs
            if (x in 10f..(width - 10f)) {
                val timeStr = timeFormat.format(java.util.Date(labelMs))
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        timeStr, x, chartHeight + (height - chartHeight) * 0.6f, timeLabelPaint
                    )
                }
            }
            labelMs += 5000L
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
    // Thresholds lowered to match iOS visual output
    // Android raw PCM samples are typically lower than iOS
    return when {
        level < 0.15f -> DecibelGreen
        level < 0.35f -> DecibelYellow
        level < 0.55f -> DecibelOrange
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
