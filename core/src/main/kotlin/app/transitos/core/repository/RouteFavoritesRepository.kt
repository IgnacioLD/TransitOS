package app.transitos.core.repository

import app.transitos.core.model.SavedRoute
import kotlinx.coroutines.flow.Flow

public interface RouteFavoritesRepository {
    public fun observeSavedRoutes(): Flow<List<SavedRoute>>
    public suspend fun saveRoute(route: SavedRoute)
    public suspend fun removeRoute(routeId: String)
}
