package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A directed variant of a [Line], e.g. "Line 3 → Rafelbunyol" and
 * "Line 3 → Aeroport" are two routes of the same line.
 */
@Serializable
public data class Route(
    val id: String,
    val lineId: String,
    val name: String,
    val direction: Direction,
) {
    public enum class Direction { OUTBOUND, INBOUND }
}
