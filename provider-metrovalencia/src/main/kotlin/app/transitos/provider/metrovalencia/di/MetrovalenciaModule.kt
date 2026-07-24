package app.transitos.provider.metrovalencia.di

import app.transitos.core.provider.ProviderInfo
import app.transitos.core.provider.ProviderRegistry
import app.transitos.core.provider.ProviderSettingsRepository
import app.transitos.core.repository.TransitRepository
import app.transitos.provider.metrovalencia.MetrovalenciaBackend
import app.transitos.provider.metrovalencia.MetrovalenciaConfig
import app.transitos.provider.metrovalencia.MetrovalenciaRepository
import app.transitos.provider.metrovalencia.api.MetrovalenciaApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Wires the Metrovalencia provider into the app graph.
 *
 * Adding a second operator (EMT, Renfe, …) means adding another provider module
 * like this one and reconciling the [TransitRepository] binding — the rest of
 * the app stays untouched.
 */
val metrovalenciaModule = module {
    single { MetrovalenciaConfig() }
    single { MetrovalenciaApi(get(), get()) }

    // Register this provider in the global registry so the Settings screen
    // can discover it without knowing the module at compile time.
    single {
        val registry = get<ProviderRegistry>()
        registry.register(
            ProviderInfo(
                id = MetrovalenciaConfig.OPERATOR_ID,
                name = MetrovalenciaConfig.OPERATOR_NAME,
                backends = MetrovalenciaBackend.entries,
            ),
        )
    }

    // Dedicated, long-lived scope for the provider's background polling loops.
    // SupervisorJob so one failing fetch doesn't cancel sibling fetches.
    single<CoroutineScope>(named(PROVIDER_IO_QUALIFIER)) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<TransitRepository> {
        MetrovalenciaRepository(
            api = get(),
            config = get(),
            ioScope = get(qualifier = named(PROVIDER_IO_QUALIFIER)),
            settings = get(),
        )
    }
}

const val PROVIDER_IO_QUALIFIER = "provider_io"
