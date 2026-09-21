package com.glossostudio.transitos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glossostudio.transitos.core.design.theme.TransitOSTheme
import com.glossostudio.transitos.core.design.theme.isDark
import com.glossostudio.transitos.core.repository.ThemeMode
import com.glossostudio.transitos.core.repository.ThemePreference
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : AppCompatActivity() {

    private val themePreference: ThemePreference by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by themePreference.flow.collectAsStateWithLifecycle(
                initialValue = themePreference.current(),
            )
            // Keep the status/navigation bar icons legible against whichever
            // palette the user picked, regardless of the system dark setting.
            val isDark = themeMode.isDark()
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle(isDark),
                    navigationBarStyle = systemBarStyle(isDark),
                )
            }
            TransitOSTheme(themeMode = themeMode) {
                KoinAndroidContext {
                    TransitOSApp()
                }
            }
        }
    }

    private fun systemBarStyle(isDark: Boolean): SystemBarStyle = if (isDark) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }
}
