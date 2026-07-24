package app.transitos.core.data.di

import app.transitos.core.data.DataStoreFavoritesRepository
import app.transitos.core.data.DataStoreProviderSettingsRepository
import app.transitos.core.data.DataStoreRouteFavoritesRepository
import app.transitos.core.data.DataStoreThemePreference
import app.transitos.core.data.SharedPrefsLanguagePreference
import app.transitos.core.provider.ProviderRegistry
import app.transitos.core.provider.ProviderSettingsRepository
import app.transitos.core.repository.FavoritesRepository
import app.transitos.core.repository.LanguagePreference
import app.transitos.core.repository.RouteFavoritesRepository
import app.transitos.core.repository.ThemePreference
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreDataModule = module {
    single<FavoritesRepository> { DataStoreFavoritesRepository(androidContext()) }
    single<ProviderSettingsRepository> { DataStoreProviderSettingsRepository(androidContext()) }
    single<RouteFavoritesRepository> { DataStoreRouteFavoritesRepository(androidContext()) }
    single { ProviderRegistry() }
    single { SharedPrefsLanguagePreference(androidContext()) }
    single<LanguagePreference> { get<SharedPrefsLanguagePreference>() }
    single<ThemePreference> { DataStoreThemePreference(androidContext()) }
}
