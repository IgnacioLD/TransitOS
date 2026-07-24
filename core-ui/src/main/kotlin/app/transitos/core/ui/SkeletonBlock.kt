package com.glossostudio.transitos.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.glossostudio.transitos.core.design.theme.Alpha

/**
 * Content-shaped shimmer placeholder. Used by the skeleton loading state to
 * preview upcoming cards without the cognitive cost of a spinner — calmer,
 * and the screen doesn't jump when data arrives.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = Alpha.SKELETON,
        targetValue = Alpha.SKELETON * 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
    )
}
