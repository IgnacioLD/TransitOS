package app.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A physical vehicle currently in service. Used for map views and live tracking.
 * Fields beyond identity are optional because not every provider exposes them.
 */
@Serializable
public data class Vehicle(
    val id: String,
    val lineId: String,
    val mode: TransportMode,
    val lat: Double? = null,
    val lon: Double? = null,
    val bearing: Float? = null,
)
