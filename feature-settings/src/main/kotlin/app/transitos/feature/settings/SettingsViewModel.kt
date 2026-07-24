package app.transitos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.transitos.core.provider.ProviderRegistry
import app.transitos.core.provider.ProviderSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val registry: ProviderRegistry,
    private val settings: ProviderSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        observeAllBackends()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(providers = registry.providers),
            )

    fun setBackend(providerId: String, backendId: String) {
        viewModelScope.launch {
            settings.setProviderBackend(providerId, backendId)
        }
    }

    private fun observeAllBackends(): kotlinx.coroutines.flow.Flow<SettingsUiState> {
        val providers = registry.providers
        if (providers.isEmpty()) {
            return flowOf(SettingsUiState(providers = emptyList(), backends = emptyMap()))
        }
        return combine(
            providers.map { provider ->
                settings.observeProviderBackend(provider.id).map { backendId ->
                    val backend = provider.backends.firstOrNull { it.id == backendId }
                        ?: provider.backends.first()
                    provider.id to backend
                }
            },
        ) { pairs -> SettingsUiState(providers = providers, backends = pairs.toMap()) }
    }
}
