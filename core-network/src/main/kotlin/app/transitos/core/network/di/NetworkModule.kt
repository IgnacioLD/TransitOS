package app.transitos.core.network.di

import app.transitos.core.network.HttpClientFactory
import app.transitos.core.network.NetworkConfig
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Owns the network-stack graph: the static [NetworkConfig] and the singleton
 * [HttpClient] built from it. Lives in `:core-network` so the Ktor types never
 * leak to consuming modules — they only see the resulting [Module].
 *
 * `debug` is taken as a parameter because only the application module knows
 * `BuildConfig.DEBUG`.
 */
fun networkModule(debug: Boolean): Module = module {
    single { NetworkConfig(debug = debug) }
    single { HttpClientFactory(get()).create() }
}
