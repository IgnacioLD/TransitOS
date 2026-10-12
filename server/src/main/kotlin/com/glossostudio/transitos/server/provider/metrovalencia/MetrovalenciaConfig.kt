package com.glossostudio.transitos.server.provider.metrovalencia

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Static configuration for the Metrovalencia provider.
 *
 * [baseUrl] targets the production endpoint exposed by FGV for its official app.
 * See `server/RESEARCH-metrovalencia.md` for how it was discovered and why it
 * differs from the 2022-era `ap18/api/public/...` URL referenced by some
 * third-party projects.
 *
 * Poll intervals are deliberately conservative, the underlying data does not
 * change faster than these windows, and we want to be a polite client of an
 * undocumented endpoint.
 */
class MetrovalenciaConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    private val languageProvider: () -> String = { "es" },
    val sede: String = "V",
    val arrivalsPollMs: Long = DEFAULT_ARRIVALS_POLL_MS,
    val alertsPollMs: Long = DEFAULT_ALERTS_POLL_MS,
    val catalogRefreshMs: Long = DEFAULT_CATALOG_REFRESH_MS,
) {
    val language: String
        get() = languageProvider()

    val fullBaseUrl: String
        get() = "$baseUrl${languageProvider()}/api/v1/$sede/"

    @OptIn(ExperimentalCoroutinesApi::class)
    companion object {
        const val OPERATOR_ID = "metrovalencia"
        const val OPERATOR_NAME = "Metrovalencia"
        const val DEFAULT_BASE_URL = "https://www.fgv.es/fgv/app/"

        private const val DEFAULT_ARRIVALS_POLL_MS = 30_000L
        private const val DEFAULT_ALERTS_POLL_MS = 5 * 60_000L
        private const val DEFAULT_CATALOG_REFRESH_MS = 6 * 60 * 60_000L
    }
}
