package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

@Serializable
public data class SavedRoute(
    val id: String,
    val originStopId: String,
    val destinationStopId: String,
    val label: String = "",
)
