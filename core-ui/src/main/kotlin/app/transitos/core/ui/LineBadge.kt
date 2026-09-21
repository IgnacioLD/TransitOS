package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glossostudio.transitos.core.design.theme.onColor

/**
 * The coloured line identifier chip, `L3` on the line's brand colour.
 *
 * This is the single most recognisable transit-app element, so it gets a little
 * care: a soft top-light gradient for depth and a hairline border so pale
 * operator colours (yellow, light grey) still read against a pale surface.
 *
 * Falls back to the theme primary when no colour is supplied, so the badge is
 * never an empty coloured square.
 *
 * @param label     short line name, e.g. "L3" or "C5"
 * @param colorArgb optional ARGB colour from the operator; null uses primary
 * @param size      square edge; the badge is a rounded square, not a circle
 * @param contentDescription spoken label for TalkBack, pass the semantic
 *   meaning ("Line L3"), not just the visible text
 */
@Composable
fun LineBadge(
    label: String,
    modifier: Modifier = Modifier,
    colorArgb: Long? = null,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    val badgeColor = colorArgb?.let(::Color) ?: MaterialTheme.colorScheme.primary
    val foreground = onColor(badgeColor)
    val shape = RoundedCornerShape(size * 0.30f)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                ),
            )
            .background(badgeColor)
            .padding(horizontal = size * 0.12f)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            fontSize = if (label.length > 2) (size.value * 0.34f).sp else (size.value * 0.42f).sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A quiet outlined badge used when a line has no known brand colour. Keeps the
 * grid honest instead of painting the fallback primary on every unknown line.
 */
@Composable
fun LineBadgeOutline(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = size * 0.12f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontSize = if (label.length > 2) (size.value * 0.34f).sp else (size.value * 0.42f).sp,
            maxLines = 1,
        )
    }
}
