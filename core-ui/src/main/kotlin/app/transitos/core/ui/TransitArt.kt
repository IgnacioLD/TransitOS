package com.glossostudio.transitos.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TransitOS's own illustration: a fragment of a transit network, three line
 * ribbons crossing and a highlighted interchange. It replaces the generic
 * icon medallion in empty and error states so those moments still feel like
 * part of the product.
 *
 * The line colours are the brand's own metro palette, on a soft primary
 * container disc. One node pulses gently so the art is alive without being
 * distracting.
 */
@Composable
fun TransitNetworkArt(
    modifier: Modifier = Modifier,
    size: Dp = 156.dp,
) {
    val backdrop = MaterialTheme.colorScheme.primaryContainer
    val node = MaterialTheme.colorScheme.surface
    val lineA = Color(0xFF1B4DA0)
    val lineB = Color(0xFFD0103A)
    val lineC = Color(0xFFF5C518)
    val interchange = Color(0xFF0C6E6D)

    val transition = rememberInfiniteTransition(label = "network-art")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "node-pulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val radius = s / 2f
        drawCircle(color = backdrop, radius = radius, center = Offset(radius, radius))

        val stroke = s * 0.05f
        val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val pathA = Path().apply {
            moveTo(s * 0.12f, s * 0.70f)
            quadraticTo(s * 0.42f, s * 0.64f, s * 0.58f, s * 0.40f)
            quadraticTo(s * 0.70f, s * 0.22f, s * 0.88f, s * 0.24f)
        }
        val pathB = Path().apply {
            moveTo(s * 0.18f, s * 0.30f)
            quadraticTo(s * 0.40f, s * 0.44f, s * 0.56f, s * 0.52f)
            quadraticTo(s * 0.74f, s * 0.62f, s * 0.86f, s * 0.82f)
        }
        val pathC = Path().apply {
            moveTo(s * 0.16f, s * 0.84f)
            quadraticTo(s * 0.36f, s * 0.74f, s * 0.52f, s * 0.56f)
            quadraticTo(s * 0.66f, s * 0.42f, s * 0.84f, s * 0.36f)
        }

        drawPath(pathA, color = lineA, style = style)
        drawPath(pathB, color = lineB, style = style)
        drawPath(pathC, color = lineC, style = style)

        val nodes = listOf(
            Offset(s * 0.58f, s * 0.40f) to lineA,
            Offset(s * 0.56f, s * 0.52f) to lineB,
            Offset(s * 0.52f, s * 0.56f) to lineC,
        )
        nodes.forEach { (center, color) ->
            drawCircle(color = node, radius = stroke * 1.15f, center = center)
            drawCircle(color = color, radius = stroke * 0.66f, center = center)
        }

        // Highlighted interchange with a soft animated halo.
        val highlight = Offset(s * 0.36f, s * 0.74f)
        drawCircle(color = interchange.copy(alpha = pulse * 0.28f), radius = stroke * 2.1f, center = highlight)
        drawCircle(color = node, radius = stroke * 1.2f, center = highlight)
        drawCircle(color = interchange, radius = stroke * 0.72f, center = highlight)
    }
}
