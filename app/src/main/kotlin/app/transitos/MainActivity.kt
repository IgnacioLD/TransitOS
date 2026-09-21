package com.glossostudio.transitos

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.glossostudio.transitos.core.design.theme.TransitOSTheme
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.repository.ThemePreference
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : AppCompatActivity() {

    private val themePreference: ThemePreference by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the system bar style before the first frame, then keep it in
        // sync with the user's theme so status/navigation icons stay legible
        // against whichever palette is selected, regardless of system dark mode.
        applySystemBarStyle(themePreference.current())
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                themePreference.flow.collect(::applySystemBarStyle)
            }
        }

        setContent {
            val themeMode by themePreference.flow.collectAsStateWithLifecycle(
                initialValue = themePreference.current(),
            )
            TransitOSTheme(themeMode = themeMode) {
                KoinAndroidContext {
                    TransitOSApp()
                }
            }
        }
    }

    private fun applySystemBarStyle(mode: ThemeMode) {
        val isDark = mode.resolvesToDark()
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle(isDark),
            navigationBarStyle = systemBarStyle(isDark),
        )
    }

    private fun ThemeMode.resolvesToDark(): Boolean = when (this) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM ->
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun systemBarStyle(isDark: Boolean): SystemBarStyle = if (isDark) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }
}
