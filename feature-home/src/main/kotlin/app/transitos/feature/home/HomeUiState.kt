package app.transitos.feature.home

import app.transitos.core.model.Alert
import app.transitos.core.model.Arrival
import app.transitos.core.model.Stop
import app.transitos.core.result.AppError

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val favorites: List<FavoriteArrivals>,
        val alerts: List<Alert>,
        val savedRoutes: List<SavedRouteInfo> = emptyList(),
    ) : HomeUiState

    data class Error(val error: AppError) : HomeUiState
}

data class FavoriteArrivals(
    val stop: Stop,
    val arrivals: List<Arrival>,
    val lastUpdatedMs: Long = 0L,
)

data class SavedRouteInfo(
    val id: String,
    val originName: String,
    val destinationName: String,
    val originStopId: String,
    val destinationStopId: String,
)
