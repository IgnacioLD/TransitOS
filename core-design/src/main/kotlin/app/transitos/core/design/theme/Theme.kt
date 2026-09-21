package com.glossostudio.transitos.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.glossostudio.transitos.core.repository.ThemeMode

/**
 * Root theme of the app.
 *
 * By default the app uses the TransitOS brand palette so it looks the same on
 * every device. Pass [dynamicColor] = true to opt into Android 12+ wallpaper
 * colour instead. AMOLED always forces the pure-black palette and never uses
 * dynamic colour, because "true black" is the whole point of that mode.
 *
 * Pass [themeMode] to honour the user preference (SYSTEM / LIGHT / DARK / AMOLED).
 *
 * Custom tokens ([Spacing], [Elevation]) are provided via CompositionLocals so
 * feature code resolves them through `LocalSpacing.current` / `LocalElevation.current`
 * instead of carrying `.dp` literals.
 */
@Composable
fun TransitOSTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColors

        ThemeMode.LIGHT -> {
            if (dynamicColor && supportsDynamic) {
                dynamicLightColorScheme(LocalContext.current)
            } else {
                LightColors
            }
        }

        ThemeMode.DARK -> {
            if (dynamicColor && supportsDynamic) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                DarkColors
            }
        }

        ThemeMode.SYSTEM -> {
            val dark = isSystemInDarkTheme()
            when {
                dynamicColor && supportsDynamic -> {
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

/** True when [themeMode] resolves to a dark palette (DARK or AMOLED). */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.DARK, ThemeMode.AMOLED -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}
