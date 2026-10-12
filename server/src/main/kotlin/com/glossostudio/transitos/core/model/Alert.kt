package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/** A service alert, mirroring the app's `:core` model. */
@Serializable
data class Alert(
    val id: String,
    val operatorId: String,
    val lineIds: Set<String> = emptySet(),
    val stopIds: Set<String> = emptySet(),
    val severity: Severity,
    val title: String,
    val body: String? = null,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val lineShortName: String? = null,
    val lineColor: Long? = null,
) {
    enum class Severity { INFO, WARNING, SEVERE }
}
