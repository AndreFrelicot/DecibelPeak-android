package com.example.decibelpeak.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedNumber(
    number: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.White
) {
    val numberString = number.toString()
    
    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        numberString.forEachIndexed { index, char ->
            // Use a fixed width for digits to prevent layout jitter
            // The width needs to be wide enough for the widest digit (usually '0' or '8')
            // We can estimate this or use a monospaced font, but let's try a Box with fixed width.
            // For 80sp font, a digit is roughly 40-50dp.
            
            Box(
                modifier = Modifier.width(50.dp), // Approximate width for 80sp
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height / 2 } + fadeIn() + scaleIn(initialScale = 0.5f))
                                .togetherWith(
                                    slideOutVertically { height -> -height / 2 } + fadeOut() + scaleOut(targetScale = 1.5f)
                                )
                        } else {
                            (slideInVertically { height -> -height / 2 } + fadeIn() + scaleIn(initialScale = 0.5f))
                                .togetherWith(
                                    slideOutVertically { height -> height / 2 } + fadeOut() + scaleOut(targetScale = 1.5f)
                                )
                        }.using(
                            androidx.compose.animation.SizeTransform(clip = false)
                        )
                    },
                    label = "char"
                ) { targetChar ->
                    Text(
                        text = targetChar.toString(),
                        style = style,
                        color = color,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
