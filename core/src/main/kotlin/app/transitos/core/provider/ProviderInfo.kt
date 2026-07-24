package com.glossostudio.transitos.core.provider

public data class ProviderInfo(
    val id: String,
    val name: String,
    val backends: List<ProviderBackend>,
)
