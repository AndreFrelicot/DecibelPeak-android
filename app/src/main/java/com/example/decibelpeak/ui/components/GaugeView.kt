package com.example.decibelpeak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import com.example.decibelpeak.ui.theme.DecibelGreen
import com.example.decibelpeak.ui.theme.DecibelOrange
import com.example.decibelpeak.ui.theme.DecibelRed
import com.example.decibelpeak.ui.theme.DecibelYellow
import kotlin.math.min

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

    Box(modifier = modifier) {
        // Gauge Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = min(size.width, size.height)
            val strokeWidth = size * 0.11f
            val activeStrokeWidth = size * 0.125f
            
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
                size = Size(size - strokeWidth, size - strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )

            // Active Arc
            if (animatedValue > 0) {
                 drawArc(
                    brush = gradientBrush,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedValue,
                    useCenter = false,
                    style = Stroke(width = activeStrokeWidth, cap = StrokeCap.Round),
                    size = Size(size - strokeWidth, size - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            }

            // Center Glow (Radial Gradient)
            if (animatedValue > 0) {
                val glowColor = getDecibelColor(value).copy(alpha = 0.3f)
                val glowRadius = size * 0.5f // ~140/280 of original
                
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
            val tickRadius = size / 2 - strokeWidth * 1.5f
            val tickLengthSmall = size * 0.036f
            val tickLengthLarge = size * 0.054f
            
            for (i in 0..11) {
                val angle = startAngle + (i.toFloat() / 11f) * sweepAngle
                val isLarge = i % 3 == 0
                val length = if (isLarge) tickLengthLarge else tickLengthSmall
                val width = if (isLarge) size * 0.011f else size * 0.0036f
                
                rotate(degrees = angle + 90f, pivot = center) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = center + Offset(0f, -tickRadius),
                        end = center + Offset(0f, -tickRadius + length),
                        strokeWidth = width,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Scale Labels
        val scaleLabels = listOf(20, 40, 60, 80, 100, 120)
        Box(modifier = Modifier.fillMaxSize()) {
             scaleLabels.forEach { db ->
                 val normalizedDb = (db - 20.0) / 110.0
                 val angle = 135.0 + normalizedDb * 270.0
                 val radians = Math.toRadians(angle - 90.0) // -90 because 0 is right in trig, but we want 0 up? No, 0 is right.
                 // Actually, let's use the rotation approach which is easier for alignment
                 
                 // We want to position the text at a certain radius
                 // Radius should be outside the gauge? No, iOS screenshot shows them outside.
                 // iOS code: .offset(y: -labelOffset) .rotationEffect(...)
                 // This means they rotate the whole view.
                 
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .rotate(angle.toFloat() + 90f), // Rotate to align with tick
                     contentAlignment = Alignment.TopCenter
                 ) {
                     Text(
                         text = "$db",
                         color = Color.Gray,
                         fontSize = 12.sp,
                         modifier = Modifier
                             .padding(top = 10.dp) // Adjust padding to push it out/in
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
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = getDecibelColor(value)
                )
            } else {
                Text(
                    text = "–",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "dB",
                color = Color.Gray,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = getDecibelDescription(value),
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 14.sp
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

fun getDecibelColor(value: Double): Color {
    return when {
        value < 60 -> DecibelGreen
        value < 85 -> DecibelYellow
        value < 100 -> DecibelOrange
        else -> DecibelRed
    }
}
