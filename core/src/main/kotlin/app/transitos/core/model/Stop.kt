package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A single boarding/alighting point: a metro platform, a bus pole, a train stop.
 *
 * For grouped facilities (a metro station with multiple platforms) use
 * [parentStationId] to link sibling stops; do not model a separate Station type.
 * This keeps the model uniform across modes — a bus stop and a metro platform
 * are the same concept from the application's perspective.
 */
@Serializable
public data class Stop(
    val id: String,
    val name: String,
    val operatorId: String,
    val mode: TransportMode,
    val lat: Double?,
    val lon: Double?,
    val parentStationId: String? = null,
)
