package app.transitos.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.model.Alert

/**
 * Collapsible alerts block.
 *
 * When there are more than [previewCount] alerts (default 3), only the first
 * [previewCount] are rendered plus a "Ver las N líneas" toggle. This avoids
 * the wall-of-rows problem when an operator-wide incident flags every line
 * at once (e.g. FGV currently returns one alert per affected line, so a
 * network-wide event produces 10+ rows).
 *
 * The header carries the live count so the user always knows the total even
 * when collapsed. Expansion state survives recomposition and config changes
 * via [rememberSaveable]; it resets if the alert set changes identity.
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
            .padding(bottom = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionHeader(text = stringResource(R.string.alerts_section_title, alerts.size))

        // Stable keys so animated visibility doesn't fight the LazyColumn.
        alerts.take(visibleCount).forEach { alert ->
            AlertRow(
                title = alert.title,
                body = alert.body,
                lineShortName = alert.lineShortName,
                lineColor = alert.lineColor,
                modifier = Modifier.fillMaxWidth(),
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
    val label = if (expanded) {
        stringResource(R.string.alerts_show_less)
    } else {
        stringResource(R.string.alerts_show_all, totalCount)
    }
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
