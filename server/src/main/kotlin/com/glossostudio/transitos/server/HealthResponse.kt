package com.glossostudio.transitos.server

import kotlinx.serialization.Serializable

/** Body of `GET /health`, used by the reverse proxy and monitoring. */
@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
)
