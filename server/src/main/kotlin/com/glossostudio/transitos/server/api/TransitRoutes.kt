package com.glossostudio.transitos.server.api

import com.glossostudio.transitos.server.provider.metrovalencia.MetrovalenciaBackend
import com.glossostudio.transitos.server.service.MetrovalenciaService
import com.glossostudio.transitos.server.service.PlanQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.LocalDate

/**
 * The unified REST API the app consumes. Every response is JSON in the app's
 * `:core` model shapes, so the app decodes it directly into domain types. Stop
 * and line ids are canonical `<operatorId>:<localId>` (`mv:` for Metrovalencia).
 */
fun Route.transitRoutes(metrovalencia: MetrovalenciaService) {
    get("/operators") {
        call.respond(
            listOf(
                OperatorInfo(
                    id = MetrovalenciaService.OPERATOR_ID,
                    name = MetrovalenciaService.OPERATOR_NAME,
                    backends = MetrovalenciaBackend.entries.map { BackendInfo(it.id, it.displayName) },
                ),
            ),
        )
    }

    get("/stops") { call.respond(metrovalencia.stops()) }

    get("/lines") { call.respond(metrovalencia.lines()) }

    get("/geometries") { call.respond(metrovalencia.geometries()) }

    get("/alerts") { call.respond(metrovalencia.alerts()) }

    get("/arrivals/{stopId}") {
        val stopId = call.parameters["stopId"]
        if (stopId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing stopId"))
            return@get
        }
        call.respond(metrovalencia.arrivals(stopId))
    }

    get("/plan") {
        val from = call.request.queryParameters["from"]
        val to = call.request.queryParameters["to"]
        val dateRaw = call.request.queryParameters["date"]
        if (from.isNullOrBlank() || to.isNullOrBlank() || dateRaw.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("from, to and date are required"))
            return@get
        }
        val date = runCatching { LocalDate.parse(dateRaw) }.getOrNull()
        if (date == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("date must be ISO yyyy-MM-dd"))
            return@get
        }
        val time = call.request.queryParameters["time"]
        val isDeparture = call.request.queryParameters["arriveBy"]?.toBoolean()?.not() ?: true
        val minTransferMinutes = call.request.queryParameters["minTransferMinutes"]?.toIntOrNull() ?: DEFAULT_TRANSFER_MINUTES
        call.respond(
            metrovalencia.plan(
                PlanQuery(
                    originStopId = from,
                    destinationStopId = to,
                    date = date,
                    hora = time,
                    isDeparture = isDeparture,
                    minTransferMinutes = minTransferMinutes,
                ),
            ),
        )
    }
}

private const val DEFAULT_TRANSFER_MINUTES = 5
