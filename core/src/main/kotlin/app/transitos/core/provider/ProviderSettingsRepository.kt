package com.glossostudio.transitos.core.provider

import kotlinx.coroutines.flow.Flow

public interface ProviderSettingsRepository {
    public fun observeProviderBackend(providerId: String): Flow<String>
    public suspend fun setProviderBackend(providerId: String, backendId: String)
}
