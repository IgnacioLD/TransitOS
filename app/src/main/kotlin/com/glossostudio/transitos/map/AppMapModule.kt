package com.glossostudio.transitos.map

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appMapModule = module {
    viewModelOf(::MapViewModel)
}
