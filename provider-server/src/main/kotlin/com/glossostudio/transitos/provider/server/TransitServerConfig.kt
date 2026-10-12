package com.glossostudio.transitos.provider.server

/**
 * Where the aggregation server lives and how often the app polls it.
 *
 * [baseUrl] is supplied by the application (from `BuildConfig.SERVER_BASE_URL`),
 * so the same module points at a local emulator host in development and at the
 * deployed VPS in release without a rebuild of the module itself.
 */
data class TransitServerConfig(
    val baseUrl: String,
    val arrivalsPollMs: Long = DEFAULT_ARRIVALS_POLL_MS,
    val alertsPollMs: Long = DEFAULT_ALERTS_POLL_MS,
    val catalogRefreshMs: Long = DEFAULT_CATALOG_REFRESH_MS,
) {
    companion object {
        private const val DEFAULT_ARRIVALS_POLL_MS = 30_000L
        private const val DEFAULT_ALERTS_POLL_MS = 5 * 60_000L
        private const val DEFAULT_CATALOG_REFRESH_MS = 6 * 60 * 60_000L
    }
}
