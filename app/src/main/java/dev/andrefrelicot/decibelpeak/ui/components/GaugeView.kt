package dev.andrefrelicot.decibelpeak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelGreen
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelOrange
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelRed
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelYellow

@Composable
fun CircularGaugeView(
    value: Double,
    isRecording: Boolean = true,
    modifier: Modifier = Modifier
) {
    val minValue = 20.0
    val maxValue = 130.0
    val normalizedValue = ((value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0)
    
    val animatedValue by animateFloatAsState(
        targetValue = if (isRecording) normalizedValue.toFloat() else 0f,
        animationSpec = tween(durationMillis = 200), // Match iOS 0.2s
        label = "gaugeValue"
    )

    BoxWithConstraints(modifier = modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val containerSizeDp = androidx.compose.ui.unit.min(maxWidth, maxHeight)
        val containerSize = with(density) { containerSizeDp.toPx() }
        val gaugeSize = containerSize * 0.75f
        val gaugeRadius = gaugeSize / 2f
        
        // Gauge Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = min(size.width, size.height)
            // Ensure we work with the same size basis, though fillMaxSize should match containerSize roughly
            // Center is always center of canvas
            
            val strokeWidth = gaugeSize * 0.11f
            val activeStrokeWidth = gaugeSize * 0.125f
            
            val startAngle = 135f
            val sweepAngle = 270f
            
            // Absolute Gradient
            val mixColor = Color(0xFFFF5200) // Mix of Orange and Red for the 0-degree crossover

            val gradientBrush = Brush.sweepGradient(
                0.0f to mixColor,
                0.125f to DecibelRed,
                0.375f to DecibelGreen,
                0.625f to DecibelYellow,
                0.875f to DecibelOrange,
                1.0f to mixColor,
                center = center
            )
            
            val backgroundGradientBrush = Brush.sweepGradient(
                0.0f to mixColor.copy(alpha = 0.3f),
                0.125f to DecibelRed.copy(alpha = 0.3f),
                0.375f to DecibelGreen.copy(alpha = 0.3f),
                0.625f to DecibelYellow.copy(alpha = 0.3f),
                0.875f to DecibelOrange.copy(alpha = 0.3f),
                1.0f to mixColor.copy(alpha = 0.3f),
                center = center
            )

            // Background Arc
            drawArc(
                brush = backgroundGradientBrush,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(gaugeSize - strokeWidth, gaugeSize - strokeWidth),
                topLeft = center - Offset((gaugeSize - strokeWidth) / 2, (gaugeSize - strokeWidth) / 2)
            )

            // Active Arc
            if (animatedValue > 0) {
                 drawArc(
                    brush = gradientBrush,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedValue,
                    useCenter = false,
                    style = Stroke(width = activeStrokeWidth, cap = StrokeCap.Round),
                    size = Size(gaugeSize - strokeWidth, gaugeSize - strokeWidth),
                    topLeft = center - Offset((gaugeSize - strokeWidth) / 2, (gaugeSize - strokeWidth) / 2)
                )
            }

            // Center Glow (Radial Gradient)
            if (animatedValue > 0) {
                val glowColor = getDecibelColor(value).copy(alpha = 0.3f)
                val glowRadius = gaugeSize * 0.5f // ~140/280 of original
                
                val glowBrush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = glowRadius
                )
                
                drawCircle(
                    brush = glowBrush,
                    radius = glowRadius,
                    center = center
                )
            }

            // Ticks
            // iOS: tickOffset = size * 0.55. gaugeRadius = size * 0.5.
            // So ticks start at 0.55 * gaugeSize from center.
            val tickStartRadius = gaugeSize * 0.55f
            val tickLengthSmall = gaugeSize * 0.036f
            val tickLengthLarge = gaugeSize * 0.054f
            
            for (i in 0..11) {
                val angle = startAngle + (i.toFloat() / 11f) * sweepAngle
                val isLarge = i % 3 == 0
                val length = if (isLarge) tickLengthLarge else tickLengthSmall
                val width = if (isLarge) gaugeSize * 0.011f else gaugeSize * 0.0036f
                
                rotate(degrees = angle + 90f, pivot = center) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = center + Offset(0f, -tickStartRadius),
                        end = center + Offset(0f, -(tickStartRadius + length)),
                        strokeWidth = width,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Scale Labels
        val scaleLabels = listOf(20, 40, 60, 80, 100, 120)
        // iOS: labelOffset = size * 0.66.
        // Distance from center = 0.66 * gaugeSize.
        // We use padding from top to position them.
        // Box is size of container. Center is containerSize / 2.
        // Padding = (containerSize / 2) - (0.66 * gaugeSize)
        val labelRadius = gaugeSize * 0.63f
        val labelPadding = (containerSize - labelRadius * 2) / 2
        
        Box(modifier = Modifier.fillMaxSize()) {
             scaleLabels.forEach { db ->
                 val normalizedDb = (db - 20.0) / 110.0
                 val angle = 135.0 + normalizedDb * 270.0
                 
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .rotate(angle.toFloat() + 90f), // Rotate to align with tick
                     contentAlignment = Alignment.TopCenter
                 ) {
                     Text(
                         text = "$db",
                         color = Color.Gray,
                         fontSize = (gaugeSize * 0.043f / density.density).sp, // Scale font size
                         modifier = Modifier
                             .padding(top = with(density) { labelPadding.toDp() }) 
                             .rotate(-(angle.toFloat() + 90f)) // Counter-rotate text to keep it upright
                     )
                 }
             }
        }

        // Center Text
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording && value > 0) {
                AnimatedNumber(
                    number = value.toInt(),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = (gaugeSize * 0.257f / density.density).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = getDecibelColor(value)
                )
            } else {
                Text(
                    text = "–",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = (gaugeSize * 0.257f / density.density).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "dB",
                color = Color.Gray,
                fontSize = (gaugeSize * 0.086f / density.density).sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = getDecibelDescription(value),
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = (gaugeSize * 0.05f / density.density).sp
            )
        }
    }
}

fun getDecibelDescription(value: Double): String {
    return when {
        value < 30 -> "Very Quiet"
        value < 50 -> "Quiet"
        value < 60 -> "Moderate"
        value < 70 -> "Normal Conversation"
        value < 80 -> "Loud"
        value < 90 -> "Very Loud"
        value < 100 -> "Extremely Loud"
        value < 110 -> "Dangerous"
        value < 130 -> "Painful"
        else -> "Threshold of Pain"
    }
}

// iOS system blue color
private val DecibelBlue = Color(0xFF007AFF)

fun getDecibelColor(value: Double): Color {
    return when {
        value < 40 -> DecibelBlue    // Blue: 0-40 dB (matching iOS)
        value < 60 -> DecibelGreen   // Green: 40-60 dB
        value < 80 -> DecibelYellow  // Yellow: 60-80 dB
        value < 100 -> DecibelOrange // Orange: 80-100 dB
        else -> DecibelRed           // Red: 100+ dB
    }
}
