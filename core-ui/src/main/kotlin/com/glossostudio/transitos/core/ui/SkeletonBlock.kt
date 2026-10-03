package com.glossostudio.transitos.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.Alpha

/**
 * Content-shaped shimmer placeholder. A soft highlight sweeps across the block
 * instead of the whole thing blinking, which reads as "loading" rather than
 * "broken" and keeps the screen stable when real content arrives.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.SKELETON)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.SKELETON_HIGH)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-sweep",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .drawBehind {
                val band = size.width * 0.5f
                val travel = size.width + band
                val startX = progress * travel - band
                drawRect(color = base)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(startX, 0f),
                        end = Offset(startX + band, 0f),
                    ),
                )
            },
    )
}
