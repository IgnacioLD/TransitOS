package com.glossostudio.transitos.server

import com.glossostudio.transitos.core.network.HttpStatusException
import com.glossostudio.transitos.server.api.ErrorResponse
import com.glossostudio.transitos.server.api.transitRoutes
import com.glossostudio.transitos.server.net.UpstreamHttpClientFactory
import com.glossostudio.transitos.server.provider.metrovalencia.MetrovalenciaConfig
import com.glossostudio.transitos.server.provider.metrovalencia.api.MetrovalenciaApi
import com.glossostudio.transitos.server.service.MetrovalenciaService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

/**
 * The multi-operator aggregation server (see `docs/multi-operator.md`). It
 * fetches from the operators, parses and caches politely, and exposes one
 * unified REST API the app consumes through a single thin provider.
 */
fun main() {
    val config = ServerConfig.fromEnv()
    val client = UpstreamHttpClientFactory.create()
    val fgvConfig = MetrovalenciaConfig()
    val metrovalencia = MetrovalenciaService(
        api = MetrovalenciaApi(client, fgvConfig),
        config = fgvConfig,
    )

    embeddedServer(Netty, host = config.host, port = config.port) {
        transitModule(metrovalencia)
    }.start(wait = true)

    client.close()
}

fun Application.transitModule(metrovalencia: MetrovalenciaService) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }
    install(StatusPages) {
        exception<HttpStatusException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.status),
                ErrorResponse(cause.message ?: "upstream error"),
            )
        }
        exception<Exception> { call, cause ->
            // Any non-HTTP failure is an upstream problem as far as the client is
            // concerned. Never leak stack traces to the app.
            call.respond(HttpStatusCode.BadGateway, ErrorResponse(cause.message ?: "upstream failure"))
        }
    }
    routing {
        get("/health") { call.respond(HealthResponse(status = STATUS_OK, version = SERVER_VERSION)) }
        transitRoutes(metrovalencia)
    }
}

private const val STATUS_OK = "ok"
