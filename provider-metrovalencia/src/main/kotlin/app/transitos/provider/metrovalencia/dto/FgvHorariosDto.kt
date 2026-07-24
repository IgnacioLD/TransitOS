package app.transitos.provider.metrovalencia.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `POST /horarios-online2`. FGV returns one [FgvJourneyAlternativeDto]
 * per routing alternative — for Metrovalencia's small network this is almost
 * always a single direct option, but multi-transfer journeys can yield several.
 *
 * The outer envelope is the same `status/error/resultado` triple FGV uses
 * across its API; `status != 200` means no route could be planned (the
 * resultado field then carries a Spanish error string instead of the array).
 */
@Serializable
data class FgvHorariosResponseDto(
    val status: Int? = null,
    val error: Int? = null,
    val resultado: List<FgvJourneyAlternativeDto> = emptyList(),
)

@Serializable
data class FgvJourneyAlternativeDto(
    val titulo: FgvLocalizedTextDto? = null,
    val estacion_origen: FgvStationDto? = null,
    val estacion_destino: FgvStationDto? = null,
    val tarifas: String? = null,
    val huella_de_carbono: Double? = null,
    val duracion_minutos: Int = 0,
    val distancia: Long = 0,
    val transbordos: List<FgvTransbordoDto> = emptyList(),
)

@Serializable
data class FgvLocalizedTextDto(
    val ES: String? = null,
    val EN: String? = null,
    val VL: String? = null,
)

/**
 * One segment of a planned journey. Despite the name "transbordo" (transfer),
 * FGV returns one of these per leg even for direct trips — so a direct
 * Benimaclet → Pobla de Farnals journey has exactly one transbordo whose
 * origin and destination match the journey's.
 *
 * [horas] is a sparse map from `"HH"` to a list of `"HH:mm"` departure times
 * for that hour. Empty hours are omitted, so iterating sorted keys gives the
 * full schedule.
 */
@Serializable
data class FgvTransbordoDto(
    val estacion_origen_transbordo: FgvStationDto? = null,
    val estacion_destino_transbordo: FgvStationDto? = null,
    val lineas: List<String> = emptyList(),
    val destinos: List<String> = emptyList(),
    val horas: Map<String, List<String>> = emptyMap(),
)
