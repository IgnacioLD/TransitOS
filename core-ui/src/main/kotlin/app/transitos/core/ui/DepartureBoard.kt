package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glossostudio.transitos.core.design.theme.BrandColors
import com.glossostudio.transitos.core.design.theme.HeroShape
import com.glossostudio.transitos.core.design.theme.PillShape
import com.glossostudio.transitos.core.model.Arrival

/**
 * The home departure board: the single most important piece of information in
 * the app, blown up to hero size.
 *
 * A deep brand gradient surface with a faint network watermark, the line badge
 * and destination, and the next departure's minutes as the typographic anchor.
 * The following departures appear as compact chips so the user can already
 * sense the cadence without leaving the card.
 *
 * Everything sits on the fixed brand gradient, so the same hero reads
 * correctly in light, dark and AMOLED.
 *
 * @param next the soonest arrival; drives the big counter
 * @param upcoming the following arrivals, shown as chips (the caller should
 *   exclude [next])
 */
@Composable
fun DepartureBoard(
    stopName: String,
    next: Arrival,
    modifier: Modifier = Modifier,
    upcoming: List<Arrival> = emptyList(),
    lastUpdatedMs: Long = 0L,
    onClick: (() -> Unit)? = null,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(BrandColors.HeroGradientTop, BrandColors.HeroGradientBottom),
    )
    val a11y = boardA11yLabel(stopName, next)
    val noLine = stringResource(R.string.arrival_no_line)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(gradient)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = a11y },
    ) {
        HeroWatermark(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.board_next_departure).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.OnHeroMuted,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.weight(1f),
                )
                LiveChip(isRealTime = next.isRealTime)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = stopName,
                style = MaterialTheme.typography.headlineSmall,
                color = BrandColors.OnHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineBadge(
                    label = next.lineShortName ?: noLine,
                    colorArgb = next.lineColor ?: HERO_BADGE_FALLBACK,
                    size = 46.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = next.destination,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandColors.OnHero,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val minutes = next.minutesAway
                if (minutes != null && minutes > 0) {
                    AnimatedMinutes(
                        minutes = minutes,
                        numberStyle = MaterialTheme.typography.displayMedium,
                        unitStyle = MaterialTheme.typography.titleSmall,
                        numberColor = BrandColors.OnHero,
                        unitColor = BrandColors.OnHeroMuted,
                    )
                } else {
                    StatusPill(
                        text = stringResource(R.string.arrival_boarding),
                        containerColor = BrandColors.HeroAccent,
                        contentColor = Color(0xFF3F2D00),
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = BrandColors.OnHero.copy(alpha = 0.18f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    upcoming.take(MAX_UPCOMING).forEach { arrival ->
                        UpcomingChip(arrival = arrival)
                    }
                }
            }

            if (lastUpdatedMs > 0L) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.card_updated_prefix, relativeTimeLabel(lastUpdatedMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandColors.OnHeroMuted,
                )
            }
        }
    }
}

@Composable
private fun LiveChip(isRealTime: Boolean) {
    StatusPill(
        text = if (isRealTime) stringResource(R.string.arrival_live) else stringResource(R.string.arrival_scheduled),
        containerColor = BrandColors.OnHero.copy(alpha = 0.16f),
        contentColor = BrandColors.OnHero,
        leading = {
            if (isRealTime) {
                RealtimeDot(diameter = 7.dp, color = BrandColors.HeroAccent)
            }
        },
    )
}

@Composable
private fun UpcomingChip(arrival: Arrival) {
    val noLine = stringResource(R.string.arrival_no_line)
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(BrandColors.OnHero.copy(alpha = 0.14f))
            .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LineBadge(
            label = arrival.lineShortName ?: noLine,
            colorArgb = arrival.lineColor ?: HERO_BADGE_FALLBACK,
            size = 24.dp,
        )
        val minutes = arrival.minutesAway
        Text(
            text = if (minutes != null && minutes > 0) {
                stringResource(R.string.board_minutes_short, minutes)
            } else {
                stringResource(R.string.arrival_boarding)
            },
            style = MaterialTheme.typography.labelLarge,
            color = BrandColors.OnHero,
            maxLines = 1,
        )
    }
}

/** Faint rail watermark so the hero reads as part of the network language. */
@Composable
private fun HeroWatermark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = h * 0.075f
        val paint = Color.White.copy(alpha = 0.07f)

        val first = Path().apply {
            moveTo(-w * 0.05f, h * 0.86f)
            quadraticTo(w * 0.32f, h * 0.56f, w * 0.60f, h * 0.66f)
            quadraticTo(w * 0.84f, h * 0.74f, w * 1.06f, h * 0.30f)
        }
        val second = Path().apply {
            moveTo(-w * 0.05f, h * 1.05f)
            quadraticTo(w * 0.40f, h * 0.82f, w * 0.72f, h * 0.92f)
            quadraticTo(w * 0.94f, h * 0.99f, w * 1.06f, h * 0.70f)
        }
        drawPath(first, color = paint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawPath(second, color = paint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        drawCircle(color = Color.White.copy(alpha = 0.10f), radius = stroke * 0.6f, center = Offset(w * 0.60f, h * 0.66f))
        drawCircle(color = Color.White.copy(alpha = 0.10f), radius = stroke * 0.6f, center = Offset(w * 0.32f, h * 0.78f))
    }
}

@Composable
private fun relativeTimeLabel(epochMs: Long): String = when (val relative = relativeTime(epochMs)) {
    is RelativeTime.JustNow -> stringResource(R.string.time_just_now)
    is RelativeTime.Minutes -> stringResource(R.string.time_minutes_ago, relative.value)
    is RelativeTime.Hours -> stringResource(R.string.time_hours_ago, relative.value)
}

@Composable
private fun boardA11yLabel(stopName: String, next: Arrival): String {
    val linePrefix = stringResource(R.string.a11y_line_prefix, next.lineShortName ?: "")
    val towards = stringResource(R.string.a11y_towards, next.destination)
    val eta = next.minutesAway?.let { stringResource(R.string.a11y_eta_minutes, it) }
        ?: stringResource(R.string.a11y_eta_unavailable)
    return "$stopName. $linePrefix $towards, $eta".replace("  ", " ").trim()
}

private const val MAX_UPCOMING = 3
private const val HERO_BADGE_FALLBACK = 0xFFF5C55AL
