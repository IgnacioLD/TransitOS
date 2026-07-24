package app.transitos.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transitos.core.repository.FavoritesRepository
import app.transitos.core.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the Search screen: holds the query, observes the stop catalogue and
 * the user's favourites, and exposes the derived filtered list. Toggle actions
 * are forwarded to [FavoritesRepository]; the resulting favourite-id flow
 * automatically refreshes the Home screen via its own subscription.
 */
class SearchViewModel(
    private val repository: TransitRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        repository.observeStops(),
        favorites.observeFavoriteStopIds(),
        query,
    ) { stops, favIds, q ->
        SearchUiState(
            query = q,
            allStops = stops,
            favoriteIds = favIds,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun toggleFavorite(stopId: String) {
        viewModelScope.launch {
            if (stopId in uiState.value.favoriteIds) {
                favorites.removeFavorite(stopId)
            } else {
                favorites.addFavorite(stopId)
            }
        }
    }
}
