package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/** A single boarding/alighting point, mirroring the app's `:core` model. */
@Serializable
data class Stop(
    val id: String,
    val name: String,
    val operatorId: String,
    val mode: TransportMode,
    val lat: Double?,
    val lon: Double?,
    val parentStationId: String? = null,
)
