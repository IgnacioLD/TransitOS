package app.transitos

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.transitos.core.data.SharedPrefsLanguagePreference
import app.transitos.core.data.di.coreDataModule
import app.transitos.core.network.di.networkModule
import app.transitos.feature.home.di.homeModule
import app.transitos.feature.planner.di.plannerModule
import app.transitos.feature.search.di.searchModule
import app.transitos.feature.settings.di.settingsModule
import app.transitos.map.appMapModule
import app.transitos.provider.metrovalencia.di.metrovalenciaModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TransitOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val langPref = SharedPrefsLanguagePreference(this)
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(langPref.current),
            )
        }

        startKoin {
            androidLogger()
            androidContext(this@TransitOSApplication)
            modules(
                networkModule(debug = BuildConfig.DEBUG),
                coreDataModule,
                metrovalenciaModule,
                homeModule,
                searchModule,
                plannerModule,
                settingsModule,
                appMapModule,
            )
        }
    }
}
