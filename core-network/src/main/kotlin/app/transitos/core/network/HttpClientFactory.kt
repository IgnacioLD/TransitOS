package app.transitos.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import android.util.Log

/**
 * Builds the single [HttpClient] used by every provider. Centralising the
 * client keeps connection pooling, JSON policy and timeouts consistent across
 * operators.
 *
 * Auth and retry policies will be added here once the Metrovalencia API
 * investigation (next milestone) tells us what they need to look like.
 */
class HttpClientFactory(private val config: NetworkConfig) {
    fun create(): HttpClient = HttpClient(Android) {
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
}
