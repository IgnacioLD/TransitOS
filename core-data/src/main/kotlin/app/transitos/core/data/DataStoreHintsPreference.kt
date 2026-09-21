package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.glossostudio.transitos.core.repository.HintKeys
import com.glossostudio.transitos.core.repository.HintsPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreHintsPreference(
    private val context: Context,
) : HintsPreference {

    override fun observeSeen(key: String): Flow<Boolean> =
        context.transitosDataStore.data.map { prefs -> key in (prefs[KEY] ?: emptySet()) }

    override fun isSeen(key: String): Boolean = seenSet().contains(key)

    override suspend fun markSeen(key: String) {
        updateSet { it + key }
    }

    override suspend fun markAllSeen() {
        updateSet { it + HintKeys.all }
    }

    override suspend fun resetAll() {
        updateSet { emptySet() }
    }

    private fun seenSet(): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(SHARED_KEY, emptySet())
            ?.toSet()
            .orEmpty()

    private suspend fun updateSet(transform: (Set<String>) -> Set<String>) {
        val updated = transform(seenSet())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(SHARED_KEY, updated).apply()
        context.transitosDataStore.edit { it[KEY] = updated }
    }

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val SHARED_KEY = "seen_hints"
        val KEY = stringSetPreferencesKey("seen_hints")
    }
}
