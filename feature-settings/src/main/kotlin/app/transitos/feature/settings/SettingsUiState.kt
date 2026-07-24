package app.transitos.feature.settings

import app.transitos.core.provider.ProviderBackend
import app.transitos.core.provider.ProviderInfo

data class SettingsUiState(
    val providers: List<ProviderInfo> = emptyList(),
    val backends: Map<String, ProviderBackend> = emptyMap(),
)
