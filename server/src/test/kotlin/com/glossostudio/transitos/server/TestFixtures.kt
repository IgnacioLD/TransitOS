package com.glossostudio.transitos.server

import com.glossostudio.transitos.server.net.installUpstreamPlugins
import com.glossostudio.transitos.server.provider.metrovalencia.MetrovalenciaConfig
import com.glossostudio.transitos.server.provider.metrovalencia.api.MetrovalenciaApi
import com.glossostudio.transitos.server.service.MetrovalenciaService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** A service wired to the given mock engine, with the production plugin stack. */
internal fun testService(engine: MockEngine): MetrovalenciaService {
    val config = MetrovalenciaConfig(baseUrl = "https://example.test/")
    val client = HttpClient(engine) { installUpstreamPlugins() }
    return MetrovalenciaService(api = MetrovalenciaApi(client, config), config = config)
}

/** An engine that fails the test loudly if any upstream call is made. */
internal fun noUpstreamEngine(): MockEngine = MockEngine { request ->
    respond(
        content = "unexpected upstream call: ${request.url.encodedPath}",
        status = HttpStatusCode.InternalServerError,
    )
}

/** FGV responses needed to exercise the catalogue routes. */
internal fun fgvEngine(): MockEngine = MockEngine { request ->
    when {
        request.url.encodedPath.endsWith("/estaciones") ->
            respond(RESP_ESTACIONES, HttpStatusCode.OK, jsonHeaders())

        request.url.encodedPath.endsWith("/lineas") ->
            respond(RESP_LINEAS, HttpStatusCode.OK, jsonHeaders())

        else -> respond("not found", HttpStatusCode.NotFound)
    }
}

internal fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

internal const val RESP_ESTACIONES: String =
    """[{"id":226,"estacion_id_FGV":12,"nombre":"Benimaclet","latitud":39.4848,"longitud":-0.3623}]"""

internal const val RESP_LINEAS: String =
    """[{"id":1,"linea_id_FGV":3,"nombre_corto":"3","tipo":"1","color":"#FF0000"}]"""
