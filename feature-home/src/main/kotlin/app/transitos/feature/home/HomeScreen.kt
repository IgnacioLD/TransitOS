package app.transitos.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val alertCount = (state as? HomeUiState.Ready)?.alerts?.size ?: 0

    Scaffold(
        topBar = { HomeTopBar(subtitle = networkStatusText(alertCount), scrollBehavior = scrollBehavior) },
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
                    is HomeUiState.Ready -> HomeContent(state = current)
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
        scrollBehavior = scrollBehavior,
    )
}

private fun networkStatusText(alertCount: Int): String = when {
    alertCount == 0 -> "Sin avisos en Metrovalencia"
    alertCount == 1 -> "1 línea con avisos"
    else -> "$alertCount líneas con avisos"
}

@Composable
private fun HomeContent(state: HomeUiState.Ready) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = spacing.md,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
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
