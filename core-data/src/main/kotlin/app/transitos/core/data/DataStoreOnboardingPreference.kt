package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.glossostudio.transitos.core.repository.OnboardingPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreOnboardingPreference(
    private val context: Context,
) : OnboardingPreference {

    override val flow: Flow<Boolean> = context.transitosDataStore.data.map { prefs ->
        prefs[KEY] ?: false
    }

    override fun current(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SHARED_KEY, false)

    override suspend fun setCompleted(completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(SHARED_KEY, completed).apply()
        context.transitosDataStore.edit { it[KEY] = completed }
    }

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val SHARED_KEY = "onboarding_completed"
        val KEY = booleanPreferencesKey("onboarding_completed")
    }
}
