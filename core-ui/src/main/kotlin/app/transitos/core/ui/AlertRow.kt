package app.transitos.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.transitos.core.design.theme.LocalSpacing

/**
 * One row in an alerts list: a coloured line badge (when the alert affects a
 * single known line) and the alert title on a soft error-container surface.
 *
 * Kept stateless and reusable so any screen — Home, Stop detail, Search — can
 * show an alert consistently.
 */
@Composable
fun AlertRow(
    title: String,
    modifier: Modifier = Modifier,
    lineShortName: String? = null,
    lineColor: Long? = null,
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (lineShortName != null) {
                LineBadge(
                    label = lineShortName,
                    colorArgb = lineColor,
                    size = 32.dp,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
