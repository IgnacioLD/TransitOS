package app.transitos.feature.search

import app.transitos.core.model.Stop

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
