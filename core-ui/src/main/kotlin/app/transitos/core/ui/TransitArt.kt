package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TransitOS's own illustration: a compact schematic metro plan on a soft
 * primary-container disc. It fills the empty state so a screen with no data yet
 * still feels like part of the product.
 *
 * The fragment is authored in [MetrovalenciaNetworks.badge] and drawn by the
 * shared [MetroNetworkArtwork] renderer, so it is the same visual language as
 * the home hero: 45/90 lines, tick stations, double-circle interchanges and a
 * terminal bar. One interchange pulses gently so the plan is alive without
 * being distracting.
 */
@Composable
fun TransitNetworkArt(
    modifier: Modifier = Modifier,
    size: Dp = 156.dp,
) {
    val backdrop = MaterialTheme.colorScheme.primaryContainer
    val station = MaterialTheme.colorScheme.surface
    val ring = MaterialTheme.colorScheme.onPrimaryContainer
    val style = MetroArtStyle(
        frame = MetroFrame.CONTAIN,
        contentScale = 0.70f,
        strokeScale = 0.031f,
        lineAlpha = 1f,
        casingColor = backdrop,
        casingScale = 0.7f,
        stationColor = station,
        ringColor = ring,
        terminalColor = ring,
        blendMode = BlendMode.SrcOver,
        trainLineId = null,
        trainColor = Color.Transparent,
        pulseInterchanges = true,
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backdrop),
    ) {
        MetroNetworkArtwork(
            network = MetrovalenciaNetworks.badge,
            style = style,
            modifier = Modifier.matchParentSize(),
            animate = true,
        )
    }
}
