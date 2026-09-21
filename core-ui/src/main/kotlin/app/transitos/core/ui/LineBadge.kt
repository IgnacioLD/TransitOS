package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** Rounded-square corner radius as a fraction of the badge edge. */
private const val CORNER_RADIUS_FRACTION = 0.30f

/** Side padding as a fraction of the badge edge, keeps labels off the border. */
private const val HORIZONTAL_PADDING_FRACTION = 0.12f

/** Alpha of the white top-light gradient that gives the badge depth. */
private const val GRADIENT_HIGHLIGHT_ALPHA = 0.14f

/** Alpha of the hairline border drawn in the foreground colour. */
private const val BORDER_ALPHA = 0.15f

/** Labels longer than this are shrunk so they fit inside the badge. */
private const val COMPACT_LABEL_LENGTH = 2

/** Font size as a fraction of the badge edge, for short and long labels. */
private const val COMPACT_LABEL_FONT_FRACTION = 0.34f
private const val DEFAULT_LABEL_FONT_FRACTION = 0.42f

private val DefaultBadgeSize = 40.dp
private val BorderWidth = 1.dp

/**
 * The coloured line identifier chip, `L3` on the line's brand colour.
 *
 * This is the single most recognisable transit-app element, so it gets a little
 * care: the brand colour sits on the bottom, a soft top-light gradient is
 * layered over it for depth, and a hairline border in the foreground colour
 * keeps pale operator colours (yellow, light grey) legible against a pale
 * surface.
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
    size: Dp = DefaultBadgeSize,
    contentDescription: String? = null,
) {
    val badgeColor = colorArgb?.let(::Color) ?: MaterialTheme.colorScheme.primary
    val foreground = onColor(badgeColor)
    val shape = RoundedCornerShape(size * CORNER_RADIUS_FRACTION)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .clip(shape)
            .background(badgeColor)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = GRADIENT_HIGHLIGHT_ALPHA),
                        Color.Transparent,
                    ),
                ),
            )
            .border(width = BorderWidth, color = foreground.copy(alpha = BORDER_ALPHA), shape = shape)
            .padding(horizontal = size * HORIZONTAL_PADDING_FRACTION)
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
            fontSize = if (label.length > COMPACT_LABEL_LENGTH) {
                (size.value * COMPACT_LABEL_FONT_FRACTION).sp
            } else {
                (size.value * DEFAULT_LABEL_FONT_FRACTION).sp
            },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
