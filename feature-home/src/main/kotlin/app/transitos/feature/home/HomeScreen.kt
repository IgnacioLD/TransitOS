package com.glossostudio.transitos.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.ui.AlertsSection
import com.glossostudio.transitos.core.ui.EmptyState
import com.glossostudio.transitos.core.ui.ErrorState
import com.glossostudio.transitos.core.ui.FavoriteStopCard
import com.glossostudio.transitos.core.ui.FavoritesSkeleton
import com.glossostudio.transitos.core.ui.SectionHeader
import com.glossostudio.transitos.core.ui.R as coreUiR
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
        onRenameRoute = viewModel::renameRoute,
        onRefresh = viewModel::refresh,
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
    onRenameRoute: (routeId: String, label: String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val alertCount = (state as? HomeUiState.Ready)?.alerts?.size ?: 0

    Scaffold(
        topBar = {
            HomeTopBar(
                alertCount = alertCount,
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
                    is HomeUiState.Ready -> {
                        PullToRefreshBox(
                            isRefreshing = current.isRefreshing,
                            onRefresh = onRefresh,
                        ) {
                            HomeContent(
                                state = current,
                                onNavigateToPlanner = onNavigateToPlanner,
                                onRenameRoute = onRenameRoute,
                            )
                        }
                    }
                }
            }
        }
    }
}
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    alertCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigateToSettings: () -> Unit,
) {
    val subtitle = when {
        alertCount == 0 -> stringResource(R.string.home_no_alerts)
        alertCount == 1 -> stringResource(R.string.home_alerts_singular)
        else -> stringResource(R.string.home_alerts_plural, alertCount)
    }
    val subtitleColor = if (alertCount == 0) {
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
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_cd))
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
    onRenameRoute: (routeId: String, label: String) -> Unit = { _, _ -> },
) {
    val spacing = LocalSpacing.current
    val savedRoutesTitle = stringResource(R.string.home_saved_routes)
    val favoritesTitle = stringResource(R.string.home_favorites)
    val noFavoritesTitle = stringResource(R.string.home_no_favorites_title)
    val noFavoritesSubtitle = stringResource(R.string.home_no_favorites_subtitle)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = spacing.md,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (state.savedRoutes.isNotEmpty()) {
            item { SectionHeader(savedRoutesTitle) }
            items(state.savedRoutes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    onClick = { onNavigateToPlanner(route.originStopId, route.destinationStopId) },
                    onRename = { label -> onRenameRoute(route.id, label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenGutter),
                )
            }
            item { Spacer(Modifier.height(spacing.md)) }
        }

        item { SectionHeader(favoritesTitle) }

        if (state.favorites.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.BookmarkAdd,
                    title = noFavoritesTitle,
                    subtitle = noFavoritesSubtitle,
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
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

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
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Directions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                if (route.label.isNotBlank()) {
                    Text(
                        text = route.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${route.originName} → ${route.destinationName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
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
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.home_rename_route),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (showRenameDialog) {
        var text by remember { mutableStateOf(route.label) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.home_rename_route_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.home_rename_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(text.trim())
                    showRenameDialog = false
                }) { Text(stringResource(coreUiR.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(coreUiR.string.action_cancel))
                }
            },
        )
    }
}
