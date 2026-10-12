package com.glossostudio.transitos.server.api

import kotlinx.serialization.Serializable

/** An operator the server can serve, with its selectable data backends. */
@Serializable
data class OperatorInfo(
    val id: String,
    val name: String,
    val backends: List<BackendInfo>,
)

/** A selectable data source for an operator (e.g. FGV realtime vs NAP). */
@Serializable
data class BackendInfo(
    val id: String,
    val displayName: String,
)

/** Uniform error body for non-2xx responses. */
@Serializable
data class ErrorResponse(
    val error: String,
)
