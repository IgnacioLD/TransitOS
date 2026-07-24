package com.glossostudio.transitos.provider.metrovalencia.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `POST /planificador-online2`. Unlike `horarios-online2` which
 * returns full-day schedules, this endpoint returns specific planned journeys
 * with exact departure/arrival times per leg, line colors, and transfer info.
 *
 * Parameters: `estacion_origen_id`, `estacion_destino_id`, `fecha` (dd/MM/yyyy),
 * and either `hora_salida` or `hora_llegada` (HH:mm) — never both.
 */
@Serializable
data class FgvPlanificadorResponseDto(
    val status: Int? = null,
    val error: Int? = null,
    val resultado: List<FgvPlanificadorDto> = emptyList(),
)

@Serializable
data class FgvPlanificadorDto(
    val titulo: FgvLocalizedTextDto? = null,
    val estacion_origen: FgvStationDto? = null,
    val estacion_destino: FgvStationDto? = null,
    val duracion_minutos: Int = 0,
    val huella_de_carbono: Double? = null,
    val fecha_peticion: String? = null,
    val tarifas: String? = null,
    val mensaje: String? = null,
    val pasos: List<FgvPasoDto> = emptyList(),
)

@Serializable
data class FgvPasoDto(
    val orden: Long = 0,
    val hora_salida: String = "",
    val hora_llegada: String = "",
    val estacion_origen: FgvStationDto? = null,
    val estacion_destino: FgvStationDto? = null,
    val tren_origen: String = "",
    val linea_origen: FgvPlanificadorLineaDto? = null,
    val transbordo: FgvTransbordoPlanificadorDto? = null,
    val puntos: List<FgvPuntoDto> = emptyList(),
)

@Serializable
data class FgvPlanificadorLineaDto(
    val color: String? = null,
    val nombre_corto: String? = null,
    val nombre_largo: String? = null,
    @kotlinx.serialization.SerialName("linea_id_FGV") val lineaIdFgv: Long? = null,
)

@Serializable
data class FgvTransbordoPlanificadorDto(
    val linea_origen: FgvPlanificadorLineaDto? = null,
    val linea_destino: FgvPlanificadorLineaDto? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @kotlinx.serialization.SerialName("min_espera") val minEspera: Int = 0,
)

@Serializable
data class FgvPuntoDto(
    val tipo: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val hora: String? = null,
    val nombre_estacion: String? = null,
    val lineas_nombre: String? = null,
    val color: String? = null,
)
