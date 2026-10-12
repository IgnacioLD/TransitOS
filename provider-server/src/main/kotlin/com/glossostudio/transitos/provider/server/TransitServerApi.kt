package com.glossostudio.transitos.provider.server

import com.glossostudio.transitos.core.model.Alert
import com.glossostudio.transitos.core.model.Arrival
import com.glossostudio.transitos.core.model.Journey
import com.glossostudio.transitos.core.model.Line
import com.glossostudio.transitos.core.model.LineGeometry
import com.glossostudio.transitos.core.model.Stop
import com.glossostudio.transitos.core.network.requireSuccess
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate

/** A journey-planning request to the server's `/plan` endpoint. */
data class PlanRequest(
    val originStopId: String,
    val destinationStopId: String,
    val date: LocalDate,
    val hora: String?,
    val isDeparture: Boolean,
    val minTransferMinutes: Int,
)

/**
 * Thin Ktor client for the TransitOS aggregation server's unified API. Every
 * response is JSON in the app's `:core` model shapes, so the bodies decode
 * straight into domain types. No caching or error mapping here, that is the
 * repository's job.
 */
class TransitServerApi(
    private val client: HttpClient,
    private val config: TransitServerConfig,
) {
    suspend fun getStops(): List<Stop> = client.get(url("/stops")).requireSuccess().body()

    suspend fun getLines(): List<Line> = client.get(url("/lines")).requireSuccess().body()

    suspend fun getGeometries(): List<LineGeometry> = client.get(url("/geometries")).requireSuccess().body()

    suspend fun getAlerts(): List<Alert> = client.get(url("/alerts")).requireSuccess().body()

    suspend fun getArrivals(stopId: String): List<Arrival> = client.get(url("/arrivals/$stopId")).requireSuccess().body()

    suspend fun planJourney(request: PlanRequest): List<Journey> = client.get(url("/plan")) {
        parameter("from", request.originStopId)
        parameter("to", request.destinationStopId)
        parameter("date", request.date.toString())
        if (request.hora != null) parameter("time", request.hora)
        parameter("arriveBy", (!request.isDeparture).toString())
        parameter("minTransferMinutes", request.minTransferMinutes)
    }.requireSuccess().body()

    private fun url(path: String): String = config.baseUrl.trimEnd('/') + path
}
