package com.glossostudio.transitos.provider.metrovalencia

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.LineGeometry
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.network.HttpStatusException
import com.glossostudio.transitos.core.network.NetworkMonitor
import com.glossostudio.transitos.core.network.safeCall
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.core.result.AppResult
import com.glossostudio.transitos.core.result.getOrNull
import com.glossostudio.transitos.provider.metrovalencia.api.MetrovalenciaApi
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPlanificadorDto
import com.glossostudio.transitos.provider.metrovalencia.mapper.LineDisplayInfo
import com.glossostudio.transitos.provider.metrovalencia.mapper.addMinutesToTime
import com.glossostudio.transitos.provider.metrovalencia.mapper.formatAsFgvFecha
import com.glossostudio.transitos.provider.metrovalencia.mapper.parseArgbHexOrNull
import com.glossostudio.transitos.provider.metrovalencia.mapper.timeDiffMinutes
import com.glossostudio.transitos.provider.metrovalencia.mapper.toAlert
import com.glossostudio.transitos.provider.metrovalencia.mapper.toArrivals
import com.glossostudio.transitos.provider.metrovalencia.mapper.toJourney
import com.glossostudio.transitos.provider.metrovalencia.mapper.toLine
import com.glossostudio.transitos.provider.metrovalencia.mapper.toLineGeometries
import com.glossostudio.transitos.provider.metrovalencia.mapper.toStop
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

/**
 * FGV-backed implementation of [TransitRepository].
 *
 * Network footprint was a deliberate design concern, FGV's endpoints are
 * undocumented and we want to be a polite client. The strategy:
 *
 *  - **Catalogs** (`stops`, `lines`, `alerts`) are shared [StateFlow]s. `lines`
 *    is eager (always warm) because both arrivals and alerts need to look up
 *    line display info; warming it once means N favorite arrivals polls don't
 *    each re-fetch lineas. `stops` is WhileSubscribed so it pauses when nobody
 *    is looking.
 *  - **Live arrivals** poll per stop at [MetrovalenciaConfig.arrivalsPollMs].
 *    Each tick does ONE call (`horarios-prevision-3`) and reads the cached
 *    lines table, no extra fetches.
 *  - **Identical emissions are dropped** via [distinctUntilChanged].
 *
 * Every stream follows the [TransitRepository] failure contract through
 * [polling]: a failure is emitted only while there is no data yet, the last
 * good value is kept otherwise, and failed fetches retry with exponential
 * backoff instead of waiting for the next regular refresh.
 *
 * If FGV ever objects to the undocumented endpoint, this file is the only
 * thing that needs replacing, see `RESEARCH.md` (Option 3).
 */
class MetrovalenciaRepository(
    private val api: MetrovalenciaApi,
    private val config: MetrovalenciaConfig,
    private val ioScope: CoroutineScope,
    private val settings: ProviderSettingsRepository,
    networkMonitor: NetworkMonitor,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : TransitRepository {

    // A broadcast, so one refresh wakes every poller (each favourite stop runs
    // its own). Pollers that are mid-fetch simply miss it, which is fine.
    private val refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun refresh() {
        refreshEvents.tryEmit(Unit)
    }

    init {
        // Coming back online retries everything at once instead of waiting out
        // whatever backoff step each stream had reached while offline.
        ioScope.launch {
            networkMonitor.isOnline.filter { it }.collect { refreshEvents.emit(Unit) }
        }
    }

    private val backend: StateFlow<MetrovalenciaBackend> = settings
        .observeProviderBackend(MetrovalenciaConfig.OPERATOR_ID)
        .map { it.toMetrovalenciaBackendOr(MetrovalenciaBackend.FGV) }
        .stateIn(ioScope, SharingStarted.Eagerly, MetrovalenciaBackend.FGV)

    // Eager: the lines catalogue is needed by both arrivals and alerts to look
    // up display info. Warming it once here means we never re-fetch it per
    // poll, one GET every `catalogRefreshMs`, regardless of how many favorites
    // the user has.
    private val linesState: StateFlow<AppResult<List<Line>>> = polling(config.catalogRefreshMs) {
        api.getLines().map { it.toLine() }
    }.stateIn(ioScope, SharingStarted.Eagerly, AppResult.Loading)

    private val stopsState: StateFlow<AppResult<List<Stop>>> = polling(config.catalogRefreshMs) {
        api.getStations().map { it.toStop() }
    }.stateIn(ioScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppResult.Loading)

    private val alertsState: StateFlow<AppResult<List<Alert>>> = polling(config.alertsPollMs) {
        fetchAlertsWithLineNames()
    }.stateIn(ioScope, SharingStarted.Eagerly, AppResult.Loading)

    /**
     * The network shape comes from `/sincronizacion`, a ~6 MB bundle that only
     * the map needs. Starting [SharingStarted.Lazily] means the app never pays
     * for it unless a map is opened, and keeping it shared for the process
     * lifetime means re-opening the map does not re-download it. Until it
     * loads the map shows its bundled fallback.
     */
    private val lineGeometriesState: StateFlow<List<LineGeometry>> = polling(config.catalogRefreshMs) {
        api.getSincronizacion().toLineGeometries()
    }
        .map { it.getOrNull().orEmpty() }
        .stateIn(scope = ioScope, started = SharingStarted.Lazily, initialValue = emptyList())

    override fun observeStops(): Flow<AppResult<List<Stop>>> = stopsState
    override fun observeLines(): Flow<AppResult<List<Line>>> = linesState
    override fun observeLineGeometries(): Flow<List<LineGeometry>> = lineGeometriesState

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAlerts(): Flow<AppResult<List<Alert>>> = backend.flatMapLatest { currentBackend ->
        if (currentBackend == MetrovalenciaBackend.NAP) {
            flowOf(AppResult.Success(emptyList()))
        } else {
            alertsState
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeArrivals(stopId: String): Flow<AppResult<List<Arrival>>> {
        val fgvId = stopId.removePrefix(STOP_ID_PREFIX).toLongOrNull()
            ?: return flowOf(AppResult.Success(emptyList()))
        return backend.flatMapLatest { currentBackend ->
            if (currentBackend == MetrovalenciaBackend.NAP) {
                flowOf(AppResult.Success(emptyList()))
            } else {
                polling(config.arrivalsPollMs) {
                    val lineByFgvId = linesState.value.getOrNull().orEmpty().byFgvId()
                    val response = api.getArrivals(fgvId)
                    val now = nowMs()
                    response.previsiones.flatMap { it.toArrivals(stopId, now, lineByFgvId) }
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
        return api.getStations().firstOrNull { it.estacionIdFgv == fgvId }?.id
    }

    override suspend fun planJourney(
        originStopId: String,
        destinationStopId: String,
        date: LocalDate,
        hora: String?,
        isDeparture: Boolean,
        minTransferMinutes: Int,
    ): AppResult<List<Journey>> {
        if (backend.value == MetrovalenciaBackend.NAP) return AppResult.Success(emptyList())
        return safeCall {
            val originInternal = resolveInternalStationId(originStopId)
            val destinationInternal = resolveInternalStationId(destinationStopId)
            if (originInternal == null || destinationInternal == null) return AppResult.Success(emptyList())
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
            // FGV can report planner failures in the body while answering 200.
            val bodyStatus = response.status
            if (bodyStatus != null && bodyStatus != HTTP_OK) {
                throw HttpStatusException(bodyStatus, "FGV planner answered status $bodyStatus")
            }
            response.resultado.mapNotNull { dto ->
                val enforced = enforceTransferBuffer(dto, date, minTransferMinutes)
                enforced.toJourney(date)
            }
        }
    }

    /**
     * Enforces the user's minimum transfer time by re-planning tight transfers.
     *
     * Walks the planned legs: when the real schedule gap at a transfer is
     * shorter than [minTransferMinutes], calls `planificador-online2` again
     * from the transfer station to the final destination with
     * `hora_salida = previous_arrival + buffer`. The returned sub-journey's
     * legs replace everything from the transfer onward. If the sub-journey
     * itself has a tight transfer, the loop re-checks it (cascading).
     *
     * Falls back to the original legs when re-planning fails or the station
     * lacks an internal id.
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
            if (realWait != null && realWait < minTransferMinutes) {
                val transferInternalId = pasos[i].estacion_origen?.id
                if (transferInternalId != null) {
                    val newDeparture = addMinutesToTime(prevArrival, minTransferMinutes)
                    // A failed re-plan keeps the original legs rather than
                    // failing the whole journey.
                    val subResponse = safeCall {
                        api.planificadorOnline(
                            originInternalId = transferInternalId,
                            destinationInternalId = destInternalId,
                            fecha = date.formatAsFgvFecha(),
                            horaSalida = newDeparture,
                            horaLlegada = null,
                        )
                    }.getOrNull()
                    if (subResponse?.status == HTTP_OK && subResponse.resultado.isNotEmpty()) {
                        val subPasos = subResponse.resultado.first().pasos
                        if (subPasos.isNotEmpty()) {
                            pasos.subList(i, pasos.size).clear()
                            pasos.addAll(subPasos)
                            continue
                        }
                    }
                }
            }
            i++
        }
        return dto.copy(pasos = pasos)
    }

    /**
     * Builds alerts using a fresh fetch of lineas + incidencias. The lookup is
     * keyed on the lineas **internal** `id` because that's what
     * `incidencias.linea_id` returns (NOT `linea_id_FGV`). Without the line
     * table the alerts still load, just without line names.
     */
    private suspend fun fetchAlertsWithLineNames(): List<Alert> {
        val lineByInternalId: Map<Long, LineDisplayInfo> = safeCall {
            api.getLines().mapNotNull { dto ->
                val internalId = dto.id ?: return@mapNotNull null
                internalId to LineDisplayInfo(
                    canonicalId = LINE_ID_PREFIX + dto.lineaIdFgv,
                    shortName = dto.nombreCorto,
                    color = dto.color?.let { parseArgbHexOrNull(it) },
                )
            }.toMap()
        }.getOrNull().orEmpty()

        val response = api.getIncidencias()
        return response.incidencias.map {
            it.toAlert(lineByInternalId, response.incidenciaTranslations, config.language)
        }
    }

    private fun List<Line>.byFgvId(): Map<Long, Line> = mapNotNull { line ->
        line.id.removePrefix(LINE_ID_PREFIX).toLongOrNull()?.let { it to line }
    }.toMap()

    /**
     * Runs [fetch] every [intervalMs] and emits results under the repository
     * failure contract: [AppResult.Loading] first, a [AppResult.Failure] only
     * while nothing has loaded yet, and retries with exponential backoff
     * (capped) after a failure. A [refresh] cuts any wait short.
     */
    private fun <T> polling(intervalMs: Long, fetch: suspend () -> T): Flow<AppResult<T>> = flow {
        emit(AppResult.Loading)
        var hasData = false
        var failures = 0
        while (true) {
            when (val result = safeCall { fetch() }) {
                is AppResult.Success -> {
                    hasData = true
                    failures = 0
                    emit(result)
                }

                is AppResult.Failure -> {
                    if (!hasData) emit(result)
                    failures++
                }

                AppResult.Loading -> Unit
            }
            val wait = if (failures == 0) {
                intervalMs
            } else {
                min(BACKOFF_BASE_MS shl min(failures - 1, BACKOFF_MAX_SHIFT), min(BACKOFF_CAP_MS, intervalMs))
            }
            awaitRefreshOrTimeout(wait)
        }
    }.distinctUntilChanged()

    private suspend fun awaitRefreshOrTimeout(waitMs: Long) {
        withTimeoutOrNull(waitMs) { refreshEvents.first() }
    }

    private companion object {
        const val STOP_ID_PREFIX = "mv:"
        const val LINE_ID_PREFIX = "mv:"
        const val STOP_TIMEOUT_MS = 30_000L
        const val BACKOFF_BASE_MS = 2_000L
        const val BACKOFF_CAP_MS = 60_000L
        const val BACKOFF_MAX_SHIFT = 5
        const val HTTP_OK = 200
    }
}
