package com.glossostudio.transitos.provider.metrovalencia.mapper

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.JourneyLeg
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.model.TransportMode
import com.glossostudio.transitos.provider.metrovalencia.MetrovalenciaConfig
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvIncidenciaDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvJourneyAlternativeDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvLineDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPasoDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPlanificadorDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPrevisionDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvStationDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvTrainDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvTransbordoDto
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
 * returns, *not* `linea_id_FGV`). Without that distinction the lookup misses
 * every time and titles fall back to raw database ids.
 */
internal fun FgvIncidenciaDto.toAlert(
    lineByInternalId: Map<Long, LineDisplayInfo>,
    translations: List<com.glossostudio.transitos.provider.metrovalencia.dto.FgvIncidenciaTranslationDto>,
    locale: String,
): Alert {
    val info = lineByInternalId[lineaId]
    val translation = translations.firstOrNull {
        it.incidenciaId == id && it.locale?.equals(locale, ignoreCase = true) == true
    } ?: translations.firstOrNull { it.incidenciaId == id }
    return Alert(
        id = "$ALERT_ID_PREFIX$id",
        operatorId = MetrovalenciaConfig.OPERATOR_ID,
        lineIds = setOf(info?.canonicalId ?: (LINE_ID_PREFIX + lineaId)),
        stopIds = emptySet(),
        severity = Alert.Severity.WARNING,
        title = translation?.titulo?.takeIf { it.isNotBlank() }
            ?: "Línea ${info?.shortName ?: lineaId} con incidencias",
        body = translation?.descripcion,
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
 * Maps a planificador-online2 response to a domain [Journey]. Unlike the
 * horarios mapper, this produces legs with specific departure/arrival times,
 * line colors, and transfer info.
 *
 * [minTransferMinutes] is the user-configurable buffer: if the API says a
 * transfer takes fewer minutes than this, we pad the wait and shift subsequent
 * leg times so the displayed arrival is realistic.
 */
internal fun FgvPlanificadorDto.toJourney(
    date: LocalDate,
    minTransferMinutes: Int = 5,
): Journey? {
    if (pasos.isEmpty()) return null
    val rawLegs = pasos.mapIndexed { index, paso ->
        paso.toLeg(
            waitFromPrevious = if (index > 0) pasos[index - 1].transbordo?.minEspera else null,
        )
    }
    val legs = applyTransferBuffer(rawLegs, minTransferMinutes)
    if (legs.isEmpty()) return null
    val totalBuffer = totalBufferAdded(rawLegs, legs)
    return Journey(
        date = date,
        durationMinutes = duracion_minutos + totalBuffer,
        distanceMeters = 0L,
        fareZone = tarifas,
        carbonKg = huella_de_carbono,
        legs = legs,
    )
}

internal fun FgvPasoDto.toLeg(waitFromPrevious: Int? = null): JourneyLeg {
    val linea = linea_origen
    return JourneyLeg(
        originName = estacion_origen?.nombre.orEmpty(),
        destinationName = estacion_destino?.nombre.orEmpty(),
        headsigns = listOfNotNull(tren_origen.takeIf { it.isNotBlank() }),
        lineNames = listOfNotNull(linea?.nombre_corto?.takeIf { it.isNotBlank() }),
        lineColors = listOfNotNull(linea?.color?.let(::parseArgbHexOrNull)),
        departureTime = hora_salida.takeIf { it.isNotBlank() },
        arrivalTime = hora_llegada.takeIf { it.isNotBlank() },
        trainId = tren_origen.takeIf { it.isNotBlank() },
        waitMinutes = waitFromPrevious,
    )
}

private fun applyTransferBuffer(
    legs: List<JourneyLeg>,
    minTransfer: Int,
): List<JourneyLeg> {
    if (legs.size <= 1 || minTransfer <= 0) return legs
    var cumulativeShift = 0
    return legs.mapIndexed { index, leg ->
        if (index == 0) return@mapIndexed leg
        val wait = leg.waitMinutes ?: 0
        val needed = if (wait < minTransfer) minTransfer - wait else 0
        cumulativeShift += needed
        if (cumulativeShift > 0) {
            leg.copy(
                waitMinutes = wait + needed,
                departureTime = leg.departureTime?.let { addMinutesToTime(it, cumulativeShift) },
                arrivalTime = leg.arrivalTime?.let { addMinutesToTime(it, cumulativeShift) },
            )
        } else leg
    }
}

private fun totalBufferAdded(original: List<JourneyLeg>, buffered: List<JourneyLeg>): Int {
    var total = 0
    for (i in original.indices) {
        val o = original[i].waitMinutes ?: 0
        val b = buffered[i].waitMinutes ?: 0
        total += (b - o).coerceAtLeast(0)
    }
    return total
}

private fun addMinutesToTime(time: String, minutes: Int): String {
    val parts = time.split(":")
    if (parts.size != 2) return time
    val h = parts[0].toIntOrNull() ?: return time
    val m = parts[1].toIntOrNull() ?: return time
    val total = h * 60 + m + minutes
    return "%02d:%02d".format((total / 60) % 24, total % 60)
}

/**
 * Maps an FGV journey alternative to a domain [Journey]. Returns null when the
 * response has no usable legs (FGV sometimes returns 200 with an empty
 * transbordos array for unreachable O/D pairs).
 *
 * The departures list is flattened from the `horas` map (`"05" → ["05:25",
 * "05:55"]`) and sorted lexically, safe because every entry is zero-padded
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
    lineNames = lineas,
)

/**
 * Formats a [LocalDate] as `dd/MM/yyyy`, the only date format FGV's planner
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
