package com.glossostudio.transitos.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.LocalSpacing
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.ui.AlertsSection
import com.glossostudio.transitos.core.ui.DepartureBoard
import com.glossostudio.transitos.core.ui.EmptyState
import com.glossostudio.transitos.core.ui.ErrorState
import com.glossostudio.transitos.core.ui.FavoriteStopCard
import com.glossostudio.transitos.core.ui.FavoriteStopCardSkeleton
import com.glossostudio.transitos.core.ui.FavoritesSkeleton
import com.glossostudio.transitos.core.ui.HintCard
import com.glossostudio.transitos.core.ui.SectionHeader
import com.glossostudio.transitos.core.ui.ShareDialog
import com.glossostudio.transitos.core.ui.StatusPill
import com.glossostudio.transitos.core.ui.PLAY_STORE_URL
import com.glossostudio.transitos.core.ui.sharePlainText
import com.glossostudio.transitos.core.ui.R as coreUiR
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showHint by viewModel.showHint.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(coreUiR.string.share_chooser_title)
    var shareRoute by remember { mutableStateOf<SavedRouteInfo?>(null) }
    HomeScreen(
        state = state,
        showHint = showHint,
        onDismissHint = viewModel::dismissHint,
        onSkipHints = viewModel::skipHints,
        onNavigateToPlanner = onNavigateToPlanner,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToSearch = onNavigateToSearch,
        onRenameRoute = viewModel::renameRoute,
        onShareSavedRoute = { shareRoute = it },
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )

    shareRoute?.let { route ->
        ShareDialog(
            title = stringResource(coreUiR.string.share_dialog_title),
            message = context.getString(
                R.string.home_share_route_body,
                route.originName,
                route.destinationName,
                PLAY_STORE_URL,
            ),
            messageHint = stringResource(coreUiR.string.share_dialog_message),
            shareLabel = stringResource(coreUiR.string.action_share),
            cancelLabel = stringResource(coreUiR.string.action_cancel),
            onDismiss = { shareRoute = null },
            onShare = { text ->
                sharePlainText(context, text, chooserTitle)
                shareRoute = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {},
    onSkipHints: () -> Unit = {},
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onRenameRoute: (routeId: String, label: String) -> Unit = { _, _ -> },
    onShareSavedRoute: (SavedRouteInfo) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            HomeTopBar(
                scrollBehavior = scrollBehavior,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        // The host Scaffold (TransitOSApp) already insets content above the
        // bottom navigation bar, so this nested Scaffold must not re-apply the
        // system bar insets, otherwise the bottom inset is counted twice.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "home-state",
        ) { current ->
            Box(modifier = Modifier.padding(padding)) {
                when (current) {
                    HomeUiState.Loading -> FavoritesSkeleton()
                    is HomeUiState.Error -> ErrorState(error = current.error, onRetry = onRefresh)
                    is HomeUiState.Ready -> {
                        PullToRefreshBox(
                            isRefreshing = current.isRefreshing,
                            onRefresh = onRefresh,
                        ) {
                            HomeContent(
                                state = current,
                                showHint = showHint,
                                onDismissHint = onDismissHint,
                                onSkipHints = onSkipHints,
                                onNavigateToPlanner = onNavigateToPlanner,
                                onNavigateToSearch = onNavigateToSearch,
                                onRenameRoute = onRenameRoute,
                                onShareSavedRoute = onShareSavedRoute,
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
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigateToSettings: () -> Unit,
) {
    // Single-row title only. The bar must stay within one line height so its
    // opaque background always covers its full height while collapsed and the
    // list can never show through behind the title.
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandMark()
                Text(
                    text = stringResource(R.string.home_app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            FilledTonalIconButton(
                onClick = onNavigateToSettings,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_cd),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.DirectionsTransit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun AlertStatusPill(alertCount: Int, modifier: Modifier = Modifier) {
    if (alertCount == 0) {
        StatusPill(
            text = stringResource(R.string.home_no_alerts),
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leading = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            },
        )
    } else {
        val text = if (alertCount == 1) {
            stringResource(R.string.home_alerts_singular)
        } else {
            stringResource(R.string.home_alerts_plural, alertCount)
        }
        StatusPill(
            text = text,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            leading = {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    showHint: Boolean,
    onDismissHint: () -> Unit,
    onSkipHints: () -> Unit,
    onNavigateToPlanner: (originStopId: String, destinationStopId: String) -> Unit = { _, _ -> },
    onNavigateToSearch: () -> Unit = {},
    onRenameRoute: (routeId: String, label: String) -> Unit = { _, _ -> },
    onShareSavedRoute: (SavedRouteInfo) -> Unit = {},
) {
    val spacing = LocalSpacing.current
    val savedRoutesTitle = stringResource(R.string.home_saved_routes)
    val favoritesTitle = stringResource(R.string.home_favorites)
    val moreStopsTitle = stringResource(R.string.home_more_stops)
    val noFavoritesTitle = stringResource(R.string.home_no_favorites_title)
    val noFavoritesSubtitle = stringResource(R.string.home_no_favorites_subtitle)
    val searchAction = stringResource(R.string.home_search_stations)

    // The soonest upcoming departure across every favourite becomes the hero.
    val heroPair: Pair<FavoriteArrivals, Arrival>? = remember(state.favorites) {
        state.favorites
            .mapNotNull { favorite ->
                favorite.arrivals
                    .minByOrNull { it.minutesAway ?: Int.MAX_VALUE }
                    ?.let { favorite to it }
            }
            .minByOrNull { (_, arrival) -> arrival.minutesAway ?: Int.MAX_VALUE }
    }
    val heroStopId = heroPair?.first?.stop?.id
    val otherFavorites = state.favorites.filter { it.stop.id != heroStopId }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (showHint) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HintCard(
                    title = stringResource(R.string.home_hint_title),
                    text = stringResource(R.string.home_hint_body),
                    icon = Icons.Outlined.Star,
                    dismissLabel = stringResource(coreUiR.string.hint_dismiss),
                    skipLabel = stringResource(coreUiR.string.hint_skip),
                    onDismiss = onDismissHint,
                    onSkip = onSkipHints,
                    modifier = Modifier.padding(
                        start = spacing.screenGutter,
                        end = spacing.screenGutter,
                        top = spacing.xs,
                    ),
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            // A lazy-grid item pins the cross-axis width, so the pill is wrapped
            // in a Row to let it hug its content instead of stretching.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.screenGutter,
                        end = spacing.screenGutter,
                        top = spacing.sm,
                    ),
            ) {
                AlertStatusPill(alertCount = state.alerts.size)
            }
        }

        if (heroPair != null) {
            val (favorite, next) = heroPair
            item(span = { GridItemSpan(maxLineSpan) }) {
                DepartureBoard(
                    stopName = favorite.stop.name,
                    next = next,
                    upcoming = favorite.arrivals.filter { it != next },
                    lastUpdatedMs = favorite.lastUpdatedMs,
                    modifier = Modifier.padding(
                        start = spacing.screenGutter,
                        end = spacing.screenGutter,
                        top = spacing.xs,
                    ),
                )
            }
        }

        if (state.savedRoutes.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(text = savedRoutesTitle)
            }
            items(state.savedRoutes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    onClick = { onNavigateToPlanner(route.originStopId, route.destinationStopId) },
                    onRename = { label -> onRenameRoute(route.id, label) },
                    onShare = { onShareSavedRoute(route) },
                    modifier = Modifier.padding(horizontal = spacing.screenGutter),
                )
            }
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(text = favoritesTitle)
            }
            items(listOf(0, 1), key = { "skeleton-$it" }) {
                FavoriteStopCardSkeleton(
                    modifier = Modifier.padding(horizontal = spacing.screenGutter),
                )
            }
        } else if (state.favorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(text = favoritesTitle)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = noFavoritesTitle,
                    subtitle = noFavoritesSubtitle,
                    modifier = Modifier.fillMaxWidth(),
                    action = {
                        FilledTonalButton(onClick = onNavigateToSearch) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(searchAction)
                        }
                    },
                )
            }
        } else if (otherFavorites.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(text = if (heroPair != null) moreStopsTitle else favoritesTitle)
            }
            items(otherFavorites, key = { it.stop.id }) { favorite ->
                FavoriteStopCard(
                    stop = favorite.stop,
                    arrivals = favorite.arrivals,
                    lastUpdatedMs = favorite.lastUpdatedMs,
                    modifier = Modifier.padding(horizontal = spacing.screenGutter),
                )
            }
        }

        if (state.alerts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
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
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, top = spacing.md, bottom = spacing.md, end = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            RouteRail()
            Column(modifier = Modifier.weight(1f)) {
                if (route.label.isNotBlank()) {
                    Text(
                        text = route.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = route.originName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = route.destinationName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.home_share_route),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.home_rename_route),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Origin/destination dots joined by a connector, the classic route motif. */
@Composable
private fun RouteRail() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
    }
}
