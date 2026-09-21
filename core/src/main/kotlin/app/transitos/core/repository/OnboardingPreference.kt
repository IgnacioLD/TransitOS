package com.glossostudio.transitos.core.repository

import kotlinx.coroutines.flow.Flow

/**
 * Tracks whether the new-user onboarding has been seen. The first launch shows
 * the guided tour; afterwards the flag stays set unless the user replays it from
 * Settings.
 */
public interface OnboardingPreference {
    public val flow: Flow<Boolean>
    public fun current(): Boolean
    public suspend fun setCompleted(completed: Boolean)
}
