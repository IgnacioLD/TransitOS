package app.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A commercial line: "Line 3", "Line 5", "Cercanías C5". A line owns one or more [Route]s.
 *
 * [color] is ARGB (0xAARRGGBB) so providers map their own color encoding into one shape.
 */
@Serializable
public data class Line(
    val id: String,
    val name: String,
    val operatorId: String,
    val mode: TransportMode,
    val color: Long? = null,
    val textColor: Long? = null,
)
