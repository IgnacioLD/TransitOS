package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.glossostudio.transitos.core.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
