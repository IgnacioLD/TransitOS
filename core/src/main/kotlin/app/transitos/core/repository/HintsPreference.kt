package com.glossostudio.transitos.core.repository

import kotlinx.coroutines.flow.Flow

/** Stable keys for the one-time contextual hints, one per feature screen. */
public object HintKeys {
    public const val HOME: String = "hint_home"
    public const val SEARCH: String = "hint_search"
    public const val PLANNER: String = "hint_planner"
    public val all: Set<String> = setOf(HOME, SEARCH, PLANNER)
}

/**
 * Remembers which contextual hints the user has already seen. Hints are shown
 * once, can be skipped in one go, and can be replayed from Settings.
 */
public interface HintsPreference {
    public fun observeSeen(key: String): Flow<Boolean>
    public fun isSeen(key: String): Boolean
    public suspend fun markSeen(key: String)
    public suspend fun markAllSeen()
    public suspend fun resetAll()
}
