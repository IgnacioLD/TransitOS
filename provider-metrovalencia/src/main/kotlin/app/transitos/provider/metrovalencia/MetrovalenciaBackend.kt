package app.transitos.provider.metrovalencia

import app.transitos.core.provider.ProviderBackend

enum class MetrovalenciaBackend(
    override val id: String,
    override val displayName: String,
) : ProviderBackend {
    FGV("fgv", "FGV (Tiempo real)"),
    NAP("nap", "NAP (Horarios)"),
}

val METROVALENCIA_BACKENDS: List<MetrovalenciaBackend> = MetrovalenciaBackend.entries

fun String.toMetrovalenciaBackendOr(default: MetrovalenciaBackend): MetrovalenciaBackend =
    MetrovalenciaBackend.entries.firstOrNull { it.id == this } ?: default
