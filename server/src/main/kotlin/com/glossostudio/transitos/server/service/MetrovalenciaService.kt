package com.glossostudio.transitos.server.service

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.LineGeometry
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.network.HttpStatusException
import com.glossostudio.transitos.server.cache.TtlCache
import com.glossostudio.transitos.server.provider.metrovalencia.MetrovalenciaConfig
import com.glossostudio.transitos.server.provider.metrovalencia.api.MetrovalenciaApi
import com.glossostudio.transitos.server.provider.metrovalencia.dto.FgvLineDto
import com.glossostudio.transitos.server.provider.metrovalencia.dto.FgvPasoDto
import com.glossostudio.transitos.server.provider.metrovalencia.dto.FgvPlanificadorDto
import com.glossostudio.transitos.server.provider.metrovalencia.dto.FgvStationDto
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.LineDisplayInfo
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.addMinutesToTime
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.formatAsFgvFecha
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.parseArgbHexOrNull
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.timeDiffMinutes
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.toAlert
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.toArrivals
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.toJourney
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.toLine
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.toLineGeometries
import com.glossostudio.transitos.server.provider.metrovalencia.mapper.toStop
import kotlinx.datetime.LocalDate

/** Cache lifetimes, tuned to how fast each datum actually changes. */
data class CacheTtls(
    val catalogMs: Long = DEFAULT_CATALOG_TTL_MS,
    val arrivalsMs: Long = DEFAULT_ARRIVALS_TTL_MS,
    val alertsMs: Long = DEFAULT_ALERTS_TTL_MS,
)

/** A journey-planning request. */
data class PlanQuery(
    val originStopId: String,
    val destinationStopId: String,
    val date: LocalDate,
    val hora: String?,
    val isDeparture: Boolean,
    val minTransferMinutes: Int,
)

/** Injectable time source, so tests can pin "now". */
data class ServerClock(
    val nowMs: () -> Long = System::currentTimeMillis,
    val nowTime: () -> String = ::currentHhMm,
)

/**
 * Metrovalencia (FGV) data access for the server, with politeness built in.
 *
 * Catalogs are cached for hours, alerts for minutes and live arrivals for
 * seconds, so a single server instance issues at most one upstream request per
 * interval regardless of how many app clients are connected. This replaces the
 * app's previous per-device polling loop; the failure/backoff contract now lives
 * between the app and this server, and the server surfaces upstream errors as
 * HTTP status codes.
 */
class MetrovalenciaService(
    private val api: MetrovalenciaApi,
    private val config: MetrovalenciaConfig,
    private val cache: TtlCache = TtlCache(),
    private val ttls: CacheTtls = CacheTtls(),
    private val clock: ServerClock = ServerClock(),
) {
    suspend fun stops(): List<Stop> = cache.get(KEY_STOPS, ttls.catalogMs) {
        rawStations().map { it.toStop() }
    }

    suspend fun lines(): List<Line> = cache.get(KEY_LINES, ttls.catalogMs) {
        rawLines().map { it.toLine() }
    }

    suspend fun geometries(): List<LineGeometry> = cache.get(KEY_GEOMETRIES, ttls.catalogMs) {
        api.getSincronizacion().toLineGeometries()
    }

    suspend fun arrivals(stopId: String): List<Arrival> {
        val fgvId = stopId.removePrefix(STOP_ID_PREFIX).toLongOrNull() ?: return emptyList()
        return cache.get("$KEY_ARRIVALS$stopId", ttls.arrivalsMs) {
            val lineByFgvId = lines().byFgvId()
            val now = clock.nowMs()
            api.getArrivals(fgvId).previsiones.flatMap { it.toArrivals(stopId, now, lineByFgvId) }
        }
    }

    suspend fun alerts(): List<Alert> = cache.get(KEY_ALERTS, ttls.alertsMs) { fetchAlerts() }

    /**
     * Plans a journey from one Metrovalencia stop to another. Returns an empty
     * list when the operator finds no route for the given O/D/date; throws on a
     * transport failure, which the routes surface as an error status.
     */
    suspend fun plan(query: PlanQuery): List<Journey> {
        val originInternal = resolveInternalStationId(query.originStopId) ?: return emptyList()
        val destinationInternal = resolveInternalStationId(query.destinationStopId) ?: return emptyList()
        val effectiveHora = query.hora ?: clock.nowTime()

        val response = if (query.isDeparture) {
            api.planificadorOnline(
                originInternalId = originInternal,
                destinationInternalId = destinationInternal,
                fecha = query.date.formatAsFgvFecha(),
                horaSalida = effectiveHora,
                horaLlegada = null,
            )
        } else {
            api.planificadorOnline(
                originInternalId = originInternal,
                destinationInternalId = destinationInternal,
                fecha = query.date.formatAsFgvFecha(),
                horaSalida = null,
                horaLlegada = effectiveHora,
            )
        }

        // FGV can report planner failures in the body while answering 200.
        val bodyStatus = response.status
        if (bodyStatus != null && bodyStatus != HTTP_OK) {
            throw HttpStatusException(bodyStatus, "FGV planner answered status $bodyStatus")
        }
        return response.resultado.mapNotNull { dto ->
            enforceTransferBuffer(dto, query.date, query.minTransferMinutes).toJourney(query.date)
        }
    }

    private suspend fun rawStations(): List<FgvStationDto> = cache.get(KEY_STATIONS_RAW, ttls.catalogMs) { api.getStations() }

    private suspend fun rawLines(): List<FgvLineDto> = cache.get(KEY_LINES_RAW, ttls.catalogMs) { api.getLines() }

    private suspend fun resolveInternalStationId(canonicalStopId: String): Long? {
        val fgvId = canonicalStopId.removePrefix(STOP_ID_PREFIX).toLongOrNull() ?: return null
        return rawStations().firstOrNull { it.estacionIdFgv == fgvId }?.id
    }

    private suspend fun fetchAlerts(): List<Alert> {
        val lineByInternalId: Map<Long, LineDisplayInfo> = rawLines().mapNotNull { dto ->
            val internalId = dto.id ?: return@mapNotNull null
            internalId to LineDisplayInfo(
                canonicalId = LINE_ID_PREFIX + dto.lineaIdFgv,
                shortName = dto.nombreCorto,
                color = dto.color?.let { parseArgbHexOrNull(it) },
            )
        }.toMap()

        val response = api.getIncidencias()
        return response.incidencias.map {
            it.toAlert(lineByInternalId, response.incidenciaTranslations, config.language)
        }
    }

    /**
     * Ported from the app's provider: re-plans tight transfers so every wait
     * respects the user's minimum. A failed re-plan keeps the original legs.
     */
    private suspend fun enforceTransferBuffer(
        dto: FgvPlanificadorDto,
        date: LocalDate,
        minTransferMinutes: Int,
    ): FgvPlanificadorDto {
        if (dto.pasos.size <= 1 || minTransferMinutes <= 0) return dto
        val pasos = dto.pasos.toMutableList()
        val destInternalId = dto.estacion_destino?.id ?: return dto
        var i = 1
        while (i < pasos.size) {
            val prevArrival = pasos[i - 1].hora_llegada
            val thisDeparture = pasos[i].hora_salida
            val realWait = timeDiffMinutes(prevArrival, thisDeparture)
            val transferInternalId = pasos[i].estacion_origen?.id
            val replanned = if (realWait != null && realWait < minTransferMinutes && transferInternalId != null) {
                replanFromTransfer(transferInternalId, destInternalId, prevArrival, date, minTransferMinutes)
            } else {
                null
            }
            if (replanned != null) {
                pasos.subList(i, pasos.size).clear()
                pasos.addAll(replanned)
                continue
            }
            i++
        }
        return dto.copy(pasos = pasos)
    }

    /** Re-plans the tail of a journey from a transfer station. Null on failure. */
    private suspend fun replanFromTransfer(
        transferInternalId: Long,
        destinationInternalId: Long,
        previousArrival: String,
        date: LocalDate,
        minTransferMinutes: Int,
    ): List<FgvPasoDto>? {
        val newDeparture = addMinutesToTime(previousArrival, minTransferMinutes)
        val response = runCatching {
            api.planificadorOnline(
                originInternalId = transferInternalId,
                destinationInternalId = destinationInternalId,
                fecha = date.formatAsFgvFecha(),
                horaSalida = newDeparture,
                horaLlegada = null,
            )
        }.getOrNull() ?: return null
        if (response.status != HTTP_OK) return null
        return response.resultado.firstOrNull()?.pasos?.takeIf { it.isNotEmpty() }
    }

    private fun List<Line>.byFgvId(): Map<Long, Line> = mapNotNull { line ->
        line.id.removePrefix(LINE_ID_PREFIX).toLongOrNull()?.let { it to line }
    }.toMap()

    companion object {
        const val OPERATOR_ID: String = MetrovalenciaConfig.OPERATOR_ID
        const val OPERATOR_NAME: String = MetrovalenciaConfig.OPERATOR_NAME
        const val STOP_ID_PREFIX: String = "mv:"

        private const val LINE_ID_PREFIX = "mv:"
        private const val HTTP_OK = 200
        private const val KEY_STOPS = "metrovalencia.stops"
        private const val KEY_LINES = "metrovalencia.lines"
        private const val KEY_GEOMETRIES = "metrovalencia.geometries"
        private const val KEY_ALERTS = "metrovalencia.alerts"
        private const val KEY_ARRIVALS = "metrovalencia.arrivals:"
        private const val KEY_STATIONS_RAW = "metrovalencia.stations.raw"
        private const val KEY_LINES_RAW = "metrovalencia.lines.raw"
    }
}

private const val DEFAULT_CATALOG_TTL_MS = 6 * 60 * 60_000L
private const val DEFAULT_ARRIVALS_TTL_MS = 30_000L
private const val DEFAULT_ALERTS_TTL_MS = 5 * 60_000L

private fun currentHhMm(): String {
    val now = java.time.LocalTime.now()
    return "%02d:%02d".format(now.hour, now.minute)
}
