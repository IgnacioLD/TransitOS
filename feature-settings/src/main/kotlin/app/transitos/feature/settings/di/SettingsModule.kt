package com.glossostudio.transitos.feature.settings.di

import com.glossostudio.transitos.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
