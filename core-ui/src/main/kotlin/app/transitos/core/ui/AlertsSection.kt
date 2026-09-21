package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.model.Alert

/**
 * Collapsible service-alerts block.
 *
 * When there are more than [previewCount] alerts, only the first [previewCount]
 * are rendered plus a "Show all N lines" toggle. This avoids the wall-of-rows
 * problem when an operator-wide incident flags every line at once.
 *
 * The header carries the live count so the user always knows the total even
 * when collapsed. Expansion state survives recomposition and config changes
 * via [rememberSaveable].
 */
@Composable
fun AlertsSection(
    alerts: List<Alert>,
    modifier: Modifier = Modifier,
    previewCount: Int = 3,
) {
    if (alerts.isEmpty()) return
    val spacing = LocalSpacing.current
    var expanded by rememberSaveable(alerts.size) { mutableStateOf(false) }
    val showToggle = alerts.size > previewCount
    val visibleCount = if (showToggle && !expanded) previewCount else alerts.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionHeader(
            text = stringResource(R.string.alerts_section_title),
            trailing = { CountBadge(count = alerts.size) },
        )

        alerts.take(visibleCount).forEach { alert ->
            AlertRow(
                title = alert.title,
                body = alert.body,
                lineShortName = alert.lineShortName,
                lineColor = alert.lineColor,
                severity = alert.severity,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenGutter),
            )
        }

        if (showToggle) {
            ExpandToggle(
                expanded = expanded,
                totalCount = alerts.size,
                onClick = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun ExpandToggle(
    expanded: Boolean,
    totalCount: Int,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenGutter),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (expanded) stringResource(R.string.alerts_show_less)
                else stringResource(R.string.alerts_show_all, totalCount),
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
