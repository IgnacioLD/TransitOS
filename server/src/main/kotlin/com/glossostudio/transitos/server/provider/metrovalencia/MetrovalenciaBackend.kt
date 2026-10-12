package com.glossostudio.transitos.server.provider.metrovalencia

/**
 * Selectable data backends for Metrovalencia. The server simply advertises them
 * on `/operators`; the app picks one per operator and sends it on each request
 * (wired in a later phase).
 */
enum class MetrovalenciaBackend(
    val id: String,
    val displayName: String,
) {
    FGV("fgv", "FGV (Tiempo real, inestable)"),
    NAP("nap", "NAP (Horarios programados)"),
}
