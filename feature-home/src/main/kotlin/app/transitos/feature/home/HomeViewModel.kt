package com.glossostudio.transitos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.HintKeys
import com.glossostudio.transitos.core.repository.HintsPreference
import com.glossostudio.transitos.core.repository.RouteFavoritesRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.core.result.AppError
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

    private fun homeData(): Flow<HomeUiState> = combine(
        repository.observeStops(),
        favorites.observeFavoriteStopIds(),
        repository.observeAlerts(),
        routeFavorites.observeSavedRoutes(),
    ) { stops, favIds, alerts, savedRoutes -> Four(stops, favIds, alerts, savedRoutes) }
        .flatMapLatest { (stops, favIds, alerts, savedRoutes) ->
            val favoriteStops = stops.filter { it.id in favIds }
            val stopMap = stops.associateBy { it.id }
            val routeInfos = savedRoutes.mapNotNull { sr ->
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
                } else null
            }

            val arrivalsFlow = if (favoriteStops.isEmpty()) {
                flowOf(emptyList<FavoriteArrivals>())
            } else {
                combine(
                    flows = favoriteStops.map { stop ->
                        repository.observeArrivals(stop.id).map { arrivals ->
                            FavoriteArrivals(
                                stop = stop,
                                arrivals = arrivals,
                                lastUpdatedMs = System.currentTimeMillis(),
                            )
                        }
                    },
                    transform = { array -> array.toList() },
                )
            }
            arrivalsFlow.map { favs ->
                HomeUiState.Ready(
                    favorites = favs,
                    alerts = alerts,
                    savedRoutes = routeInfos,
                    isLoading = favs.isEmpty() && favIds.isNotEmpty(),
                )
            }
        }

    private val _isRefreshing = MutableStateFlow(false)

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
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
            _isRefreshing.value = false
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        contentState,
        _isRefreshing,
    ) { state, refreshing ->
        if (state is HomeUiState.Ready) state.copy(isRefreshing = refreshing)
        else state
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

private data class Four<T1, T2, T3, T4>(
    val first: T1, val second: T2, val third: T3, val fourth: T4,
)
