package com.glossostudio.transitos.core.repository

import com.glossostudio.transitos.core.model.SavedRoute
import kotlinx.coroutines.flow.Flow

public interface RouteFavoritesRepository {
    public fun observeSavedRoutes(): Flow<List<SavedRoute>>
    public suspend fun saveRoute(route: SavedRoute)
    public suspend fun removeRoute(routeId: String)
    public suspend fun renameRoute(routeId: String, label: String)
}
