package com.glossostudio.transitos.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small pulsing "live" indicator: a solid core with an expanding, fading halo,
 * the universal shorthand for "this datum is real-time". The cadence is calm
 * (1.6 s) so it reads as a heartbeat, not a notification.
 *
 * Colours default to the brand amber, which pairs with the tram-light motif,
 * but callers can override for other semantic uses.
 */
@Composable
fun RealtimeDot(
    modifier: Modifier = Modifier,
    diameter: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,
) {
    val transition = rememberInfiniteTransition(label = "realtime-pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "realtime-progress",
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(diameter * (0.6f + progress * 1.4f))
                .clip(CircleShape)
                .background(color.copy(alpha = (1f - progress) * 0.45f)),
        )
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(color),
        )
    }
}
