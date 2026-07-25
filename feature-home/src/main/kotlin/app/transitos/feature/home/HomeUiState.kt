package com.glossostudio.transitos.feature.home

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.result.AppError

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val favorites: List<FavoriteArrivals>,
        val alerts: List<Alert>,
        val savedRoutes: List<SavedRouteInfo> = emptyList(),
        val isRefreshing: Boolean = false,
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
    val label: String = "",
)
