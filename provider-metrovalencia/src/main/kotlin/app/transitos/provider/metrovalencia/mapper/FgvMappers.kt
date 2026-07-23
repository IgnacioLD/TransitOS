package app.transitos.provider.metrovalencia.mapper

import app.transitos.core.model.Alert
import app.transitos.core.model.Arrival
import app.transitos.core.model.Journey
import app.transitos.core.model.JourneyLeg
import app.transitos.core.model.Line
import app.transitos.core.model.Stop
import app.transitos.core.model.TransportMode
import app.transitos.provider.metrovalencia.MetrovalenciaConfig
import app.transitos.provider.metrovalencia.dto.FgvIncidenciaDto
import app.transitos.provider.metrovalencia.dto.FgvJourneyAlternativeDto
import app.transitos.provider.metrovalencia.dto.FgvLineDto
import app.transitos.provider.metrovalencia.dto.FgvPrevisionDto
import app.transitos.provider.metrovalencia.dto.FgvStationDto
import app.transitos.provider.metrovalencia.dto.FgvTrainDto
import app.transitos.provider.metrovalencia.dto.FgvTransbordoDto
import kotlinx.datetime.LocalDate

/**
 * Pure mappings from FGV wire DTOs to the universal domain models. All functions
 * are stateless and side-effect free so they can be unit-tested without a
 * network or a Koin graph.
 */

internal fun FgvStationDto.toStop(): Stop = Stop(
    id = STOP_ID_PREFIX + estacionIdFgv,
    name = nombre,
    operatorId = MetrovalenciaConfig.OPERATOR_ID,
    mode = TransportMode.METRO,
    lat = latitud,
    lon = longitud,
    parentStationId = null,
)

internal fun FgvLineDto.toLine(): Line = Line(
    id = LINE_ID_PREFIX + lineaIdFgv,
    name = nombreCorto,
    operatorId = MetrovalenciaConfig.OPERATOR_ID,
    mode = TransportMode.METRO,
    color = color?.let(::parseArgbHexOrNull),
    textColor = null,
)

/**
 * Expands one prevision (a line + its upcoming trains at this stop) into one
 * [Arrival] per train. [stopId] is the canonical domain stop id (already
 * prefixed) of the station we asked about; [lineByFgvId] lets each arrival
 * carry its line's short name and colour for badge rendering.
 */
internal fun FgvPrevisionDto.toArrivals(
    stopId: String,
    nowEpochMs: Long,
    lineByFgvId: Map<Long, Line>,
): List<Arrival> {
    val fgvLineId = lineId ?: line ?: lineaIdInterna ?: 0L
    val line = lineByFgvId[fgvLineId]
    return trains.map { train ->
        train.toArrival(
            stopId = stopId,
            lineId = LINE_ID_PREFIX + fgvLineId,
            lineShortName = line?.name,
            lineColor = line?.color,
            nowEpochMs = nowEpochMs,
        )
    }
}

private fun FgvTrainDto.toArrival(
    stopId: String,
    lineId: String,
    lineShortName: String?,
    lineColor: Long?,
    nowEpochMs: Long,
): Arrival = Arrival(
    stopId = stopId,
    lineId = lineId,
    destination = destino,
    estimatedEpochMs = nowEpochMs + seconds * 1_000L,
    minutesAway = (seconds / 60L).toInt(),
    vehicleId = vehicle?.toString(),
    isRealTime = true,
    lineShortName = lineShortName,
    lineColor = lineColor,
)

/**
 * Display info for a single line, used to enrich alerts with badge data.
 * Decoupled from the full [Line] model so the alert flow can build this from
 * raw DTOs without losing the internal-id ↔ FGV-id distinction.
 */
public data class LineDisplayInfo(
    public val canonicalId: String,
    public val shortName: String,
    public val color: Long?,
)

/**
 * Maps an FGV incidencia to a domain [Alert]. [lineByInternalId] is keyed by
 * the FGV **internal** line id (the value `incidencias.linea_id` actually
 * returns — *not* `linea_id_FGV`). Without that distinction the lookup misses
 * every time and titles fall back to raw database ids.
 */
internal fun FgvIncidenciaDto.toAlert(lineByInternalId: Map<Long, LineDisplayInfo>): Alert {
    val info = lineByInternalId[lineaId]
    return Alert(
        id = "$ALERT_ID_PREFIX$id",
        operatorId = MetrovalenciaConfig.OPERATOR_ID,
        lineIds = setOf(info?.canonicalId ?: (LINE_ID_PREFIX + lineaId)),
        stopIds = emptySet(),
        severity = Alert.Severity.WARNING,
        title = "Línea ${info?.shortName ?: lineaId} con incidencias",
        body = null,
        startEpochMs = null,
        endEpochMs = null,
        lineShortName = info?.shortName,
        lineColor = info?.color,
    )
}

/**
 * Parses an ARGB [Long] from an FGV colour string. Accepts `#RRGGBB` and
 * `#AARRGGBB`; anything else returns null so the UI falls back to the theme.
 */
internal fun parseArgbHexOrNull(hex: String): Long? = runCatching {
    val clean = hex.removePrefix("#")
    when (clean.length) {
        6 -> ("FF$clean").toLong(16)
        8 -> clean.toLong(16)
        else -> null
    }
}.getOrNull() ?: null

/**
 * Maps an FGV journey alternative to a domain [Journey]. Returns null when the
 * response has no usable legs (FGV sometimes returns 200 with an empty
 * transbordos array for unreachable O/D pairs).
 *
 * The departures list is flattened from the `horas` map (`"05" → ["05:25",
 * "05:55"]`) and sorted lexically — safe because every entry is zero-padded
 * `"HH:mm"`, so lexical order equals chronological order.
 */
internal fun FgvJourneyAlternativeDto.toJourney(date: LocalDate): Journey? {
    if (transbordos.isEmpty()) return null
    val legs = transbordos.mapNotNull { it.toLeg() }
    if (legs.isEmpty()) return null
    return Journey(
        date = date,
        durationMinutes = duracion_minutos,
        distanceMeters = distancia,
        fareZone = tarifas,
        carbonKg = huella_de_carbono,
        legs = legs,
    )
}

internal fun FgvTransbordoDto.toLeg(): JourneyLeg = JourneyLeg(
    originName = estacion_origen_transbordo?.nombre.orEmpty(),
    destinationName = estacion_destino_transbordo?.nombre.orEmpty(),
    headsigns = destinos,
    departures = horas
        .flatMap { (hour, times) -> times.map { time -> time } }
        .sorted(),
)

/**
 * Formats a [LocalDate] as `dd/MM/yyyy` — the only date format FGV's planner
 * accepts. ISO `yyyy-MM-dd` returns HTTP 400.
 */
internal fun LocalDate.formatAsFgvFecha(): String {
    val day = dayOfMonth.toString().padStart(2, '0')
    val month = monthNumber.toString().padStart(2, '0')
    return "$day/$month/$year"
}

private const val STOP_ID_PREFIX = "mv:"
private const val LINE_ID_PREFIX = "mv:"
private const val ALERT_ID_PREFIX = "mv:"
