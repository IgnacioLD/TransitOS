package app.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.model.Arrival
import app.transitos.core.model.Stop

/**
 * Card grouping a favorited [Stop] with its upcoming [arrivals]. The stop name
 * is the header; each arrival is a row with line badge, destination, realtime
 * indicator and a large minutes figure as the visual anchor.
 *
 * The first arrival row gets a subtle surface tint to mark "this is the next
 * one" — the most-glanced piece of information on the screen. The footer shows
 * a relative "actualizado hace Xs" so the user can judge freshness at a glance.
 *
 * @param lastUpdatedMs epoch milliseconds of the last arrivals fetch. Pass 0
 *   to suppress the footer (e.g. when the list is empty).
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            FavoriteStopHeader(stop = stop)

            if (arrivals.isEmpty()) {
                NoArrivalsHint()
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(spacing.xs))
                arrivals
                    .take(MAX_ARRIVALS_PER_CARD)
                    .forEachIndexed { index, arrival ->
                        ArrivalRow(
                            arrival = arrival,
                            isNext = index == 0,
                        )
                    }
            }

            if (lastUpdatedMs > 0L) {
                Text(
                    text = "Actualizado ${relativeTime(lastUpdatedMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun FavoriteStopHeader(stop: Stop) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stop.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NoArrivalsHint() {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.shapes.small,
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
                text = "Próximo tren sin confirmar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Usa el Planificador para ver horarios programados",
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
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }
    val a11y = buildA11yLabel(arrival, isNext)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.small)
            .padding(horizontal = spacing.sm, vertical = spacing.sm)
            .semantics { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        LineBadge(
            label = arrival.lineShortName ?: "—",
            colorArgb = arrival.lineColor,
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
                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (arrival.isRealTime) RealtimeDot()
                Text(
                    text = if (arrival.isRealTime) "En vivo" else "Horario",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ArrivalMinutes(arrival = arrival, emphasized = isNext)
    }
}

@Composable
private fun ArrivalMinutes(arrival: Arrival, emphasized: Boolean) {
    val minutes = arrival.minutesAway
    val value = if (minutes != null && minutes <= 0) "Próximo" else minutes?.toString() ?: "—"
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (value != "Próximo") {
            Text(
                text = "min",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

private fun buildA11yLabel(arrival: Arrival, isNext: Boolean): String {
    val head = if (isNext) "Próximo " else ""
    val line = arrival.lineShortName?.let { "línea $it" } ?: ""
    val eta = arrival.minutesAway?.let { "${it} minutos" } ?: "hora no disponible"
    return "$head${line} hacia ${arrival.destination}, $eta".trim().replace("  ", " ")
}

private const val MAX_ARRIVALS_PER_CARD = 5
