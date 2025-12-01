package com.example.decibelpeak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SoundLevelIndicator(
    icon: ImageVector,
    label: String,
    range: String,
    color: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isActive) 1.1f else 1.0f, label = "scale")
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) color else Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier
                .size(48.dp)  // Doubled from default 24.dp
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isActive) Color.White else Color.Gray.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = range,
            color = if (isActive) Color.Gray else Color.Gray.copy(alpha = 0.3f),
            fontSize = 11.sp
        )
    }
}
