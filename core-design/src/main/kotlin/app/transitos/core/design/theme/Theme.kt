package app.transitos.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Root theme of the app. Honours Android 12+ dynamic colour when available so
 * TransitOS feels native on every device, and falls back to the brand teal
 * palette elsewhere.
 *
 * Custom tokens ([Spacing], [Elevation]) are provided via CompositionLocals so
 * feature code resolves them through `LocalSpacing.current` / `LocalElevation.current`
 * instead of carrying `.dp` literals.
 */
@Composable
fun TransitOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
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
