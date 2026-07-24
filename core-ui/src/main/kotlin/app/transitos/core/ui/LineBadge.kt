package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.onColor

/**
 * The coloured line identifier chip — `L3` on the line's brand colour. This is
 * the single most recognizable transit-app element; every provider that knows
 * the line colour should populate [Arrival.lineColor] so this renders correctly.
 *
 * Falls back to the theme primary when no colour is supplied (so the badge is
 * never an empty coloured square).
 *
 * @param label       short line name, e.g. "L3" or "C5"
 * @param colorArgb   optional ARGB colour from the operator; null uses primary
 * @param contentDescription spoken label for TalkBack — pass through the
 *   semantic meaning ("Línea L3"), not just the visible text
 */
@Composable
fun LineBadge(
    label: String,
    modifier: Modifier = Modifier,
    colorArgb: Long? = null,
    size: Dp = 36.dp,
    contentDescription: String? = null,
) {
    val badgeColor = colorArgb?.let(::Color) ?: MaterialTheme.colorScheme.primary
    val foreground = onColor(badgeColor)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(badgeColor)
            .let { if (contentDescription != null) it.semantics { this.contentDescription = contentDescription } else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
