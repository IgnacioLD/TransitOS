package com.glossostudio.transitos.core.repository

import kotlinx.coroutines.flow.Flow

/**
 * Whether the experimental live-train simulation is shown on the map.
 *
 * This is an approximation, not a GPS feed: no FGV endpoint publishes vehicle
 * positions, so the map animates trains along the published line geometry.
 * Defaults to off so the experimental feature never surprises the user.
 */
public interface LiveTrainsPreference {
    public val flow: Flow<Boolean>
    public fun current(): Boolean
    public suspend fun set(enabled: Boolean)
}
