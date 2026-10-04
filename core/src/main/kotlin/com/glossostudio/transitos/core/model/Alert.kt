package com.glossostudio.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A service alert affecting one or more lines/stops. Mapped from GTFS-Realtime
 * ServiceAlerts where the provider exposes them, or from operator-specific feeds.
 *
 * [lineShortName] and [lineColor] are denormalised display hints populated when
 * the alert affects exactly one line, lets the UI render a line badge without
 * a separate lookup table. Multi-line alerts leave them null.
 */
@Serializable
public data class Alert(
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
    public enum class Severity { INFO, WARNING, SEVERE }
}
