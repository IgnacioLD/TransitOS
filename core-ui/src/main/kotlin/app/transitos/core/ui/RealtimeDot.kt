package app.transitos.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.transitos.core.design.theme.Alpha

/**
 * Small pulsing dot signalling "this datum is live". Calm cadence (1.6 s
 * breathing cycle) so it reads as a heartbeat rather than a notification.
 *
 * Reserved for rows whose source is genuinely real-time. Scheduled-only rows
 * in the future will omit it.
 */
@Composable
fun RealtimeDot(
    modifier: Modifier = Modifier,
    diameter: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "realtime-pulse")
    val alpha by transition.animateFloat(
        initialValue = Alpha.MEDIUM,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "realtime-alpha",
    )
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "realtime-scale",
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(diameter)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}
