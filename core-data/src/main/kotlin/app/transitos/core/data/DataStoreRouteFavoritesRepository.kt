package app.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.transitos.core.model.SavedRoute
import app.transitos.core.repository.RouteFavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreRouteFavoritesRepository(
    private val context: Context,
) : RouteFavoritesRepository {

    override fun observeSavedRoutes(): Flow<List<SavedRoute>> =
        context.transitosDataStore.data.map { prefs ->
            val json = prefs[ROUTES_KEY] ?: return@map emptyList()
            runCatching { Json.decodeFromString<List<SavedRoute>>(json) }.getOrDefault(emptyList())
        }

    override suspend fun saveRoute(route: SavedRoute) {
        withContext(Dispatchers.IO) {
            context.transitosDataStore.edit { prefs ->
                val current = prefs[ROUTES_KEY]?.let { json ->
                    runCatching { Json.decodeFromString<List<SavedRoute>>(json) }.getOrDefault(emptyList())
                } ?: emptyList()
                val updated = current.filter { it.id != route.id } + route
                prefs[ROUTES_KEY] = Json.encodeToString(updated)
            }
        }
    }

    override suspend fun removeRoute(routeId: String) {
        withContext(Dispatchers.IO) {
            context.transitosDataStore.edit { prefs ->
                val current = prefs[ROUTES_KEY]?.let { json ->
                    runCatching { Json.decodeFromString<List<SavedRoute>>(json) }.getOrDefault(emptyList())
                } ?: emptyList()
                val updated = current.filter { it.id != routeId }
                prefs[ROUTES_KEY] = Json.encodeToString(updated)
            }
        }
    }

    private companion object {
        val ROUTES_KEY = stringPreferencesKey("favorite_routes")
    }
}
