package com.glossostudio.transitos.core.network

/**
 * Static configuration for the network stack. Built once at app start and shared
 * by every provider. Provider-specific values (API keys, per-op base URLs) live
 * on the provider modules, not here.
 */
data class NetworkConfig(
    val debug: Boolean = false,
    /** TCP connect; generous because slow DNS on mobile networks counts too. */
    val connectTimeoutMs: Long = 15_000,
    /** Max silence between bytes: catches a stalled connection quickly. */
    val socketTimeoutMs: Long = 20_000,
    /** Whole request including the body; the map bundle is ~6 MB on slow links. */
    val requestTimeoutMs: Long = 60_000,
)
