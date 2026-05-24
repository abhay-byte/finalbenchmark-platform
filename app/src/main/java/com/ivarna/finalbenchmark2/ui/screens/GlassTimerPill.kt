package com.ivarna.finalbenchmark2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassTimerPill(timeText: String, elapsedTime: String) {
    Box(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Layer 1: Blurred Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .blur(30.dp)
        )
        
        // Layer 2: Border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
        )

        // Layer 3: Content
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ELAPSED SECTION
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Elapsed: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = elapsedTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // DIVIDER
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier
                .width(1.dp)
                .height(12.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(12.dp))

            // REMAINING SECTION
            Text(
                text = "Rem: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Animated Number
            AnimatedContent(
                targetState = timeText,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                },
                label = "TimerAnimation"
            ) { targetTime ->
                Text(
                    text = targetTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
