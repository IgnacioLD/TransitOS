package com.glossostudio.transitos.core.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.glossostudio.transitos.core.repository.LanguagePreference

/**
 * The app language is the per-app locale the system stores (Android 13+
 * per-app language settings, AppCompat's own storage below that). That is the
 * single source of truth: the language Settings shows is the one the UI uses.
 *
 * With no explicit choice the app follows the device: the first device
 * language TransitOS ships, else Spanish (the default `values/`).
 */
class AppLocaleLanguagePreference(
    private val context: Context,
) : LanguagePreference {

    override val current: String
        get() = resolve(appLocales().takeUnless { it.isEmpty } ?: LocaleListCompat.getAdjustedDefault())

    /** Must be called with an activity alive: AppCompat applies it through one. */
    override fun set(language: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    /**
     * Earlier versions kept the choice in shared preferences and re-applied it
     * from the Application, which silently did nothing on Android 13+. Apply a
     * stored choice once (from an activity) and drop the legacy key.
     */
    fun migrateLegacyChoice() {
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val legacy = prefs.getString(LEGACY_KEY, null) ?: return
        prefs.edit { remove(LEGACY_KEY) }
        if (appLocales().isEmpty && legacy in SUPPORTED) set(legacy)
    }

    private fun appLocales(): LocaleListCompat {
        val fromAppCompat = AppCompatDelegate.getApplicationLocales()
        if (!fromAppCompat.isEmpty || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return fromAppCompat
        // Before any activity exists AppCompat cannot reach the framework store.
        val framework = context.getSystemService(LocaleManager::class.java)?.applicationLocales
        return framework?.let { LocaleListCompat.wrap(it) } ?: fromAppCompat
    }

    private fun resolve(locales: LocaleListCompat): String {
        for (index in 0 until locales.size()) {
            val language = locales[index]?.language ?: continue
            if (language in SUPPORTED) return language
        }
        return DEFAULT_LANGUAGE
    }

    private companion object {
        val SUPPORTED = setOf("es", "ca", "en")
        const val DEFAULT_LANGUAGE = "es"
        const val LEGACY_PREFS = "transitos_prefs"
        const val LEGACY_KEY = "app_language"
    }
}
