package com.ivarna.finalbenchmark2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val border = BorderStroke(1.dp, borderColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = elevation,
            border = border
        ) {
            GlassCardContent(content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = elevation,
            border = border
        ) {
            GlassCardContent(content)
        }
    }
}

@Composable
private fun GlassCardContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        content()
    }
}
