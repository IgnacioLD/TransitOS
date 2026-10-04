package com.glossostudio.transitos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.SavedRoute
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.HintKeys
import com.glossostudio.transitos.core.repository.HintsPreference
import com.glossostudio.transitos.core.repository.RouteFavoritesRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.core.result.AppError
import com.glossostudio.transitos.core.result.AppResult
import com.glossostudio.transitos.core.result.getOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: TransitRepository,
    private val favorites: FavoritesRepository,
    private val routeFavorites: RouteFavoritesRepository,
    private val hintsPreference: HintsPreference,
) : ViewModel() {

    /**
     * Bumped to rebuild the whole data pipeline. A terminal failure completes
     * the source flow, and a `stateIn(WhileSubscribed)` cache will not
     * re-subscribe while collectors stay active, so after an error we restart
     * from scratch instead: the user's "Retry" must be able to leave Error.
     */
    private val retryTrigger = MutableStateFlow(0)

    private val contentState: Flow<HomeUiState> = retryTrigger.flatMapLatest {
        flow {
            emit(HomeUiState.Loading)
            emitAll(homeData())
        }.catch { throwable ->
            emit(HomeUiState.Error(AppError.Unknown(throwable.message.orEmpty(), throwable)))
        }
    }

    // Alerts are combined beside the favourites section, not inside it, so an
    // alerts update never restarts the per-stop arrival pollers.
    private fun homeData(): Flow<HomeUiState> = combine(
        favoritesSection(),
        repository.observeAlerts(),
    ) { section, alerts ->
        when (section) {
            is FavoritesSection.Failed -> HomeUiState.Error(section.error)

            is FavoritesSection.Data -> HomeUiState.Ready(
                favorites = section.favorites,
                alerts = alerts.getOrNull().orEmpty(),
                alertsStatus = alerts.toStatus(),
                savedRoutes = section.savedRoutes,
                isLoading = section.isLoading,
            )
        }
    }

    private fun favoritesSection(): Flow<FavoritesSection> = combine(
        repository.observeStops(),
        favorites.observeFavoriteStopIds(),
        routeFavorites.observeSavedRoutes(),
    ) { stops, favIds, savedRoutes -> HomeInputs(stops, favIds, savedRoutes) }
        .flatMapLatest { inputs ->
            val needsCatalog = inputs.favoriteIds.isNotEmpty() || inputs.savedRoutes.isNotEmpty()
            when (val stopsResult = inputs.stops) {
                // Without favourites the catalogue is not needed to render Home.
                is AppResult.Failure ->
                    if (needsCatalog) flowOf(FavoritesSection.Failed(stopsResult.error)) else flowOf(EMPTY_SECTION)

                AppResult.Loading ->
                    flowOf(if (needsCatalog) EMPTY_SECTION.copy(isLoading = true) else EMPTY_SECTION)

                is AppResult.Success -> sectionFor(stopsResult.value, inputs)
            }
        }

    private fun sectionFor(stops: List<Stop>, inputs: HomeInputs): Flow<FavoritesSection> {
        val favoriteStops = stops.filter { it.id in inputs.favoriteIds }
        val stopMap = stops.associateBy { it.id }
        val routeInfos = inputs.savedRoutes.mapNotNull { sr ->
            val origin = stopMap[sr.originStopId]
            val dest = stopMap[sr.destinationStopId]
            if (origin != null && dest != null) {
                SavedRouteInfo(
                    id = sr.id,
                    originName = origin.name,
                    destinationName = dest.name,
                    originStopId = sr.originStopId,
                    destinationStopId = sr.destinationStopId,
                    label = sr.label,
                )
            } else {
                null
            }
        }
        if (favoriteStops.isEmpty()) {
            return flowOf(FavoritesSection.Data(emptyList(), routeInfos, isLoading = false))
        }
        return combine(
            flows = favoriteStops.map { stop ->
                repository.observeArrivals(stop.id).map { result -> result.toFavoriteArrivals(stop) }
            },
            transform = { array -> FavoritesSection.Data(array.toList(), routeInfos, isLoading = false) },
        )
    }

    private fun AppResult<List<Arrival>>.toFavoriteArrivals(stop: Stop) = when (this) {
        is AppResult.Success -> FavoriteArrivals(stop, value, lastUpdatedMs = System.currentTimeMillis())
        is AppResult.Failure -> FavoriteArrivals(stop, emptyList(), error = error)
        AppResult.Loading -> FavoriteArrivals(stop, emptyList(), isLoading = true)
    }

    private fun AppResult<List<Alert>>.toStatus(): AlertsStatus = when (this) {
        is AppResult.Success -> AlertsStatus.Loaded
        is AppResult.Failure -> AlertsStatus.Unavailable(error)
        AppResult.Loading -> AlertsStatus.Loading
    }

    private val isRefreshing = MutableStateFlow(false)

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            // If the last attempt failed, rebuild the pipeline so the stream is
            // live again; otherwise a plain repository refresh is enough and
            // avoids flashing the content back to Loading.
            if (uiState.value is HomeUiState.Error) {
                retryTrigger.update { it + 1 }
            }
            repository.refresh()
            // The refresh signal already kicked off an immediate background
            // re-fetch. Show the spinner for a brief, fixed beat for feedback
            // then dismiss — never waiting on data arrival, which can be
            // delayed/deduped and would make the spinner feel stuck.
            delay(REFRESH_MIN_MS)
            isRefreshing.value = false
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        contentState,
        isRefreshing,
    ) { state, refreshing ->
        if (state is HomeUiState.Ready) {
            state.copy(isRefreshing = refreshing)
        } else {
            state
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    fun renameRoute(routeId: String, label: String) {
        viewModelScope.launch {
            routeFavorites.renameRoute(routeId, label)
        }
    }

    val showHint: StateFlow<Boolean> = hintsPreference.observeSeen(HintKeys.HOME)
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = !hintsPreference.isSeen(HintKeys.HOME),
        )

    fun dismissHint() {
        viewModelScope.launch { hintsPreference.markSeen(HintKeys.HOME) }
    }

    fun skipHints() {
        viewModelScope.launch { hintsPreference.markAllSeen() }
    }

    private companion object {
        /** How long the pull-to-refresh spinner stays up for feedback. */
        const val REFRESH_MIN_MS = 600L
    }
}

/** Everything the favourites section is derived from, combined into one emission. */
private data class HomeInputs(
    val stops: AppResult<List<Stop>>,
    val favoriteIds: Set<String>,
    val savedRoutes: List<SavedRoute>,
)

private sealed interface FavoritesSection {
    data class Failed(val error: AppError) : FavoritesSection
    data class Data(
        val favorites: List<FavoriteArrivals>,
        val savedRoutes: List<SavedRouteInfo>,
        val isLoading: Boolean,
    ) : FavoritesSection
}

private val EMPTY_SECTION = FavoritesSection.Data(emptyList(), emptyList(), isLoading = false)
