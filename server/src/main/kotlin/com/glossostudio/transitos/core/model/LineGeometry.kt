package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/** A plain WGS84 coordinate, mirroring the app's `:core` model. */
@Serializable
data class GeoCoordinate(
    val lat: Double,
    val lon: Double,
)

/** The drawable shape of a [Line], mirroring the app's `:core` model. */
@Serializable
data class LineGeometry(
    val lineId: String,
    val shortName: String,
    val color: Long? = null,
    val mode: TransportMode = TransportMode.METRO,
    val isTram: Boolean = false,
    val points: List<GeoCoordinate> = emptyList(),
    val stationIds: List<String> = emptyList(),
)
