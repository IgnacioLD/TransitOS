package com.glossostudio.transitos.provider.metrovalencia.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /sincronizacion`, FGV's offline-bootstrap bundle.
 *
 * The response is large (~6 MB, no compression) and carries dozens of
 * collections; we only declare the three needed to rebuild the network
 * geometry. `ignoreUnknownKeys` skips the rest while decoding.
 *
 *  - [FgvSyncDataDto.lineas] carries each line's `forma_id`.
 *  - [FgvSyncDataDto.puntos] carries the shape points keyed by that
 *    `forma_id_FGV`, ordered by `orden`: the authoritative track alignment.
 *  - [FgvSyncDataDto.estaciones] is the coordinate catalogue, used only to
 *    reconstruct a line when its shape is missing.
 */
@Serializable
data class FgvSincronizacionResponseDto(
    val timestamp: String? = null,
    val data: FgvSyncDataDto = FgvSyncDataDto(),
)

@Serializable
data class FgvSyncDataDto(
    val lineas: List<FgvLineDto> = emptyList(),
    val estaciones: List<FgvStationDto> = emptyList(),
    val puntos: List<FgvShapePointDto> = emptyList(),
)

@Serializable
data class FgvShapePointDto(
    @SerialName("forma_id_FGV") val formaIdFgv: Long? = null,
    val orden: Int = 0,
    val latitud: Double? = null,
    val longitud: Double? = null,
)
