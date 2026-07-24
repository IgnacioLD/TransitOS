package com.glossostudio.transitos.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * A scheduled journey between two stops on a given [date]. Composed of one or
 * more [JourneyLeg]s, direct trips have a single leg, transfers have several.
 */
@Serializable
public data class Journey(
    public val date: LocalDate,
    public val durationMinutes: Int,
    public val distanceMeters: Long,
    public val fareZone: String?,
    public val carbonKg: Double?,
    public val legs: List<JourneyLeg>,
) {
    public val departureTime: String? get() = legs.firstOrNull()?.departureTime
    public val arrivalTime: String? get() = legs.lastOrNull()?.arrivalTime
    public val hasTransfers: Boolean get() = legs.size > 1
}

@Serializable
public data class JourneyLeg(
    public val originName: String,
    public val destinationName: String,
    public val headsigns: List<String>,
    public val lineNames: List<String> = emptyList(),
    public val lineColors: List<Long> = emptyList(),
    public val departureTime: String? = null,
    public val arrivalTime: String? = null,
    public val trainId: String? = null,
    public val waitMinutes: Int? = null,
    public val departures: List<String> = emptyList(),
)
