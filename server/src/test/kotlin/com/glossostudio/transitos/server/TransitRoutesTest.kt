package com.glossostudio.transitos.server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for the unified API: responses must carry the JSON shapes the
 * app's `:core` models decode. These pin the wire format against drift between
 * the server's models and the client's.
 */
class TransitRoutesTest {
    @Test
    fun stopsEndpointReturnsMappedStops() = testApplication {
        application { transitModule(testService(fgvEngine())) }

        val response = client.get("/stops")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"id\":\"mv:12\""), body)
        assertTrue(body.contains("\"operatorId\":\"metrovalencia\""), body)
        assertTrue(body.contains("\"mode\":\"METRO\""), body)
        assertTrue(body.contains("\"lat\":39.4848"), body)
    }

    @Test
    fun linesEndpointReturnsMappedLines() = testApplication {
        application { transitModule(testService(fgvEngine())) }

        val response = client.get("/lines")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"id\":\"mv:3\""), body)
        assertTrue(body.contains("\"name\":\"3\""), body)
        assertTrue(body.contains("\"color\":4294901760"), body)
    }

    @Test
    fun operatorsEndpointListsMetrovalencia() = testApplication {
        application { transitModule(testService(noUpstreamEngine())) }

        val response = client.get("/operators")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"id\":\"metrovalencia\""), body)
        assertTrue(body.contains("\"backends\""), body)
    }

    @Test
    fun planWithoutRequiredParamsIsBadRequest() = testApplication {
        application { transitModule(testService(noUpstreamEngine())) }

        val response = client.get("/plan?from=mv:1&to=mv:2")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun planWithInvalidDateIsBadRequest() = testApplication {
        application { transitModule(testService(noUpstreamEngine())) }

        val response = client.get("/plan?from=mv:1&to=mv:2&date=29-07-2026")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
