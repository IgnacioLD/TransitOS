package com.glossostudio.transitos

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.TransitOSTheme
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.repository.ThemePreference
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : AppCompatActivity() {

    private val themePreference: ThemePreference by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the system bar style before the first frame so the very first
        // launch is already edge-to-edge with correct icon contrast.
        applySystemBarStyle(themePreference.current())
        super.onCreate(savedInstanceState)

        setContent {
            // A single collection of the theme preference drives both the
            // palette and the system bar icons, so the flow is never collected
            // twice.
            val themeMode by themePreference.flow.collectAsStateWithLifecycle(
                initialValue = themePreference.current(),
            )
            LaunchedEffect(themeMode) { applySystemBarStyle(themeMode) }
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
