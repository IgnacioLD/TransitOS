package com.glossostudio.transitos.core.data.di

import com.glossostudio.transitos.core.data.DataStoreFavoritesRepository
import com.glossostudio.transitos.core.data.DataStoreProviderSettingsRepository
import com.glossostudio.transitos.core.data.DataStoreRouteFavoritesRepository
import com.glossostudio.transitos.core.data.DataStoreThemePreference
import com.glossostudio.transitos.core.data.DataStoreTransferBufferPreference
import com.glossostudio.transitos.core.data.SharedPrefsLanguagePreference
import com.glossostudio.transitos.core.provider.ProviderRegistry
import com.glossostudio.transitos.core.provider.ProviderSettingsRepository
import com.glossostudio.transitos.core.repository.FavoritesRepository
import com.glossostudio.transitos.core.repository.LanguagePreference
import com.glossostudio.transitos.core.repository.RouteFavoritesRepository
import com.glossostudio.transitos.core.repository.ThemePreference
import com.glossostudio.transitos.core.repository.TransferBufferPreference
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
    single<TransferBufferPreference> { DataStoreTransferBufferPreference(androidContext()) }
}
