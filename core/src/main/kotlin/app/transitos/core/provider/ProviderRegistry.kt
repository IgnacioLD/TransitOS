package app.transitos.core.provider

public class ProviderRegistry {
    private val _providers: MutableList<ProviderInfo> = mutableListOf()

    public val providers: List<ProviderInfo> get() = _providers.toList()

    public fun register(info: ProviderInfo) {
        _providers.add(info)
    }
}
