package com.glossostudio.transitos.provider.metrovalencia.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FgvIncidenciasResponseDto(
    val incidencias: List<FgvIncidenciaDto> = emptyList(),
    @SerialName("incidencias_translations") val incidenciaTranslations: List<FgvIncidenciaTranslationDto> = emptyList(),
)

@Serializable
data class FgvIncidenciaDto(
    val id: Long,
    @SerialName("linea_id") val lineaId: Long,
    val fecha: String? = null,
    val sede: String? = null,
)

@Serializable
data class FgvIncidenciaTranslationDto(
    val id: Long,
    @SerialName("incidencia_id") val incidenciaId: Long,
    val titulo: String? = null,
    val descripcion: String? = null,
    val locale: String? = null,
)
