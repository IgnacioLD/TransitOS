package com.glossostudio.transitos

import com.glossostudio.transitos.core.review.AppReviewer
import com.glossostudio.transitos.review.ActivityProvider
import com.glossostudio.transitos.review.PlayAppReviewer
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { ActivityProvider() }
    single<AppReviewer> { PlayAppReviewer(androidContext(), get()) }
    viewModelOf(::AppViewModel)
}
