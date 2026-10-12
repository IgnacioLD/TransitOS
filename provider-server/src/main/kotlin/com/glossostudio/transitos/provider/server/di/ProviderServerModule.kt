package com.glossostudio.transitos.provider.server.di

import com.glossostudio.transitos.core.provider.ProviderInfo
import com.glossostudio.transitos.core.provider.ProviderRegistry
import com.glossostudio.transitos.core.repository.TransitRepository
import com.glossostudio.transitos.provider.server.MetrovalenciaBackend
import com.glossostudio.transitos.provider.server.TransitServerApi
import com.glossostudio.transitos.provider.server.TransitServerConfig
import com.glossostudio.transitos.provider.server.TransitServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Wires the server-backed provider into the app graph. [baseUrl] comes from the
 * application (`BuildConfig.SERVER_BASE_URL`) so the module stays environment
 * agnostic.
 */
fun providerServerModule(baseUrl: String): Module = module {
    single { TransitServerConfig(baseUrl = baseUrl) }
    single { TransitServerApi(get(), get()) }

    // Register the operator so Settings can discover it without compile-time
    // knowledge of the module.
    single(createdAtStart = true) {
        get<ProviderRegistry>().register(
            ProviderInfo(
                id = OPERATOR_ID,
                name = OPERATOR_NAME,
                backends = MetrovalenciaBackend.entries,
            ),
        )
    }

    // Dedicated, long-lived scope for the provider's background polling loops.
    single<CoroutineScope>(named(PROVIDER_IO_QUALIFIER)) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<TransitRepository> {
        TransitServerRepository(
            api = get(),
            config = get(),
            ioScope = get(qualifier = named(PROVIDER_IO_QUALIFIER)),
            networkMonitor = get(),
        )
    }
}

const val PROVIDER_IO_QUALIFIER: String = "provider_io"

private const val OPERATOR_ID = "metrovalencia"
private const val OPERATOR_NAME = "Metrovalencia"
