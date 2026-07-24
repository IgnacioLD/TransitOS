package com.glossostudio.transitos.feature.settings

import com.glossostudio.transitos.core.provider.ProviderBackend
import com.glossostudio.transitos.core.provider.ProviderInfo

data class SettingsUiState(
    val providers: List<ProviderInfo> = emptyList(),
    val backends: Map<String, ProviderBackend> = emptyMap(),
)
