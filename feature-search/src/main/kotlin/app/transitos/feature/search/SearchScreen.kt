package app.transitos.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.model.Stop
import app.transitos.core.ui.EmptyState
import app.transitos.core.ui.SkeletonBlock
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Buscar",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "$allStopsSize estaciones",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
                    title = "Sin resultados",
                    subtitle = if (query.isBlank()) "Cargando estaciones…"
                    else "No hay estaciones que coincidan con «${query}».",
                )
                else -> StopsList(
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
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenGutter, vertical = spacing.md),
        placeholder = { Text("Estación, línea o destino") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun StopsList(
    stops: List<Stop>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        items(stops, key = { it.id }) { stop ->
            val isFavorite = remember(stop.id, favoriteIds) { stop.id in favoriteIds }
            StopRow(
                stop = stop,
                isFavorite = isFavorite,
                onToggleFavorite = { onToggleFavorite(stop.id) },
            )
        }
    }
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}
