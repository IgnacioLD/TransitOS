package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A plain WGS84 coordinate. Kept free of any map SDK type so the domain layer
 * stays platform agnostic.
 */
@Serializable
public data class GeoCoordinate(
    val lat: Double,
    val lon: Double,
)

/**
 * The drawable shape of a [Line]: the real track polyline plus the ordered
 * stations that sit on it.
 *
 * [points] is the authoritative geometry as published by the operator (not a
 * hand-traced approximation), so a map can draw the line and know it follows
 * the actual alignment. [stationIds] are canonical stop ids in travel order and
 * let the UI cross-reference geometry with the stops catalogue. Both may be
 * empty when a provider does not publish geometry.
 */
@Serializable
public data class LineGeometry(
    val lineId: String,
    val shortName: String,
    val color: Long? = null,
    val mode: TransportMode = TransportMode.METRO,
    val isTram: Boolean = false,
    val points: List<GeoCoordinate> = emptyList(),
    val stationIds: List<String> = emptyList(),
)
