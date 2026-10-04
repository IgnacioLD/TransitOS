package com.glossostudio.transitos.core.network

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HttpClientCookiesTest {

    @Test
    fun `cookies set by the server are replayed on later requests`() = runTest {
        val engine = MockEngine { request ->
            val cookie = request.headers[HttpHeaders.Cookie]
            respond(
                content = cookie ?: "no-cookie",
                status = HttpStatusCode.OK,
                headers = if (cookie == null) {
                    headersOf(
                        HttpHeaders.ContentType to listOf("text/plain"),
                        HttpHeaders.SetCookie to listOf("fgv_api_session=abc123; Path=/"),
                    )
                } else {
                    headersOf(HttpHeaders.ContentType, "text/plain")
                },
            )
        }
        val client = HttpClient(engine) { installTransitOsPlugins(NetworkConfig(debug = false)) }

        val first = client.get("https://www.fgv.es/fgv/app/es/api/v1/V/estaciones").bodyAsText()
        val second = client.get("https://www.fgv.es/fgv/app/es/api/v1/V/lineas").bodyAsText()

        assertThat(first).isEqualTo("no-cookie")
        assertThat(second).contains("fgv_api_session=abc123")
    }
}
