package com.glossostudio.transitos.provider.metrovalencia.api

import com.glossostudio.transitos.core.network.NetworkConfig
import com.glossostudio.transitos.core.network.installTransitOsPlugins
import com.glossostudio.transitos.provider.metrovalencia.MetrovalenciaConfig
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MetrovalenciaApiSessionTest {

    private val arrivalsJson =
        """{"previsiones":[{"line":3,"line_id":3,"linea_id_interna":44,""" +
            """"trains":[{"destino":"Aeroport","seconds":120,"vehicle":3086}]}]}"""

    private val stationsJson = """[{"id":226,"estacion_id_FGV":12,"nombre":"Benimaclet"}]"""

    private val sessionHeaders = headersOf(
        HttpHeaders.ContentType to listOf("application/json"),
        HttpHeaders.SetCookie to listOf("fgv_api_session=abc123; Path=/"),
    )

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val sessionErrorJson = """{"status":400,"error":-1,"resultado":"Error"}"""

    private fun api(engine: MockEngine): MetrovalenciaApi {
        val client = HttpClient(engine) {
            installTransitOsPlugins(NetworkConfig(debug = false))
        }
        return MetrovalenciaApi(client, MetrovalenciaConfig())
    }

    private fun pathOf(request: HttpRequestData): String = request.url.encodedPath

    @Test
    fun `arrivals prime the session first and reuse the cookie`() = runTest {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests += pathOf(request)
            when {
                request.url.encodedPath.endsWith("estaciones") ->
                    respond(stationsJson, HttpStatusCode.OK, sessionHeaders)
                request.headers[HttpHeaders.Cookie] == null ->
                    respond(sessionErrorJson, HttpStatusCode.NotFound, jsonHeaders)
                else ->
                    respond(arrivalsJson, HttpStatusCode.OK, jsonHeaders)
            }
        }

        val arrivals = api(engine).getArrivals(12)

        assertThat(arrivals.previsiones).hasSize(1)
        assertThat(requests).hasSize(2)
        assertThat(requests[0]).endsWith("estaciones")
        assertThat(requests[1]).endsWith("horarios-prevision-3/12")
    }

    @Test
    fun `session is primed once and reused across polls`() = runTest {
        var primes = 0
        var arrivalsCalls = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("estaciones")) {
                primes++
                respond(stationsJson, HttpStatusCode.OK, sessionHeaders)
            } else {
                arrivalsCalls++
                respond(arrivalsJson, HttpStatusCode.OK, jsonHeaders)
            }
        }

        val api = api(engine)
        api.getArrivals(12)
        api.getArrivals(12)

        assertThat(primes).isEqualTo(1)
        assertThat(arrivalsCalls).isEqualTo(2)
    }

    @Test
    fun `failed arrivals re-prime the session and retry once`() = runTest {
        var primes = 0
        var arrivalsCalls = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("estaciones")) {
                primes++
                respond(stationsJson, HttpStatusCode.OK, sessionHeaders)
            } else {
                arrivalsCalls++
                if (arrivalsCalls == 1) {
                    respond(sessionErrorJson, HttpStatusCode.NotFound, jsonHeaders)
                } else {
                    respond(arrivalsJson, HttpStatusCode.OK, jsonHeaders)
                }
            }
        }

        val arrivals = api(engine).getArrivals(12)

        assertThat(arrivals.previsiones).hasSize(1)
        assertThat(primes).isEqualTo(2)
        assertThat(arrivalsCalls).isEqualTo(2)
    }
}
