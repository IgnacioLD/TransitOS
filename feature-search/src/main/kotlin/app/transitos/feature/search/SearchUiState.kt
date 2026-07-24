package app.transitos.feature.search

import app.transitos.core.model.Stop

/**
 * Immutable UI state for the Search screen. [filteredStops] is derived from
 * [allStops] and [query] so the composable never recomputes it during
 * recomposition.
 */
data class SearchUiState(
    val query: String = "",
    val allStops: List<Stop> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
) {
    val filteredStops: List<Stop>
        get() = if (query.isBlank()) {
            allStops
        } else {
            allStops.filter { stop ->
                stop.name.contains(query, ignoreCase = true)
            }
        }
}
