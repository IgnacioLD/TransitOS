package com.glossostudio.transitos.provider.metrovalencia

import com.glossostudio.transitos.core.provider.ProviderBackend

enum class MetrovalenciaBackend(
    override val id: String,
    override val displayName: String,
) : ProviderBackend {
    FGV("fgv", "FGV (Tiempo real — inestable)"),
    NAP("nap", "NAP (Horarios programados)"),
}

val METROVALENCIA_BACKENDS: List<MetrovalenciaBackend> = MetrovalenciaBackend.entries

fun String.toMetrovalenciaBackendOr(default: MetrovalenciaBackend): MetrovalenciaBackend =
    MetrovalenciaBackend.entries.firstOrNull { it.id == this } ?: default
