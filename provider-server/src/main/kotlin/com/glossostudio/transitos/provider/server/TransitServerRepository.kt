package com.glossostudio.transitos.provider.server

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.LineGeometry
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.network.NetworkMonitor
import com.glossostudio.transitos.core.network.safeCall
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.core.result.AppResult
import com.glossostudio.transitos.core.result.getOrNull
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

/**
 * [TransitRepository] backed by the TransitOS aggregation server.
 *
 * The server already caches politely upstream, so this client stays simple: it
 * polls the unified API on the same cadence the app always used and applies the
 * same failure contract through [polling]. Catalogs are shared [StateFlow]s,
 * live arrivals poll per stop.
 */
class TransitServerRepository(
    private val api: TransitServerApi,
    private val config: TransitServerConfig,
    private val ioScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
) : TransitRepository {

    private val refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun refresh() {
        refreshEvents.tryEmit(Unit)
    }

    init {
        ioScope.launch {
            networkMonitor.isOnline.filter { it }.collect { refreshEvents.emit(Unit) }
        }
    }

    private val linesState: StateFlow<AppResult<List<Line>>> = polling(config.catalogRefreshMs) {
        api.getLines()
    }.stateIn(ioScope, SharingStarted.Eagerly, AppResult.Loading)

    private val stopsState: StateFlow<AppResult<List<Stop>>> = polling(config.catalogRefreshMs) {
        api.getStops()
    }.stateIn(ioScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppResult.Loading)

    private val alertsState: StateFlow<AppResult<List<Alert>>> = polling(config.alertsPollMs) {
        api.getAlerts()
    }.stateIn(ioScope, SharingStarted.Eagerly, AppResult.Loading)

    private val lineGeometriesState: StateFlow<List<LineGeometry>> = polling(config.catalogRefreshMs) {
        api.getGeometries()
    }
        .map { it.getOrNull().orEmpty() }
        .stateIn(scope = ioScope, started = SharingStarted.Lazily, initialValue = emptyList())

    override fun observeStops(): Flow<AppResult<List<Stop>>> = stopsState
    override fun observeLines(): Flow<AppResult<List<Line>>> = linesState
    override fun observeLineGeometries(): Flow<List<LineGeometry>> = lineGeometriesState
    override fun observeAlerts(): Flow<AppResult<List<Alert>>> = alertsState

    override fun observeArrivals(stopId: String): Flow<AppResult<List<Arrival>>> = polling(config.arrivalsPollMs) { api.getArrivals(stopId) }

    override suspend fun planJourney(
        originStopId: String,
        destinationStopId: String,
        date: LocalDate,
        hora: String?,
        isDeparture: Boolean,
        minTransferMinutes: Int,
    ): AppResult<List<Journey>> = safeCall {
        api.planJourney(
            PlanRequest(
                originStopId = originStopId,
                destinationStopId = destinationStopId,
                date = date,
                hora = hora,
                isDeparture = isDeparture,
                minTransferMinutes = minTransferMinutes,
            ),
        )
    }

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
        const val STOP_TIMEOUT_MS = 30_000L
        const val BACKOFF_BASE_MS = 2_000L
        const val BACKOFF_CAP_MS = 60_000L
        const val BACKOFF_MAX_SHIFT = 5
    }
}
