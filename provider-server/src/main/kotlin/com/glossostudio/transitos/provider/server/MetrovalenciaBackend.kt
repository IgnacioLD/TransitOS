package com.glossostudio.transitos.provider.server

import com.glossostudio.transitos.core.provider.ProviderBackend

/**
 * Selectable backends for the Metrovalencia operator, exposed to Settings.
 * The server advertises the same list on `/operators`.
 */
enum class MetrovalenciaBackend(
    override val id: String,
    override val displayName: String,
) : ProviderBackend {
    FGV("fgv", "FGV (Tiempo real, inestable)"),
    NAP("nap", "NAP (Horarios programados)"),
}
