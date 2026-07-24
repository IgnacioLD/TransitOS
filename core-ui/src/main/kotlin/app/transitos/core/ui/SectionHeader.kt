package app.transitos.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import app.transitos.core.design.theme.LocalSpacing

/**
 * Small uppercase label that opens a section ("Favoritos", "Avisos"). Reserved
 * for grouping — never use this for content text.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.4.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = spacing.lg,
                end = spacing.lg,
                top = spacing.lg,
                bottom = spacing.xs,
            ),
    )
}
