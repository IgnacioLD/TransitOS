package com.glossostudio.transitos.core.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persisted user favourites — the canonical source of "which stops the user
 * cares about". Lives in the domain layer so the contract is independent of
 * the storage mechanism (DataStore today, Room tomorrow, sync eventually).
 *
 * Implementations must be safe to call from the main thread; I/O happens on
 * Dispatchers.IO internally. Stop ids follow the provider's canonical id space
 * (e.g. `"mv:12"` for Metrovalencia's Benimaclet).
 */
public interface FavoritesRepository {
    public fun observeFavoriteStopIds(): Flow<Set<String>>
    public suspend fun addFavorite(stopId: String)
    public suspend fun removeFavorite(stopId: String)
}
