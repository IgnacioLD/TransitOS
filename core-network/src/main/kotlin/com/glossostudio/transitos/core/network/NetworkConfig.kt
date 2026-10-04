package com.glossostudio.transitos.core.network

/**
 * Static configuration for the network stack. Built once at app start and shared
 * by every provider. Provider-specific values (API keys, per-op base URLs) live
 * on the provider modules, not here.
 */
data class NetworkConfig(
    val debug: Boolean = false,
    val connectTimeoutMs: Long = 10_000,
    val requestTimeoutMs: Long = 15_000,
)
