package com.glossostudio.transitos.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import java.text.Normalizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: TransitRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val normalizedNames = mutableMapOf<String, String>()
    val query: StateFlow<String> = _query.asStateFlow()

    val allStops: StateFlow<List<com.glossostudio.transitos.core.model.Stop>> = repository.observeStops()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = favorites.observeFavoriteStopIds()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val filteredStops: StateFlow<List<com.glossostudio.transitos.core.model.Stop>> =
        combine(allStops, query) { stops, q ->
            if (q.isBlank()) stops
            else {
                val normalizedQuery = q.normalized()
                stops.filter { it.name.normalized().contains(normalizedQuery, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun toggleFavorite(stopId: String) {
        viewModelScope.launch {
            if (stopId in favoriteIds.value) {
                favorites.removeFavorite(stopId)
            } else {
                favorites.addFavorite(stopId)
            }
        }
    }

    private fun String.normalized(): String =
        normalizedNames.getOrPut(this) {
            Normalizer.normalize(this, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
        }
}
