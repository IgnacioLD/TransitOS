package app.transitos.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.transitos.core.design.theme.LocalSpacing
import app.transitos.core.ui.AlertsSection
import app.transitos.core.ui.EmptyState
import app.transitos.core.ui.ErrorState
import app.transitos.core.ui.FavoriteStopCard
import app.transitos.core.ui.FavoritesSkeleton
import app.transitos.core.ui.SectionHeader
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onNavigateToPlanner = onNavigateToPlanner,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val alertCount = (state as? HomeUiState.Ready)?.alerts?.size ?: 0

    Scaffold(
        topBar = {
            HomeTopBar(
                subtitle = networkStatusText(alertCount),
                scrollBehavior = scrollBehavior,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "home-state",
        ) { current ->
            Box(modifier = Modifier.padding(padding)) {
                when (current) {
                    HomeUiState.Loading -> FavoritesSkeleton()
                    is HomeUiState.Error -> ErrorState(error = current.error)
                    is HomeUiState.Ready -> HomeContent(state = current, onNavigateToPlanner = onNavigateToPlanner)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    subtitle: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigateToSettings: () -> Unit,
) {
    val subtitleColor = if (subtitle.startsWith("Sin avisos")) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    LargeTopAppBar(
        title = {
            Column {
                Text(
                    text = "TransitOS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = subtitleColor,
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Ajustes")
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

private fun networkStatusText(alertCount: Int): String = when {
    alertCount == 0 -> "Sin avisos en Metrovalencia"
    alertCount == 1 -> "1 línea con avisos"
    else -> "$alertCount líneas con avisos"
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = spacing.md,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (state.savedRoutes.isNotEmpty()) {
            item { SectionHeader("Rutas guardadas") }
            items(state.savedRoutes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    onClick = { onNavigateToPlanner(route.originStopId, route.destinationStopId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenGutter),
                )
            }
            item { Spacer(Modifier.height(spacing.md)) }
        }

        item { SectionHeader("Favoritos") }

        if (state.favorites.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.BookmarkAdd,
                    title = "Aún no tienes paradas favoritas",
                    subtitle = "Busca una estación y márcala para ver aquí sus próximas llegadas.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(state.favorites, key = { it.stop.id }) { favorite ->
                FavoriteStopCard(
                    stop = favorite.stop,
                    arrivals = favorite.arrivals,
                    lastUpdatedMs = favorite.lastUpdatedMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenGutter),
                )
            }
        }

        if (state.alerts.isNotEmpty()) {
            item {
                AlertsSection(
                    alerts = state.alerts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.md),
                )
            }
        }
    }
}

@Composable
private fun SavedRouteCard(
    route: SavedRouteInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Directions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.originName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = route.destinationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onClick) {
                Text("Planificar")
            }
        }
    }
}
