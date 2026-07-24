package app.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.transitos.core.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * DataStore Preferences-backed [FavoritesRepository].
 *
 * A single `stringSetPreferencesKey` is plenty for the MVP — a few dozen stop
 * ids at most. Migration to Room (with richer schema: ordering, custom names,
 * tags) is a future concern and would replace only this file.
 *
 * The DataStore owner is created once per [Context] via the top-level
 * [preferencesDataStore] delegate; safe to construct this repository multiple
 * times against the same application context.
 */
private val Context.transitosDataStore by preferencesDataStore(name = "transitos")

class DataStoreFavoritesRepository(
    private val context: Context,
) : FavoritesRepository {

    override fun observeFavoriteStopIds(): Flow<Set<String>> =
        context.transitosDataStore.data.map { it[FAVORITE_STOP_IDS] ?: emptySet() }

    override suspend fun addFavorite(stopId: String) {
        withContext(Dispatchers.IO) {
            context.transitosDataStore.edit { prefs ->
                prefs[FAVORITE_STOP_IDS] = (prefs[FAVORITE_STOP_IDS] ?: emptySet()) + stopId
            }
        }
    }

    override suspend fun removeFavorite(stopId: String) {
        withContext(Dispatchers.IO) {
            context.transitosDataStore.edit { prefs ->
                val current = prefs[FAVORITE_STOP_IDS] ?: emptySet()
                prefs[FAVORITE_STOP_IDS] = current - stopId
            }
        }
    }

    private companion object {
        val FAVORITE_STOP_IDS = stringSetPreferencesKey("favorite_stop_ids")
    }
}
