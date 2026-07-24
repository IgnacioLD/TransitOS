package app.transitos.core.model

import kotlinx.serialization.Serializable

/**
 * A transport operator (e.g. Metrovalencia, EMT Valencia, Renfe Cercanías).
 * Providers are bound to one operator; the UI never depends on a specific one.
 */
@Serializable
public data class Operator(
    val id: String,
    val name: String,
    val mode: TransportMode,
)
