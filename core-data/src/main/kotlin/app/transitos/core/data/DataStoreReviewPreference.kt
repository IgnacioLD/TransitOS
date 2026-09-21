package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.glossostudio.transitos.core.repository.ReviewPreference

class DataStoreReviewPreference(
    private val context: Context,
) : ReviewPreference {

    override fun hasBeenRequested(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SHARED_KEY, false)

    override suspend fun markRequested() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(SHARED_KEY, true).apply()
        context.transitosDataStore.edit { it[KEY] = true }
    }

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val SHARED_KEY = "review_requested"
        val KEY = booleanPreferencesKey("review_requested")
    }
}
