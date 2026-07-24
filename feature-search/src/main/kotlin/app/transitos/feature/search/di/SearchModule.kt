package com.glossostudio.transitos.feature.search.di

import com.glossostudio.transitos.feature.search.SearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchModule = module {
    viewModelOf(::SearchViewModel)
}
