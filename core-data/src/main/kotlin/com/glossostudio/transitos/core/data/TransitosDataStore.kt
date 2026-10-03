package com.glossostudio.transitos.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.transitosDataStore by preferencesDataStore(name = "transitos")
