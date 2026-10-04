package com.glossostudio.transitos.core.network

import com.glossostudio.transitos.core.result.AppError
import com.glossostudio.transitos.core.result.AppResult
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertThrows
import org.junit.Test

class SafeCallTest {

    @Serializable
    private data class Payload(val name: String)

    private fun client(status: HttpStatusCode, body: String) = HttpClient(
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
    ) { installTransitOsPlugins(NetworkConfig()) }

    @Test
    fun `no DNS resolution means offline`() {
        assertThat(UnknownHostException("www.fgv.es").toAppError()).isInstanceOf(AppError.Offline::class.java)
    }

    @Test
    fun `request timeouts map to timeout`() {
        assertThat(HttpRequestTimeoutException("u", 15_000).toAppError()).isInstanceOf(AppError.Timeout::class.java)
    }

    @Test
    fun `other IO failures map to network`() {
        assertThat(IOException("reset").toAppError()).isInstanceOf(AppError.Network::class.java)
    }

    @Test
    fun `HTTP statuses map to typed errors`() = runTest {
        suspend fun statusError(code: HttpStatusCode) = (safeCall { client(code, "[]").get("https://example.test/x").requireSuccess() } as AppResult.Failure).error

        assertThat(statusError(HttpStatusCode.NotFound)).isInstanceOf(AppError.NotFound::class.java)
        assertThat(statusError(HttpStatusCode.Forbidden)).isInstanceOf(AppError.Unauthorized::class.java)
        assertThat(statusError(HttpStatusCode.UnprocessableEntity)).isEqualTo(
            AppError.Server(422, "HTTP 422 from /x"),
        )
    }

    @Test
    fun `undecodable bodies map to parsing`() = runTest {
        val result = safeCall { client(HttpStatusCode.OK, "[]").get("https://example.test/x").body<Payload>() }
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Parsing::class.java)
    }

    @Test
    fun `successful calls are wrapped`() = runTest {
        val result = safeCall { client(HttpStatusCode.OK, """{"name":"Xàtiva"}""").get("https://example.test/x").body<Payload>() }
        assertThat(result).isEqualTo(AppResult.Success(Payload("Xàtiva")))
    }

    @Test
    fun `cancellation is never swallowed`() {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { safeCall { throw CancellationException("cancelled") } }
        }
    }
}
