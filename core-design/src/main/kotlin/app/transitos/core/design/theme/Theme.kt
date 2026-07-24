package app.transitos.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import app.transitos.core.repository.ThemeMode

/**
 * Root theme of the app. Honours Android 12+ dynamic colour when available so
 * TransitOS feels native on every device, and falls back to the brand teal
 * palette elsewhere.
 *
 * Pass [themeMode] to respect a user preference (SYSTEM / LIGHT / DARK / AMOLED).
 * AMOLED forces pure-black surfaces and skips dynamic colour so pixels switch off.
 *
 * Custom tokens ([Spacing], [Elevation]) are provided via CompositionLocals so
 * feature code resolves them through `LocalSpacing.current` / `LocalElevation.current`
 * instead of carrying `.dp` literals.
 */
@Composable
fun TransitOSTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColors

        ThemeMode.LIGHT -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicLightColorScheme(LocalContext.current)
            } else {
                LightColors
            }
        }

        ThemeMode.DARK -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                DarkColors
            }
        }

        ThemeMode.SYSTEM -> {
            val dark = isSystemInDarkTheme()
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (dark) dynamicDarkColorScheme(LocalContext.current)
                    else dynamicLightColorScheme(LocalContext.current)
                }
                dark -> DarkColors
                else -> LightColors
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalElevation provides Elevation(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TransitOSTypography,
            shapes = TransitOSShapes,
            content = content,
        )
    }
}
