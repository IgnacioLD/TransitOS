package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.glossostudio.transitos.core.repository.LiveTrainsPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreLiveTrainsPreference(
    private val context: Context,
) : LiveTrainsPreference {

    override val flow: Flow<Boolean> = context.transitosDataStore.data.map { prefs ->
        prefs[KEY] ?: false
    }

    override fun current(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SHARED_KEY, false)

    override suspend fun set(enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(SHARED_KEY, enabled).apply()
        context.transitosDataStore.edit { it[KEY] = enabled }
    }

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val SHARED_KEY = "live_trains_enabled"
        val KEY = booleanPreferencesKey("live_trains_enabled")
    }
}
