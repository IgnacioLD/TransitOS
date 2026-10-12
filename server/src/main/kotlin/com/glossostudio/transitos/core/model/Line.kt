package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A commercial line, mirroring the app's `:core` model. [color] is ARGB
 * (`0xAARRGGBB`).
 */
@Serializable
data class Line(
    val id: String,
    val name: String,
    val operatorId: String,
    val mode: TransportMode,
    val color: Long? = null,
    val textColor: Long? = null,
)
