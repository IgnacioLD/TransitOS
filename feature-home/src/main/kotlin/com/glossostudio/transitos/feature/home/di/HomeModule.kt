package com.glossostudio.transitos.feature.home.di

import com.glossostudio.transitos.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
}
