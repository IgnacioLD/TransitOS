package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glossostudio.transitos.core.design.theme.LocalSpacing

/**
 * Skeleton loading for the Home screen: a column of card-shaped shimmer
 * placeholders. Reads as "something is coming", not "the app is broken".
 *
 * Use during the initial state (before the first emission of [com.glossostudio.transitos.feature.home.HomeUiState.Ready]).
 */
@Composable
fun FavoritesSkeleton(
    modifier: Modifier = Modifier,
    cardCount: Int = 2,
) {
    val spacing = LocalSpacing.current
    val placeholders = IntRange(0, cardCount - 1).toList()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenGutter,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.cardGap),
    ) {
        items(placeholders, key = { it }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 22.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 28.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 28.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 28.dp)
                }
            }
        }
    }
}
