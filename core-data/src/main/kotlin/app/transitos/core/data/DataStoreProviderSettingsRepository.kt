package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DataStoreProviderSettingsRepository(
    private val context: Context,
) : ProviderSettingsRepository {

    override fun observeProviderBackend(providerId: String): Flow<String> =
        context.transitosDataStore.data.map { prefs ->
            prefs[backendKey(providerId)] ?: DEFAULT_BACKEND
        }

    override suspend fun setProviderBackend(providerId: String, backendId: String) {
        withContext(Dispatchers.IO) {
            context.transitosDataStore.edit { prefs ->
                prefs[backendKey(providerId)] = backendId
            }
        }
    }

    private fun backendKey(providerId: String) = stringPreferencesKey("provider_backend_$providerId")

    private companion object {
        const val DEFAULT_BACKEND = "fgv"
    }
}
