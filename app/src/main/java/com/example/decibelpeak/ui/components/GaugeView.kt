package com.example.decibelpeak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        animationSpec = tween(durationMillis = 300),
        label = "gaugeValue"
    )

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = min(size.width, size.height)
            val strokeWidth = size * 0.11f
            val activeStrokeWidth = size * 0.125f
            val startAngle = 135f
            val sweepAngle = 270f
            
            // Background Arc
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to DecibelGreen.copy(alpha = 0.3f),
                    0.33f to DecibelYellow.copy(alpha = 0.3f),
                    0.66f to DecibelOrange.copy(alpha = 0.3f),
                    1.0f to DecibelRed.copy(alpha = 0.3f),
                    center = center
                ),
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
                    brush = Brush.sweepGradient(
                        0.0f to DecibelGreen,
                        0.33f to DecibelYellow,
                        0.66f to DecibelOrange,
                        1.0f to DecibelRed,
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedValue,
                    useCenter = false,
                    style = Stroke(width = activeStrokeWidth, cap = StrokeCap.Round),
                    size = Size(size - strokeWidth, size - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
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
        
        // Center Text
        if (isRecording && value > 0) {
            Text(
                text = String.format("%.0f", value),
                color = getDecibelColor(value),
                fontSize = 80.sp, // Approximate, needs scaling logic
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = "dB",
                color = Color.Gray,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Center).padding(top = 80.dp)
            )
        }
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
