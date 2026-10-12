package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/** A single predicted arrival at a [Stop], mirroring the app's `:core` model. */
@Serializable
data class Arrival(
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
