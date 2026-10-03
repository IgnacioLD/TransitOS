package com.glossostudio.transitos.core.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.Motion
import com.glossostudio.transitos.core.design.theme.TabularFigures

/**
 * The live minutes countdown. The number springs between values instead of
 * jumping, so a refresh reads as "ticking" rather than a relayout, and uses
 * tabular figures so the width never shifts.
 *
 * The size and colours are injected so the same piece can sit huge on the
 * departure board or compact inside a row.
 */
@Composable
fun AnimatedMinutes(
    minutes: Int,
    modifier: Modifier = Modifier,
    numberStyle: TextStyle = MaterialTheme.typography.displayLarge,
    unitStyle: TextStyle = MaterialTheme.typography.titleMedium,
    numberColor: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = numberColor.copy(alpha = 0.7f),
) {
    val animated: Int by animateIntAsState(
        targetValue = minutes,
        animationSpec = Motion.springy(),
        label = "minutes",
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = animated.toString(),
            style = numberStyle.copy(fontFeatureSettings = TabularFigures),
            color = numberColor,
        )
        Text(
            text = stringResource(R.string.unit_minutes),
            style = unitStyle,
            color = unitColor,
            modifier = Modifier.padding(bottom = 5.dp),
        )
    }
}
