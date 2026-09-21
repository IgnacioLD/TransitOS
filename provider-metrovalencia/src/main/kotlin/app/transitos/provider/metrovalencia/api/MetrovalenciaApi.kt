package com.glossostudio.transitos.provider.metrovalencia.api

import com.glossostudio.transitos.provider.metrovalencia.MetrovalenciaConfig
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvArrivalResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvHorariosResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvIncidenciasResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvLineDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPlanificadorResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvSincronizacionResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvStationDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thin Ktor wrapper around the public FGV endpoints TransitOS consumes.
 *
 * No generic retry, no caching, no error mapping at this layer, that is the
 * repository's job. The one exception is the FGV session bootstrap for live
 * arrivals (see [getArrivals]): the session is an HTTP-transport concern and
 * belongs next to the endpoints that need it. A single shared [HttpClient] is
 * injected from `:core-network`'s Koin module so connection pooling, cookie
 * storage, JSON policy and timeouts stay consistent.
 */
class MetrovalenciaApi(
    private val client: HttpClient,
    private val config: MetrovalenciaConfig,
) {
    private val sessionMutex = Mutex()
    @Volatile
    private var sessionEstablished = false

    suspend fun getStations(): List<FgvStationDto> =
        client.get("${config.fullBaseUrl}estaciones") {
            headerAcceptJson()
        }.body()

    suspend fun getLines(): List<FgvLineDto> =
        client.get("${config.fullBaseUrl}lineas") {
            headerAcceptJson()
        }.body()

    /**
     * The heavy catalogue bundle (lineas + estaciones + track shapes). Only the
     * map needs it, so the repository keeps it behind a lazily-shared flow and
     * refreshes it at the slow catalogue cadence.
     */
    suspend fun getSincronizacion(): FgvSincronizacionResponseDto =
        client.get("${config.fullBaseUrl}sincronizacion") {
            headerAcceptJson()
        }.body()

    /**
     * @param estacionIdFgv the `estacion_id_FGV` value, not FGV's internal id.
     * The repository is responsible for using the right one.
     *
     * FGV now requires a session before serving live arrivals: without the
     * `fgv_api_session`/`XSRF-TOKEN` cookies set by a catalogue call, this
     * endpoint answers HTTP 404. The shared client's [HttpCookies] plugin keeps
     * whatever session we obtain, so [ensureSession] primes once and the poll
     * loop reuses it. If the request still fails (expired session), we reset the
     * flag, prime again and retry exactly once.
     */
    suspend fun getArrivals(estacionIdFgv: Long): FgvArrivalResponseDto {
        ensureSession()
        return try {
            fetchArrivals(estacionIdFgv)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            resetSession()
            ensureSession()
            fetchArrivals(estacionIdFgv)
        }
    }

    /**
     * Establishes the FGV session by hitting a public catalogue endpoint
     * (`estaciones`), which sets the session cookies. Idempotent and cheap after
     * the first call: the flag avoids priming on every arrival poll.
     */
    private suspend fun ensureSession() {
        if (sessionEstablished) return
        sessionMutex.withLock {
            if (sessionEstablished) return
            client.get("${config.fullBaseUrl}estaciones") {
                headerAcceptJson()
            }.body<List<FgvStationDto>>()
            sessionEstablished = true
        }
    }

    private fun resetSession() {
        sessionEstablished = false
    }

    private suspend fun fetchArrivals(estacionIdFgv: Long): FgvArrivalResponseDto {
        val response: HttpResponse = client.get("${config.fullBaseUrl}horarios-prevision-3/$estacionIdFgv") {
            headerAcceptJson()
        }
        if (!response.status.isSuccess()) {
            error("FGV horarios-prevision-3 returned HTTP ${response.status}")
        }
        return response.body()
    }

    suspend fun getIncidencias(): FgvIncidenciasResponseDto =
        client.get("${config.fullBaseUrl}incidencias") {
            headerAcceptJson()
        }.body()

    /**
     * Plans a journey via FGV's `horarios-online2`.
     *
     * **Quirks discovered by probing** (see `RESEARCH.md`):
     *  - Body is form-encoded, NOT JSON. Posting JSON returns 422.
     *  - [fecha] must be `dd/MM/yyyy` (Spanish format). ISO `yyyy-MM-dd` returns
     *    HTTP 400 with "Error obteniendo horarios".
     *  - Station ids are FGV's **internal** ids (e.g. 226 for Benimaclet), not
     *    `estacion_id_FGV` (12). The repository resolves the mapping.
     */
    suspend fun planJourney(
        originInternalId: Long,
        destinationInternalId: Long,
        fecha: String,
        hora: String? = null,
    ): FgvHorariosResponseDto = client.post("${config.fullBaseUrl}horarios-online2") {
        headerAcceptJson()
        contentType(ContentType.Application.FormUrlEncoded)
        val body = buildString {
            append("estacion_origen_id=$originInternalId")
            append("&estacion_destino_id=$destinationInternalId")
            append("&fecha=$fecha")
            if (hora != null) append("&hora=$hora")
        }
        setBody(body)
    }.body()

    /**
     * Plans a journey via FGV's `planificador-online2`.
     *
     * Unlike `horarios-online2`, this endpoint returns specific journeys with
     * exact times per leg, line colors, and transfer info.
     *
     * Pass either [horaSalida] or [horaLlegada], not both:
     *  - Departure search: set horaSalida, leave horaLlegada null.
     *  - Arrival search: set horaLlegada, leave horaSalida null.
     */
    suspend fun planificadorOnline(
        originInternalId: Long,
        destinationInternalId: Long,
        fecha: String,
        horaSalida: String? = null,
        horaLlegada: String? = null,
    ): FgvPlanificadorResponseDto = client.post("${config.fullBaseUrl}planificador-online2") {
        headerAcceptJson()
        contentType(ContentType.Application.FormUrlEncoded)
        val body = buildString {
            append("estacion_origen_id=$originInternalId")
            append("&estacion_destino_id=$destinationInternalId")
            append("&fecha=$fecha")
            horaSalida?.let { append("&hora_salida=$it") }
            horaLlegada?.let { append("&hora_llegada=$it") }
        }
        setBody(body)
    }.body()

    private fun io.ktor.client.request.HttpRequestBuilder.headerAcceptJson() {
        header("Accept", "application/json")
        header("User-Agent", USER_AGENT)
    }

    private companion object {
        // Identifies TransitOS to FGV's logs. Polite, traceable, no version
        // churn, bumping it on releases is enough.
        const val USER_AGENT = "TransitOS/0.1 (+https://github.com/) [Metrovalencia provider]"
    }
}
