package app.transitos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transitos.core.repository.FavoritesRepository
import app.transitos.core.repository.TransitRepository
import app.transitos.core.result.AppError
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

/**
 * Produces the [HomeUiState] for the Home screen.
 *
 * Favourites come from [FavoritesRepository] (DataStore-backed) — the user's
 * saved stops drive which arrival streams are observed. Stops and alerts are
 * consumed from [TransitRepository]; arrivals are fanned out one flow per
 * favourite and reduced back into a single list.
 *
 * When the user has no favourites yet, the UI shows the empty state with a
 * pointer to the Search tab.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: TransitRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val combined: Flow<HomeUiState> = combine(
        repository.observeStops(),
        favorites.observeFavoriteStopIds(),
        repository.observeAlerts(),
    ) { stops, favIds, alerts -> Triple(stops, favIds, alerts) }
        .flatMapLatest { (stops, favIds, alerts) ->
            val favoriteStops = stops.filter { it.id in favIds }
            if (favoriteStops.isEmpty()) {
                flowOf(HomeUiState.Ready(favorites = emptyList(), alerts = alerts))
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
                ).map { favs -> HomeUiState.Ready(favorites = favs, alerts = alerts) }
            }
        }

    val uiState: StateFlow<HomeUiState> = combined
        .catch { emit(HomeUiState.Error(AppError.Unknown(it.message ?: "Error desconocido", it))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )
}
