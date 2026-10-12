package com.glossostudio.transitos.server

/**
 * Runtime configuration, sourced from the environment so the same container runs
 * unchanged in development and on the VPS. Upstream secrets (the NAP `ApiKey`, in
 * later phases) are environment-only and must never be hardcoded here.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
) {
    companion object {
        const val DEFAULT_HOST: String = "0.0.0.0"
        const val DEFAULT_PORT: Int = 8080

        fun fromEnv(env: (String) -> String? = { System.getenv(it) }): ServerConfig = ServerConfig(
            host = env("TRANSITOS_HOST")?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST,
            port = env("TRANSITOS_PORT")?.toIntOrNull() ?: DEFAULT_PORT,
        )
    }
}
