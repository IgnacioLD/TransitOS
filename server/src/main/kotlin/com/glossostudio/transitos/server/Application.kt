package com.glossostudio.transitos.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

/**
 * The multi-operator aggregation server (see `docs/multi-operator.md`). Phase 0
 * is the skeleton: a health endpoint and configuration plumbing. Operator
 * endpoints are added in later phases.
 */
fun main() {
    val config = ServerConfig.fromEnv()
    embeddedServer(Netty, host = config.host, port = config.port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    routing {
        get("/health") {
            call.respond(HealthResponse(status = STATUS_OK, version = SERVER_VERSION))
        }
    }
}

private const val STATUS_OK = "ok"
