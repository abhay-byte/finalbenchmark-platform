package com.ivarna.finalbenchmark2.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    borderColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
    delayMillis: Int = 0,
    animationDuration: Int = 500,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(0.9f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                )
            )
        }
        
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (animationDuration * 0.8).toInt()
                )
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .alpha(alpha.value)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(), // Inherit width
            shape = shape,
            containerColor = containerColor,
            borderColor = borderColor,
            onClick = onClick,
            content = content
        )
    }
}
