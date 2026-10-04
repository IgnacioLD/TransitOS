package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.model.Alert

/**
 * A single service-alert banner. Severity drives the colour language:
 *
 * - [Alert.Severity.SEVERE]  → error container (disruption)
 * - [Alert.Severity.WARNING] → amber tertiary container (delays)
 * - [Alert.Severity.INFO]    → calm secondary container (notice)
 *
 * When the alert is tied to one line, its badge takes the leading slot;
 * otherwise a severity icon does.
 */
@Composable
fun AlertRow(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    lineShortName: String? = null,
    lineColor: Long? = null,
    severity: Alert.Severity = Alert.Severity.WARNING,
) {
    val spacing = LocalSpacing.current
    val (container, onContainer) = severity.colors()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = onContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (lineShortName != null) {
                LineBadge(
                    label = lineShortName,
                    colorArgb = lineColor,
                    size = 34.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(onContainer.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (severity == Alert.Severity.INFO) {
                            Icons.Outlined.Info
                        } else {
                            Icons.Outlined.Warning
                        },
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                )
                if (!body.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.82f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Alert.Severity.colors(): Pair<Color, Color> = when (this) {
    Alert.Severity.SEVERE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    Alert.Severity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    Alert.Severity.INFO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}
