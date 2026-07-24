package com.glossostudio.transitos.core.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

interface ThemePreference {
    val flow: Flow<ThemeMode>
    fun current(): ThemeMode
    suspend fun set(mode: ThemeMode)
}
