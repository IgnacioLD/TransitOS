package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.design.theme.TabularFigures
import com.glossostudio.transitos.core.design.theme.onColor
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Stop

/**
 * Card grouping a favourite [Stop] with its upcoming [arrivals].
 *
 * Layout, top to bottom: an identity header (name + freshness), then one row
 * per arrival with the line badge, destination and a large minutes figure as
 * the visual anchor. The first arrival is the hero: it gets a soft tinted
 * surface and a line-coloured accent bar so the eye lands on it instantly.
 *
 * @param lastUpdatedMs epoch milliseconds of the last arrivals fetch. Pass 0
 *   to suppress the freshness line (e.g. when the list is empty).
 */
@Composable
fun FavoriteStopCard(
    stop: Stop,
    arrivals: List<Arrival>,
    modifier: Modifier = Modifier,
    lastUpdatedMs: Long = 0L,
    onClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        ) {
            FavoriteStopHeader(stop = stop, lastUpdatedMs = lastUpdatedMs)

            if (arrivals.isEmpty()) {
                Spacer(Modifier.height(spacing.md))
                NoArrivalsHint()
            } else {
                Spacer(Modifier.height(spacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                arrivals
                    .take(MAX_ARRIVALS_PER_CARD)
                    .forEachIndexed { index, arrival ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 52.dp),
                            )
                        }
                        ArrivalRow(arrival = arrival, isNext = index == 0)
                    }
            }
        }
    }
}

@Composable
private fun FavoriteStopHeader(stop: Stop, lastUpdatedMs: Long) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stop.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (lastUpdatedMs > 0L) {
            Text(
                text = stringResource(R.string.card_updated_prefix, relativeTimeLabel(lastUpdatedMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun relativeTimeLabel(epochMs: Long): String = when (val relative = relativeTime(epochMs)) {
    is RelativeTime.JustNow -> stringResource(R.string.time_just_now)
    is RelativeTime.Minutes -> stringResource(R.string.time_minutes_ago, relative.value)
    is RelativeTime.Hours -> stringResource(R.string.time_hours_ago, relative.value)
}

@Composable
private fun NoArrivalsHint() {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.shapes.medium,
            )
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            Text(
                text = stringResource(R.string.arrival_unconfirmed_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.arrival_unconfirmed_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArrivalRow(
    arrival: Arrival,
    isNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val containerColor = if (isNext) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
    } else {
        Color.Transparent
    }
    val a11y = arrivalA11yLabel(arrival, isNext)
    val noLine = stringResource(R.string.arrival_no_line)
    val accent = arrival.lineColor?.let(::Color) ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isNext) spacing.sm else 0.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .padding(horizontal = spacing.sm, vertical = spacing.sm)
            .semantics { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        LineBadge(
            label = arrival.lineShortName ?: noLine,
            colorArgb = arrival.lineColor,
            size = if (isNext) 42.dp else 38.dp,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = arrival.destination,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (arrival.isRealTime) RealtimeDot(diameter = 7.dp)
                Text(
                    text = if (arrival.isRealTime) {
                        stringResource(R.string.arrival_live)
                    } else {
                        stringResource(R.string.arrival_scheduled)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ArrivalMinutes(arrival = arrival, emphasized = isNext, accent = accent)
    }
}

@Composable
private fun ArrivalMinutes(arrival: Arrival, emphasized: Boolean, accent: Color) {
    val minutes = arrival.minutesAway
    val isBoarding = minutes != null && minutes <= 0
    val boarding = stringResource(R.string.arrival_boarding)
    val noLine = stringResource(R.string.arrival_no_line)
    val unitMin = stringResource(R.string.unit_minutes)

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.width(76.dp),
    ) {
        when {
            isBoarding -> StatusPill(
                text = boarding,
                containerColor = accent,
                contentColor = onColor(accent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
            )

            minutes != null -> Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = minutes.toString(),
                    style = (if (emphasized) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.titleLarge).copy(fontFeatureSettings = TabularFigures),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unitMin,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                )
            }

            else -> Text(
                text = noLine,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun arrivalA11yLabel(arrival: Arrival, isNext: Boolean): String {
    val boarding = stringResource(R.string.arrival_boarding)
    val linePrefix = stringResource(R.string.a11y_line_prefix, arrival.lineShortName ?: "")
    val etaMinutes = stringResource(R.string.a11y_eta_minutes, arrival.minutesAway ?: 0)
    val etaUnavailable = stringResource(R.string.a11y_eta_unavailable)
    val towards = stringResource(R.string.a11y_towards, arrival.destination)

    val head = if (isNext) "$boarding " else ""
    val line = if (arrival.lineShortName != null) linePrefix else ""
    val eta = if (arrival.minutesAway != null) etaMinutes else etaUnavailable
    return "$head$line $towards, $eta".trim().replace("  ", " ")
}

private const val MAX_ARRIVALS_PER_CARD = 5

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun FavoriteStopCardPreview() {
    com.glossostudio.transitos.core.design.theme.TransitOSTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FavoriteStopCard(
                stop = Stop(
                    id = "benimaclet",
                    name = "Benimaclet",
                    operatorId = "metrovalencia",
                    mode = com.glossostudio.transitos.core.model.TransportMode.METRO,
                    lat = 39.48,
                    lon = -0.35,
                ),
                arrivals = listOf(
                    Arrival(
                        stopId = "benimaclet",
                        lineId = "l3",
                        destination = "Rafelbunyol",
                        minutesAway = 4,
                        isRealTime = true,
                        lineShortName = "L3",
                        lineColor = 0xFFD0103A,
                    ),
                    Arrival(
                        stopId = "benimaclet",
                        lineId = "l9",
                        destination = "Av. del Cid",
                        minutesAway = 11,
                        isRealTime = true,
                        lineShortName = "L9",
                        lineColor = 0xFFC08A3E,
                    ),
                ),
                lastUpdatedMs = 0L,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

