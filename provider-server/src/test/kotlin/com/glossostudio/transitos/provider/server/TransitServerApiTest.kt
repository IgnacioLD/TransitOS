package com.glossostudio.transitos.provider.server

import com.glossostudio.transitos.core.model.TransportMode
import com.glossostudio.transitos.core.network.NetworkConfig
import com.glossostudio.transitos.core.network.installTransitOsPlugins
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test

class TransitServerApiTest {

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine): TransitServerApi {
        val client = HttpClient(engine) { installTransitOsPlugins(NetworkConfig(debug = false)) }
        return TransitServerApi(client, TransitServerConfig(baseUrl = "https://server.test/"))
    }

    @Test
    fun `stops decode into domain models`() = runTest {
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/stops")
            respond(
                """[{"id":"mv:12","name":"Benimaclet","operatorId":"metrovalencia",""" +
                    """"mode":"METRO","lat":39.4848,"lon":-0.3623,"parentStationId":null}]""",
                HttpStatusCode.OK,
                jsonHeaders(),
            )
        }

        val stops = api(engine).getStops()

        assertThat(stops).hasSize(1)
        assertThat(stops[0].id).isEqualTo("mv:12")
        assertThat(stops[0].name).isEqualTo("Benimaclet")
        assertThat(stops[0].operatorId).isEqualTo("metrovalencia")
        assertThat(stops[0].mode).isEqualTo(TransportMode.METRO)
    }

    @Test
    fun `arrivals request the stop path`() = runTest {
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/arrivals/mv:12")
            respond("[]", HttpStatusCode.OK, jsonHeaders())
        }

        assertThat(api(engine).getArrivals("mv:12")).isEmpty()
    }

    @Test
    fun `plan sends every query parameter`() = runTest {
        var captured: Parameters? = null
        val engine = MockEngine { request ->
            captured = request.url.parameters
            respond("[]", HttpStatusCode.OK, jsonHeaders())
        }

        api(engine).planJourney(
            PlanRequest(
                originStopId = "mv:1",
                destinationStopId = "mv:2",
                date = LocalDate.parse("2026-07-23"),
                hora = "10:00",
                isDeparture = false,
                minTransferMinutes = 7,
            ),
        )

        val params = checkNotNull(captured)
        assertThat(params["from"]).isEqualTo("mv:1")
        assertThat(params["to"]).isEqualTo("mv:2")
        assertThat(params["date"]).isEqualTo("2026-07-23")
        assertThat(params["time"]).isEqualTo("10:00")
        assertThat(params["arriveBy"]).isEqualTo("true")
        assertThat(params["minTransferMinutes"]).isEqualTo("7")
    }
}
