package app.transitos.provider.metrovalencia.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /incidencias`. The endpoint returns only line-status flags
 * — no message text, no severity, no time window — so each entry maps to an
 * [app.transitos.core.model.Alert] with [Alert.Severity.WARNING] and an empty
 * body. Good enough for the MVP's per-line indicator; human-readable alert text
 * would need a different source (NAP GTFS-RT when available, or the website's
 * editorial feed).
 */
@Serializable
data class FgvIncidenciasResponseDto(
    val incidencias: List<FgvIncidenciaDto> = emptyList(),
)

@Serializable
data class FgvIncidenciaDto(
    val id: Long,
    @kotlinx.serialization.SerialName("linea_id") val lineaId: Long,
    val fecha: String? = null,
    val sede: String? = null,
)
