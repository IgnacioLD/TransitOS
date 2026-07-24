package app.transitos.provider.metrovalencia

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Static configuration for the Metrovalencia provider.
 *
 * [baseUrl] targets the production endpoint exposed by FGV for its official app.
 * See `provider-metrovalencia/RESEARCH.md` for how it was discovered and why it
 * differs from the 2022-era `ap18/api/public/...` URL referenced by some
 * third-party projects.
 *
 * Poll intervals are deliberately conservative — the underlying data does not
 * change faster than these windows, and we want to be a polite client of an
 * undocumented endpoint.
 */
class MetrovalenciaConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    private val languageProvider: () -> String = { "es" },
    val sede: String = "V",
    val arrivalsPollMs: Long = 30_000L,
    val alertsPollMs: Long = 5 * 60_000L,
    val catalogRefreshMs: Long = 6 * 60 * 60_000L,
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
    }
}
