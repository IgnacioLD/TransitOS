package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.glossostudio.transitos.core.repository.TransferBufferPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreTransferBufferPreference(private val context: Context) : TransferBufferPreference {

    override val flow: Flow<Int> = context.transitosDataStore.data.map { prefs ->
        prefs[KEY] ?: DEFAULT
    }

    override fun current(): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(SHARED_KEY, DEFAULT)

    override suspend fun set(minutes: Int) {
        val clamped = minutes.coerceIn(MIN, MAX)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(SHARED_KEY, clamped).apply()
        context.transitosDataStore.edit { it[KEY] = clamped }
    }

    private companion object {
        const val PREFS_NAME = "transitos_prefs"
        const val SHARED_KEY = "transfer_buffer_minutes"
        val KEY = intPreferencesKey("transfer_buffer_minutes")
        const val DEFAULT = 5
        const val MIN = 0
        const val MAX = 15
    }
}
