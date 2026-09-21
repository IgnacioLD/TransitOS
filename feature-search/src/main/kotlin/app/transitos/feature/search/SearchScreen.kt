package com.glossostudio.transitos.feature.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.design.theme.PillShape
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.ui.EmptyState
import com.glossostudio.transitos.core.ui.R as coreUiR
import com.glossostudio.transitos.core.ui.SkeletonBlock
import com.glossostudio.transitos.core.util.stripDiacritics
import com.glossostudio.transitos.feature.search.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filteredStops by viewModel.filteredStops.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val allStops by viewModel.allStops.collectAsStateWithLifecycle()

    SearchScreen(
        query = query,
        filteredStops = filteredStops,
        favoriteIds = favoriteIds,
        allStopsSize = allStops.size,
        onQueryChange = viewModel::onQueryChange,
        onToggleFavorite = viewModel::toggleFavorite,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    query: String,
    filteredStops: List<Stop>,
    favoriteIds: Set<String>,
    allStopsSize: Int,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.search_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.search_stations_count, allStopsSize),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchField(query = query, onQueryChange = onQueryChange)

            when {
                allStopsSize == 0 -> SearchSkeleton()
                filteredStops.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Search,
                    title = stringResource(R.string.search_no_results),
                    subtitle = if (query.isBlank()) stringResource(R.string.search_loading)
                    else stringResource(R.string.search_no_match, query),
                    modifier = Modifier.fillMaxSize(),
                )
                else -> StopsList(
                    query = query,
                    stops = filteredStops,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val spacing = LocalSpacing.current
    val keyboard = LocalSoftwareKeyboardController.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenGutter, vertical = spacing.md),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.search_clear_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = PillShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { keyboard?.hide() },
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StopsList(
    query: String,
    stops: List<Stop>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    val showGroups = query.isBlank()
    val grouped = remember(stops, showGroups) {
        if (!showGroups) {
            emptyMap()
        } else {
            stops
                .groupBy { stop -> stop.name.stripDiacritics().firstOrNull()?.uppercaseChar() ?: '#' }
                .toSortedMap()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        if (showGroups) {
            grouped.forEach { (letter, groupStops) ->
                stickyHeader(key = "header-$letter") {
                    LetterHeader(
                        letter = letter.toString(),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    )
                }
                items(groupStops, key = { it.id }) { stop ->
                    StopRow(
                        stop = stop,
                        isFavorite = stop.id in favoriteIds,
                        onToggleFavorite = { onToggleFavorite(stop.id) },
                    )
                }
            }
        } else {
            items(stops, key = { it.id }) { stop ->
                StopRow(
                    stop = stop,
                    isFavorite = stop.id in favoriteIds,
                    onToggleFavorite = { onToggleFavorite(stop.id) },
                )
            }
        }
    }
}

@Composable
private fun LetterHeader(letter: String, modifier: Modifier = Modifier) {
    Text(
        text = letter,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun StopRow(
    stop: Stop,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(
        onClick = onToggleFavorite,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.md, end = spacing.xs, top = spacing.sm, bottom = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavorite) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isFavorite) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) stringResource(coreUiR.string.cd_remove_favorite) else stringResource(coreUiR.string.cd_add_favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun SearchSkeleton() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenGutter, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        repeat(8) {
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}
