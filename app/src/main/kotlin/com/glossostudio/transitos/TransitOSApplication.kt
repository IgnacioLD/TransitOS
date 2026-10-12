package com.glossostudio.transitos

import android.app.Application
import com.glossostudio.transitos.core.data.di.coreDataModule
import com.glossostudio.transitos.core.network.di.networkModule
import com.glossostudio.transitos.feature.home.di.homeModule
import com.glossostudio.transitos.feature.onboarding.di.onboardingModule
import com.glossostudio.transitos.feature.planner.di.plannerModule
import com.glossostudio.transitos.feature.search.di.searchModule
import com.glossostudio.transitos.feature.settings.di.settingsModule
import com.glossostudio.transitos.map.appMapModule
import com.glossostudio.transitos.provider.server.di.providerServerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TransitOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@TransitOSApplication)
            modules(
                networkModule(debug = BuildConfig.DEBUG),
                coreDataModule,
                providerServerModule(BuildConfig.SERVER_BASE_URL),
                homeModule,
                searchModule,
                plannerModule,
                settingsModule,
                onboardingModule,
                appMapModule,
                appModule,
            )
        }
    }
}
