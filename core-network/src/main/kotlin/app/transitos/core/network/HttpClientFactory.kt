package com.glossostudio.transitos.core.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the single [HttpClient] used by every provider. Centralising the
 * client keeps connection pooling, JSON policy, cookies and timeouts consistent
 * across operators.
 *
 * Cookies matter for FGV: its API hands out a session cookie on the public
 * catalogue endpoints and `horarios-prevision-3` returns HTTP 404 without it.
 * Installing [HttpCookies] here means the shared client keeps that session for
 * every subsequent request.
 */
class HttpClientFactory(private val config: NetworkConfig) {
    fun create(): HttpClient = HttpClient(Android) { installTransitOsPlugins(config) }
}

/**
 * Engine-agnostic plugin set shared by the production client and tests. Extracted
 * so a `MockEngine`-backed test can exercise exactly the same stack (notably the
 * cookie storage) instead of a hand-rolled approximation.
 */
fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installTransitOsPlugins(config: NetworkConfig) {
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
    if (config.debug) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.i("TransitOS.Http", message)
                }
            }
            level = LogLevel.HEADERS
        }
    }
}
