package app.transitos.feature.planner.di

import app.transitos.feature.planner.PlannerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val plannerModule = module {
    viewModelOf(::PlannerViewModel)
}
