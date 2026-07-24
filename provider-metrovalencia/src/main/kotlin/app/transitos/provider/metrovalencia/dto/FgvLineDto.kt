package com.glossostudio.transitos.provider.metrovalencia.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /lineas`. [stops] is a CSV of `estacion_id_FGV` values in
 * line order — useful for future route/transfer modelling.
 */
@Serializable
data class FgvLineDto(
    val id: Long? = null,
    @SerialName("linea_id_FGV") val lineaIdFgv: Long,
    @SerialName("nombre_corto") val nombreCorto: String,
    @SerialName("nombre_largo") val nombreLargo: String? = null,
    val tipo: String? = null,
    val color: String? = null,
    @SerialName("forma_id") val formaId: Long? = null,
    val stops: String? = null,
    val sede: String? = null,
)
