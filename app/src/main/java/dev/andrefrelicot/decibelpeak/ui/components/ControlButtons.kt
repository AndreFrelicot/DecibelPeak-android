package dev.andrefrelicot.decibelpeak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelGreen
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelOrange
import dev.andrefrelicot.decibelpeak.ui.theme.DecibelRed

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = if (isRecording) {
        // iOS Red to Orange
        val IosRed = Color(0xFFFF3B30)
        val IosOrange = Color(0xFFFF9500)
        Brush.horizontalGradient(listOf(IosRed, IosOrange))
    } else {
        // iOS Green to Red
        val IosGreen = Color(0xFF34C759)
        val IosRed = Color(0xFFFF3B30)
        Brush.horizontalGradient(listOf(IosGreen, IosRed))
    }

    val shadowColor = if (isRecording) DecibelRed else Color.Blue

    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(28.dp), spotColor = shadowColor)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(32.dp), // Total height 56dp (12+32+12)
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isRecording) "Stop Capture" else "Start Capture",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Circular control button for landscape mode (matching iOS)
 */
@Composable
fun LandscapeControlButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = if (isRecording) {
        // iOS Red to Orange for stop
        val IosRed = Color(0xFFFF3B30)
        val IosOrange = Color(0xFFFF9500)
        Brush.horizontalGradient(listOf(IosRed, IosOrange))
    } else {
        // iOS Green to Red for start
        val IosGreen = Color(0xFF34C759)
        val IosRed = Color(0xFFFF3B30)
        Brush.horizontalGradient(listOf(IosGreen, IosRed))
    }

    val shadowColor = if (isRecording) DecibelRed.copy(alpha = 0.4f) else Color.Blue.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(10.dp, CircleShape, spotColor = shadowColor)
            .clip(CircleShape)
            .background(gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop" else "Start",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
