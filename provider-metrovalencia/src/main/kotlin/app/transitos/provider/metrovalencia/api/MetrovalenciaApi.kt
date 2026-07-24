package com.glossostudio.transitos.provider.metrovalencia.api

import com.glossostudio.transitos.provider.metrovalencia.MetrovalenciaConfig
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvArrivalResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvHorariosResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvIncidenciasResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvLineDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvPlanificadorResponseDto
import com.glossostudio.transitos.provider.metrovalencia.dto.FgvStationDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Thin Ktor wrapper around the public FGV endpoints TransitOS consumes.
 *
 * No retry, no caching, no error mapping at this layer — that is the repository's
 * job. The only responsibility here is to issue the request and decode the JSON
 * into DTOs. A single shared [HttpClient] is injected from `:core-network`'s
 * Koin module so connection pooling, JSON policy and timeouts stay consistent.
 */
class MetrovalenciaApi(
    private val client: HttpClient,
    private val config: MetrovalenciaConfig,
) {
    suspend fun getStations(): List<FgvStationDto> =
        client.get("${config.fullBaseUrl}estaciones") {
            headerAcceptJson()
        }.body()

    suspend fun getLines(): List<FgvLineDto> =
        client.get("${config.fullBaseUrl}lineas") {
            headerAcceptJson()
        }.body()

    /**
     * @param estacionIdFgv the `estacion_id_FGV` value, not FGV's internal id.
     * The repository is responsible for using the right one.
     */
    suspend fun getArrivals(estacionIdFgv: Long): FgvArrivalResponseDto =
        client.get("${config.fullBaseUrl}horarios-prevision-3/$estacionIdFgv") {
            headerAcceptJson()
        }.body()

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
        // churn — bumping it on releases is enough.
        const val USER_AGENT = "TransitOS/0.1 (+https://github.com/) [Metrovalencia provider]"
    }
}
