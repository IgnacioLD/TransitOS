package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glossostudio.transitos.core.design.theme.BrandColors
import com.glossostudio.transitos.core.design.theme.HeroShape
import com.glossostudio.transitos.core.model.Arrival

/**
 * The home departure board: the single most important piece of information in
 * the app, blown up to hero size.
 *
 * A deep brand gradient surface with a faint network watermark, the line badge
 * and the direction it is heading, and the next departure's minutes as the
 * typographic anchor. The following departures appear as compact rows with
 * their own line and direction, so the cadence is readable at a glance.
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
    val noDestination = stringResource(R.string.arrival_no_destination)
    val towardsLabel = stringResource(R.string.board_towards)
    val hasDestination = next.destination.isNotBlank()
    val destination = if (hasDestination) next.destination else noDestination

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(gradient)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = a11y },
    ) {
        HeroWatermark(modifier = Modifier.matchParentSize())
        // Re-paints the brand gradient over the watermark, so the art can only
        // contribute a fraction of its colour. The card keeps its exact brand
        // surface while the plan stays legible as a texture, never as a
        // competitor to the departure text.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(HeroArtScrim),
        )
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
                    if (hasDestination) {
                        Text(
                            text = towardsLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandColors.OnHeroMuted,
                            letterSpacing = 1.sp,
                        )
                    }
                    Text(
                        text = destination,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandColors.OnHero,
                        maxLines = 2,
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
                upcoming.take(MAX_UPCOMING).forEachIndexed { index, arrival ->
                    if (index > 0) {
                        HorizontalDivider(color = BrandColors.OnHero.copy(alpha = 0.10f))
                    }
                    UpcomingRow(arrival = arrival)
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

/**
 * One following departure: line badge, the direction it is heading and the wait.
 *
 * A full-width row rather than a narrow chip, so the headsign always stays
 * visible instead of being clipped by a horizontal scroll, and its two-line
 * allowance keeps long names such as "Alboraya Peris Arago" intact.
 */
@Composable
private fun UpcomingRow(arrival: Arrival) {
    val noLine = stringResource(R.string.arrival_no_line)
    val minutes = arrival.minutesAway
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineBadge(
            label = arrival.lineShortName ?: noLine,
            colorArgb = arrival.lineColor ?: HERO_BADGE_FALLBACK,
            size = 30.dp,
        )
        Text(
            text = arrivalDirection(arrival.destination),
            style = MaterialTheme.typography.bodyMedium,
            color = BrandColors.OnHero,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (minutes != null && minutes > 0) {
                stringResource(R.string.board_minutes_short, minutes)
            } else {
                stringResource(R.string.arrival_boarding)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BrandColors.OnHero,
            maxLines = 1,
        )
    }
}

/**
 * Faint schematic metro plan behind the board, so the hero reads as part of the
 * network language. The official line colours are blended onto the brand
 * gradient at low opacity: present and recognisable, but never competing with
 * the departure text. A single dot travels the yellow line for a touch of life.
 */
@Composable
private fun HeroWatermark(modifier: Modifier = Modifier) {
    MetroNetworkArtwork(
        network = MetrovalenciaNetworks.hero,
        style = HeroMetroStyle,
        modifier = modifier,
        animate = true,
    )
}

/**
 * Scrim painted between the watermark and the content. It is the brand gradient
 * again, so the base surface colour is unchanged, but it caps how much of the
 * artwork's brightness reaches the text. At these alphas the art contributes
 * roughly a third of its colour.
 */
private val HeroArtScrim = Brush.linearGradient(
    colors = listOf(
        BrandColors.HeroGradientTop.copy(alpha = 0.62f),
        BrandColors.HeroGradientBottom.copy(alpha = 0.70f),
    ),
)

/** Cool, low-luminance marks for the hero; pure white would glare. */
private val HeroMarkTint = Color(0xFF7FD6D4)

private val HeroMetroStyle = MetroArtStyle(
    frame = MetroFrame.COVER,
    strokeScale = 0.024f,
    lineAlpha = 0.22f,
    // No casing: at watermark strength the dark under-stroke only muddied the
    // gradient, and the scrim already separates the lines from the text.
    stationColor = HeroMarkTint.copy(alpha = 0.16f),
    ringColor = HeroMarkTint.copy(alpha = 0.22f),
    terminalColor = HeroMarkTint.copy(alpha = 0.26f),
    blendMode = BlendMode.Screen,
    trainLineId = "L1",
    trainColor = Color(0xFFBFF0EE).copy(alpha = 0.50f),
)

@Composable
private fun relativeTimeLabel(epochMs: Long): String = when (val relative = relativeTime(epochMs)) {
    is RelativeTime.JustNow -> stringResource(R.string.time_just_now)
    is RelativeTime.Minutes -> stringResource(R.string.time_minutes_ago, relative.value)
    is RelativeTime.Hours -> stringResource(R.string.time_hours_ago, relative.value)
}

@Composable
private fun boardA11yLabel(stopName: String, next: Arrival): String {
    val linePrefix = stringResource(R.string.a11y_line_prefix, next.lineShortName ?: "")
    val destination = next.destination.ifBlank { stringResource(R.string.arrival_no_destination) }
    val towards = stringResource(R.string.a11y_towards, destination)
    val eta = next.minutesAway?.let { stringResource(R.string.a11y_eta_minutes, it) }
        ?: stringResource(R.string.a11y_eta_unavailable)
    return "$stopName. $linePrefix $towards, $eta".replace("  ", " ").trim()
}

private const val MAX_UPCOMING = 3
private const val HERO_BADGE_FALLBACK = 0xFFF5C55AL
