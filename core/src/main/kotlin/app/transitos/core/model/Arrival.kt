package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A single predicted arrival at a [Stop].
 *
 * Prefer [minutesAway] for display; [estimatedEpochMs] is the absolute timestamp
 * when present. [isRealTime] distinguishes live predictions from timetable-only
 * data so the UI can badge them differently.
 *
 * [lineShortName] and [lineColor] are denormalised display hints so the UI can
 * render a line badge without a separate lookup table, every provider that
 * knows the line identity (most do) populates them; providers that don't leave
 * them null and the UI falls back to neutral styling.
 */
@Serializable
public data class Arrival(
    val stopId: String,
    val lineId: String,
    val destination: String,
    val estimatedEpochMs: Long? = null,
    val minutesAway: Int? = null,
    val vehicleId: String? = null,
    val isRealTime: Boolean,
    val lineShortName: String? = null,
    val lineColor: Long? = null,
)
