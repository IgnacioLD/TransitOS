package app.transitos.feature.home

import app.transitos.core.model.Alert
import app.transitos.core.model.Arrival
import app.transitos.core.model.Stop
import app.transitos.core.result.AppError

/**
 * Immutable UI state for the Home screen. The ViewModel produces a single
 * [HomeUiState] at a time; Compose renders by switching on it.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val favorites: List<FavoriteArrivals>,
        val alerts: List<Alert>,
    ) : HomeUiState

    data class Error(val error: AppError) : HomeUiState
}

/**
 * A favorited stop plus its upcoming arrivals, grouped for display.
 *
 * [lastUpdatedMs] is the moment the arrivals list was fetched — the card shows
 * "actualizado hace Xs" so the user can tell stale data from fresh without
 * staring at a clock.
 */
data class FavoriteArrivals(
    val stop: Stop,
    val arrivals: List<Arrival>,
    val lastUpdatedMs: Long = 0L,
)
