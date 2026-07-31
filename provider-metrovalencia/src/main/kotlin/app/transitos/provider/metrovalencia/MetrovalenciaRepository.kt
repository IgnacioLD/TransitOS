package com.glossostudio.transitos.provider.metrovalencia

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.provider.metrovalencia.api.MetrovalenciaApi
import com.glossostudio.transitos.provider.metrovalencia.mapper.LineDisplayInfo
import com.glossostudio.transitos.provider.metrovalencia.mapper.formatAsFgvFecha
import com.glossostudio.transitos.provider.metrovalencia.mapper.parseArgbHexOrNull
import com.glossostudio.transitos.provider.metrovalencia.mapper.toAlert
import com.glossostudio.transitos.provider.metrovalencia.mapper.toArrivals
import com.glossostudio.transitos.provider.metrovalencia.mapper.toJourney
import com.glossostudio.transitos.provider.metrovalencia.mapper.toLine
import com.glossostudio.transitos.provider.metrovalencia.mapper.toStop
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlin.math.min

/**
 * FGV-backed implementation of [TransitRepository]. Replaces the earlier stub.
 *
 * Network footprint was a deliberate design concern, FGV's endpoints are
 * undocumented and we want to be a polite client. The strategy:
 *
 *  - **Catalogs** (`stops`, `lines`, `alerts`) are shared [StateFlow]s. `lines`
 *    is eager (always warm) because both arrivals and alerts need to look up
 *    line display info; warming it once means N favorite arrivals polls don't
 *    each re-fetch lineas. `stops` and `alerts` are WhileSubscribed so they
 *    pause when nobody's looking.
 *  - **Live arrivals** poll per stop at [MetrovalenciaConfig.arrivalsPollMs].
 *    Each tick does ONE call (`horarios-prevision-3`) and reads the cached
 *    lines table, no extra fetches.
 *  - **Identical emissions are dropped** via [distinctUntilChanged] so a
 *    server that returns the same data doesn't trigger UI re-subscriptions or
 *    downstream polling cascades.
 *
 * Network errors are swallowed into empty emissions at this layer; the richer
 * [com.glossostudio.transitos.core.result.AppError] mapping is a future refinement.
 *
 * If FGV ever objects to the undocumented endpoint, this file is the only
 * thing that needs replacing, see `RESEARCH.md` (Option 3).
 */
class MetrovalenciaRepository(
    private val api: MetrovalenciaApi,
    private val config: MetrovalenciaConfig,
    private val ioScope: CoroutineScope,
    private val settings: ProviderSettingsRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : TransitRepository {

    private val arrivalsRefreshSignal = Channel<Unit>(Channel.CONFLATED)
    private val alertsRefreshSignal = Channel<Unit>(Channel.CONFLATED)

    override suspend fun refresh() {
        arrivalsRefreshSignal.trySend(Unit)
        alertsRefreshSignal.trySend(Unit)
    }

    private val backend: StateFlow<MetrovalenciaBackend> = settings
        .observeProviderBackend(MetrovalenciaConfig.OPERATOR_ID)
        .map { it.toMetrovalenciaBackendOr(MetrovalenciaBackend.FGV) }
        .stateIn(ioScope, SharingStarted.Eagerly, MetrovalenciaBackend.FGV)

    // Eager: the lines catalogue is needed by both arrivals and alerts to look
    // up display info. Warming it once here means we never re-fetch it per
    // poll, one GET every `catalogRefreshMs`, regardless of how many favorites
    // the user has.
    private val linesState: StateFlow<List<Line>> = pollingState(
        fetch = { api.getLines().map { it.toLine() } },
        intervalMs = config.catalogRefreshMs,
        started = SharingStarted.Eagerly,
    )

    private val stopsState: StateFlow<List<Stop>> = pollingState(
        fetch = { api.getStations().map { it.toStop() } },
        intervalMs = config.catalogRefreshMs,
    )

    private val alertsState: StateFlow<List<Alert>> = pollingState(
        fetch = { fetchAlertsWithLineNames() },
        intervalMs = config.alertsPollMs,
        started = SharingStarted.Eagerly,
        refreshSignal = alertsRefreshSignal,
    )

    override fun observeStops(): Flow<List<Stop>> = stopsState
    override fun observeLines(): Flow<List<Line>> = linesState

    override fun observeAlerts(): Flow<List<Alert>> =
        backend.flatMapLatest { currentBackend ->
            if (currentBackend == MetrovalenciaBackend.NAP) flowOf(emptyList())
            else alertsState
        }

    override fun observeArrivals(stopId: String): Flow<List<Arrival>> {
        val fgvId = stopId.removePrefix(STOP_ID_PREFIX).toLongOrNull()
            ?: return flowOf(emptyList())
        return backend.flatMapLatest { currentBackend ->
            if (currentBackend == MetrovalenciaBackend.NAP) {
                flowOf(emptyList())
            } else {
                flow {
                    while (true) {
                        val arrivals = runCatching {
                            val lineByFgvId = linesState.value.byFgvId()
                            val response = api.getArrivals(fgvId)
                            val now = nowMs()
                            response.previsiones.flatMap { it.toArrivals(stopId, now, lineByFgvId) }
                        }.getOrDefault(emptyList())
                        emit(arrivals)
                        val deadline = nowMs() + config.arrivalsPollMs
                        while (true) {
                            val remaining = deadline - nowMs()
                            if (remaining <= 0L) break
                            // A refresh signal breaks the wait early so the next
                            // fetch runs immediately instead of waiting for the
                            // full poll interval.
                            if (withTimeoutOrNull(remaining) { arrivalsRefreshSignal.receive() } != null) break
                        }
                    }
                }
                    .distinctUntilChanged()
                    .retryWhen { _, attempt ->
                        delay(min(BACKOFF_BASE_MS shl attempt.toInt(), BACKOFF_CAP_MS))
                        true
                    }
            }
        }
    }

    /**
     * Resolves a canonical stop id (`"mv:12"`) to FGV's internal station id
     * (`226`) by fetching the stations catalogue. One GET per plan call -
     * acceptable because planJourney is user-initiated, not polled.
     *
     * If this ever becomes hot, cache the raw station DTOs in a StateFlow
     * alongside [stopsState] and read `.value` here.
     */
    private suspend fun resolveInternalStationId(canonicalStopId: String): Long? {
        val fgvId = canonicalStopId.removePrefix(STOP_ID_PREFIX).toLongOrNull() ?: return null
        return runCatching { api.getStations() }
            .getOrDefault(emptyList())
            .firstOrNull { it.estacionIdFgv == fgvId }
            ?.id
    }

    override suspend fun planJourney(
        originStopId: String,
        destinationStopId: String,
        date: LocalDate,
        hora: String?,
        isDeparture: Boolean,
        minTransferMinutes: Int,
    ): List<Journey> {
        if (backend.value == MetrovalenciaBackend.NAP) return emptyList()
        return runCatching {
            val originInternal = resolveInternalStationId(originStopId) ?: return emptyList()
            val destinationInternal = resolveInternalStationId(destinationStopId) ?: return emptyList()
            val effectiveHora = hora ?: java.time.LocalTime.now().let {
                "%02d:%02d".format(it.hour, it.minute)
            }
            val response = if (isDeparture) {
                api.planificadorOnline(
                    originInternalId = originInternal,
                    destinationInternalId = destinationInternal,
                    fecha = date.formatAsFgvFecha(),
                    horaSalida = effectiveHora,
                    horaLlegada = null,
                )
            } else {
                api.planificadorOnline(
                    originInternalId = originInternal,
                    destinationInternalId = destinationInternal,
                    fecha = date.formatAsFgvFecha(),
                    horaSalida = null,
                    horaLlegada = effectiveHora,
                )
            }
            if (response.status != 200) emptyList()
            else response.resultado.mapNotNull { it.toJourney(date, minTransferMinutes) }
        }.getOrDefault(emptyList())
    }

    /**
     * Builds alerts using a fresh fetch of lineas + incidencias. The lookup is
     * keyed on the lineas **internal** `id` because that's what
     * `incidencias.linea_id` returns (NOT `linea_id_FGV`).
     */
    private suspend fun fetchAlertsWithLineNames(): List<Alert> {
        val lineByInternalId: Map<Long, LineDisplayInfo> = runCatching {
            api.getLines().mapNotNull { dto ->
                val internalId = dto.id ?: return@mapNotNull null
                internalId to LineDisplayInfo(
                    canonicalId = LINE_ID_PREFIX + dto.lineaIdFgv,
                    shortName = dto.nombreCorto,
                    color = dto.color?.let { parseArgbHexOrNull(it) },
                )
            }.toMap()
        }.getOrDefault(emptyMap())

        return runCatching {
            val response = api.getIncidencias()
            response.incidencias.map {
                it.toAlert(lineByInternalId, response.incidenciaTranslations, config.language)
            }
        }.getOrDefault(emptyList())
    }

    private fun List<Line>.byFgvId(): Map<Long, Line> = mapNotNull { line ->
        line.id.removePrefix(LINE_ID_PREFIX).toLongOrNull()?.let { it to line }
    }.toMap()

    private fun <T> pollingState(
        fetch: suspend () -> List<T>,
        intervalMs: Long,
        started: SharingStarted = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        refreshSignal: Channel<Unit>? = null,
    ): StateFlow<List<T>> = flow {
        while (true) {
            emit(runCatching { fetch() }.getOrDefault(emptyList()))
            if (refreshSignal != null) {
                val deadline = nowMs() + intervalMs
                while (true) {
                    val remaining = deadline - nowMs()
                    if (remaining <= 0L) break
                    if (withTimeoutOrNull(remaining) { refreshSignal.receive() } != null) break
                }
            } else {
                delay(intervalMs)
            }
        }
    }
        .distinctUntilChanged()
        .stateIn(scope = ioScope, started = started, initialValue = emptyList())

    private companion object {
        const val STOP_ID_PREFIX = "mv:"
        const val LINE_ID_PREFIX = "mv:"
        const val STOP_TIMEOUT_MS = 30_000L
        const val BACKOFF_BASE_MS = 1_000L
        const val BACKOFF_CAP_MS = 30_000L
    }
}
