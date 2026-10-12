package com.glossostudio.transitos.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** A scheduled journey, mirroring the app's `:core` model. */
@Serializable
data class Journey(
    val date: LocalDate,
    val durationMinutes: Int,
    val distanceMeters: Long,
    val fareZone: String?,
    val carbonKg: Double?,
    val legs: List<JourneyLeg>,
) {
    val departureTime: String? get() = legs.firstOrNull()?.departureTime
    val arrivalTime: String? get() = legs.lastOrNull()?.arrivalTime
    val hasTransfers: Boolean get() = legs.size > 1
}

@Serializable
data class JourneyLeg(
    val originName: String,
    val destinationName: String,
    val headsigns: List<String>,
    val lineNames: List<String> = emptyList(),
    val lineColors: List<Long> = emptyList(),
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val trainId: String? = null,
    val waitMinutes: Int? = null,
    val departures: List<String> = emptyList(),
)
