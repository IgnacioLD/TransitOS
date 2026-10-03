package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glossostudio.transitos.core.repository.LanguagePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SharedPrefsLanguagePreference(
    private val context: Context,
) : LanguagePreference {

    override val current: String
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    override fun set(language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, language)
            .apply()
    }

    val flow: Flow<String> = context.transitosDataStore.data.map { prefs ->
        prefs[DATASTORE_KEY] ?: DEFAULT_LANGUAGE
    }

    suspend fun setAsync(language: String) {
        context.transitosDataStore.edit { prefs ->
            prefs[DATASTORE_KEY] = language
        }
    }

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val KEY = "app_language"
        const val DEFAULT_LANGUAGE = "es"
        val DATASTORE_KEY = stringPreferencesKey("app_language")
    }
}
