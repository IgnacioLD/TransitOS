package com.glossostudio.transitos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.RouteFavoritesRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.core.result.AppError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: TransitRepository,
    private val favorites: FavoritesRepository,
    private val routeFavorites: RouteFavoritesRepository,
) : ViewModel() {

    private val combined: Flow<HomeUiState> = combine(
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
                HomeUiState.Ready(favorites = favs, alerts = alerts, savedRoutes = routeInfos)
            }
        }

    val uiState: StateFlow<HomeUiState> = combined
        .catch { emit(HomeUiState.Error(AppError.Unknown(it.message ?: "Error desconocido", it))) }
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
}

private data class Four<T1, T2, T3, T4>(
    val first: T1, val second: T2, val third: T3, val fourth: T4,
)
