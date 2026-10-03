package com.glossostudio.transitos.provider.metrovalencia.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /estaciones`. Field names are exactly what FGV returns.
 *
 * Two ids are present:
 *  - [id] is FGV's internal id; the journey planner consumes it.
 *  - [estacionIdFgv] is FGV's station id; `horarios-prevision-3` and `lineas.stops`
 *    consume it. We expose this as the canonical `Stop.id` because live arrivals
 *    need it.
 */
@Serializable
data class FgvStationDto(
    val id: Long? = null,
    @SerialName("estacion_id_FGV") val estacionIdFgv: Long,
    val nombre: String,
    val transbordo: Int? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val direccion: String? = null,
    val sede: String? = null,
)
