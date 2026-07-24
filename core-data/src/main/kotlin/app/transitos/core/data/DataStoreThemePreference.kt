package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.repository.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreThemePreference(
    private val context: Context,
) : ThemePreference {

    override val flow: Flow<ThemeMode> = context.transitosDataStore.data.map { prefs ->
        prefs[KEY]?.toThemeMode() ?: ThemeMode.SYSTEM
    }

    override fun current(): ThemeMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SHARED_KEY, null)
        return raw?.toThemeMode() ?: ThemeMode.SYSTEM
    }

    override suspend fun set(mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(SHARED_KEY, mode.name).apply()
        context.transitosDataStore.edit { it[KEY] = mode.name }
    }

    private fun String.toThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val SHARED_KEY = "app_theme"
        val KEY = stringPreferencesKey("app_theme")
    }
}
