package com.glossostudio.transitos.server.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the single upstream [HttpClient] the server uses to talk to operators.
 *
 * Cookies matter for FGV: its catalogue endpoints hand out a session cookie and
 * `horarios-prevision-3` answers HTTP 404 without it. Keeping [HttpCookies] here
 * means the session is established once, server-side, and shared by every client
 * request instead of being re-primed per user.
 *
 * `expectSuccess = false` so non-2xx responses are returned and classified
 * explicitly via `requireSuccess`, matching the app's previous behaviour.
 */
object UpstreamHttpClientFactory {
    fun create(): HttpClient = HttpClient(CIO) { installUpstreamPlugins() }
}

/**
 * Engine-agnostic plugin set shared by the production client and the tests, so a
 * `MockEngine`-backed test exercises exactly the same stack (notably the cookie
 * storage) instead of a hand-rolled approximation.
 */
fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installUpstreamPlugins() {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
    }
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            },
        )
    }
}

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 20_000L
private const val REQUEST_TIMEOUT_MS = 60_000L
