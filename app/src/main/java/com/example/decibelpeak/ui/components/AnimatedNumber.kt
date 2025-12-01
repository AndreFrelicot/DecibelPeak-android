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
    color: Color = Color.White,
    digitWidth: androidx.compose.ui.unit.Dp? = null // Optional custom digit width
) {
    val numberString = number.toString()

    // Calculate digit width based on font size if not provided
    // Approximate ratio: digit width ≈ fontSize * 0.6
    val calculatedWidth = digitWidth ?: (style.fontSize.value * 0.6f).dp

    Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        numberString.forEachIndexed { index, char ->
            Box(
                modifier = Modifier.width(calculatedWidth),
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
