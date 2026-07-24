package com.glossostudio.transitos.provider.metrovalencia.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /horarios-prevision-3/{id}`. The outer object also carries
 * station-occupancy configuration which is not modelled here, we only consume
 * [previsiones] for the MVP.
 */
@Serializable
data class FgvArrivalResponseDto(
    val previsiones: List<FgvPrevisionDto> = emptyList(),
)

@Serializable
data class FgvPrevisionDto(
    val line: Long? = null,
    @SerialName("line_id") val lineId: Long? = null,
    @SerialName("linea_id_interna") val lineaIdInterna: Long? = null,
    val trains: List<FgvTrainDto> = emptyList(),
)

@Serializable
data class FgvTrainDto(
    val cabecera: Boolean? = null,
    val destino: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val meters: Double? = null,
    val seconds: Long,
    val vehicle: Long? = null,
    val capacity: Long? = null,
    @SerialName("line_id") val lineId: Long? = null,
)
