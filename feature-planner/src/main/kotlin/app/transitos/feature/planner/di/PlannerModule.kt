package com.glossostudio.transitos.feature.planner.di

import com.glossostudio.transitos.feature.planner.PlannerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val plannerModule = module {
    viewModelOf(::PlannerViewModel)
}
