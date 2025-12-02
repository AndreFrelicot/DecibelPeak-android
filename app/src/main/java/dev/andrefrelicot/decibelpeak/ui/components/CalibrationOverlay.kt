package dev.andrefrelicot.decibelpeak.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import dev.andrefrelicot.decibelpeak.data.CalibrationManager
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelGreen
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelOrange
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelRed
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelYellow
import kotlinx.coroutines.flow.SharedFlow

// iOS system blue color (matching gauge)
private val DecibelBlue = Color(0xFF007AFF)

/**
 * Calibration overlay that displays on top of the visualization zone.
 * Shows a slider for adjusting the calibration offset with cancel/validate buttons
 * positioned at top-left and top-right corners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationOverlay(
    currentOffset: Double,
    onOffsetChange: (Double) -> Unit,
    onCancel: () -> Unit,
    onValidate: () -> Unit,
    hapticFeedbackEvent: SharedFlow<Unit>? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Handle haptic feedback events
    LaunchedEffect(hapticFeedbackEvent) {
        hapticFeedbackEvent?.collect {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A26).copy(alpha = 0.95f),
                        Color(0xFF0D0D1A).copy(alpha = 0.95f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        // Cancel button - top left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = 0.2f))
                .border(1.dp, Color.Red.copy(alpha = 0.5f), CircleShape)
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }

        // Validate button - top right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(DecibelGreen.copy(alpha = 0.2f))
                .border(1.dp, DecibelGreen.copy(alpha = 0.5f), CircleShape)
                .clickable { onValidate() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Validate",
                tint = DecibelGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        // Center content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Calibration",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current value display with gradient color
            Text(
                text = formatOffset(currentOffset),
                color = getCalibrationColor(currentOffset),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${CalibrationManager.MIN_OFFSET.toInt()} dB",
                    color = DecibelBlue,
                    fontSize = 12.sp
                )
                Text(
                    text = "+${CalibrationManager.MAX_OFFSET.toInt()} dB",
                    color = DecibelRed,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Slider with gradient track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                // Gradient track background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    DecibelBlue,
                                    DecibelGreen,
                                    DecibelYellow,
                                    DecibelOrange,
                                    DecibelRed
                                )
                            )
                        )
                )

                // Slider (with transparent track to show gradient underneath)
                Slider(
                    value = currentOffset.toFloat(),
                    onValueChange = { newValue ->
                        // Round to nearest 0.5 step
                        val rounded = (newValue / CalibrationManager.STEP).let {
                            kotlin.math.round(it) * CalibrationManager.STEP
                        }
                        onOffsetChange(rounded)
                    },
                    valueRange = CalibrationManager.MIN_OFFSET.toFloat()..CalibrationManager.MAX_OFFSET.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Format the offset value for display.
 * Shows + sign for positive values, no sign for zero.
 */
private fun formatOffset(offset: Double): String {
    val sign = when {
        offset > 0 -> "+"
        else -> ""
    }
    return "$sign${String.format("%.1f", offset)} dB"
}

/**
 * Get color based on offset value using the gauge gradient.
 * Maps -20 to +20 range to Blue → Green → Yellow → Orange → Red
 */
private fun getCalibrationColor(offset: Double): Color {
    // Normalize offset from [-20, +20] to [0, 1]
    val normalized = ((offset - CalibrationManager.MIN_OFFSET) /
        (CalibrationManager.MAX_OFFSET - CalibrationManager.MIN_OFFSET)).coerceIn(0.0, 1.0)

    // Map to colors: Blue(0) → Green(0.25) → Yellow(0.5) → Orange(0.75) → Red(1.0)
    return when {
        normalized < 0.25 -> {
            // Blue to Green
            val t = (normalized / 0.25).toFloat()
            lerpColor(DecibelBlue, DecibelGreen, t)
        }
        normalized < 0.5 -> {
            // Green to Yellow
            val t = ((normalized - 0.25) / 0.25).toFloat()
            lerpColor(DecibelGreen, DecibelYellow, t)
        }
        normalized < 0.75 -> {
            // Yellow to Orange
            val t = ((normalized - 0.5) / 0.25).toFloat()
            lerpColor(DecibelYellow, DecibelOrange, t)
        }
        else -> {
            // Orange to Red
            val t = ((normalized - 0.75) / 0.25).toFloat()
            lerpColor(DecibelOrange, DecibelRed, t)
        }
    }
}

/**
 * Linear interpolation between two colors.
 */
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}
