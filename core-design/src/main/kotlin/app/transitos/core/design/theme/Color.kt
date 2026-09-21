package com.glossostudio.transitos.core.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * TransitOS colour system.
 *
 * The brand is a deep "tram-in-motion" teal paired with the warm amber of the
 * tram's headlights. Teal carries structure (primary, containers, surfaces),
 * amber is the energy accent reserved for live/now moments, highlights and
 * brand flourishes. Red stays semantic: alerts and errors only.
 *
 * Every Material 3 role is set explicitly, including the newer surface
 * container ladder (`surfaceContainerLowest`..`surfaceContainerHighest`,
 * `surfaceDim`, `surfaceBright`). That ladder is what gives the app its calm,
 * layered depth: sheets and cards sit on `surfaceContainer*` tones instead of
 * borrowed shadows, and AMOLED can collapse the ladder to near-black.
 *
 * Reference: Material Theme Builder output for `#00696B`, hand-tuned for a
 * slightly warmer neutral and a true amber tertiary.
 */

// ── Light ────────────────────────────────────────────────────────────────────
private val PrimaryLight = Color(0xFF00696B)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFF9CF1F1)
private val OnPrimaryContainerLight = Color(0xFF002020)

private val SecondaryLight = Color(0xFF4A6363)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFCCE8E8)
private val OnSecondaryContainerLight = Color(0xFF051F1F)

private val TertiaryLight = Color(0xFF7A5900)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFFFDF9E)
private val OnTertiaryContainerLight = Color(0xFF261A00)

private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

private val BackgroundLight = Color(0xFFF4FBFA)
private val OnBackgroundLight = Color(0xFF161D1D)
private val SurfaceLight = Color(0xFFF4FBFA)
private val OnSurfaceLight = Color(0xFF161D1D)
private val SurfaceVariantLight = Color(0xFFDAE5E4)
private val OnSurfaceVariantLight = Color(0xFF3F4949)
private val OutlineLight = Color(0xFF6F7979)
private val OutlineVariantLight = Color(0xFFBEC9C8)

private val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
private val SurfaceContainerLowLight = Color(0xFFEFF5F4)
private val SurfaceContainerLight = Color(0xFFE9EFEE)
private val SurfaceContainerHighLight = Color(0xFFE3EAE9)
private val SurfaceContainerHighestLight = Color(0xFFDDE4E3)
private val SurfaceDimLight = Color(0xFFD4DBDA)
private val SurfaceBrightLight = Color(0xFFF4FBFA)

private val InverseSurfaceLight = Color(0xFF2B3231)
private val InverseOnSurfaceLight = Color(0xFFECF2F1)
private val InversePrimaryLight = Color(0xFF80D4D5)
private val SurfaceTintLight = Color(0xFF00696B)

// ── Dark ─────────────────────────────────────────────────────────────────────
private val PrimaryDark = Color(0xFF80D4D5)
private val OnPrimaryDark = Color(0xFF003738)
private val PrimaryContainerDark = Color(0xFF004F50)
private val OnPrimaryContainerDark = Color(0xFF9CF1F1)

private val SecondaryDark = Color(0xFFB0CCCB)
private val OnSecondaryDark = Color(0xFF1B3534)
private val SecondaryContainerDark = Color(0xFF324B4A)
private val OnSecondaryContainerDark = Color(0xFFCCE8E8)

private val TertiaryDark = Color(0xFFF5C55A)
private val OnTertiaryDark = Color(0xFF3F2D00)
private val TertiaryContainerDark = Color(0xFF5C4300)
private val OnTertiaryContainerDark = Color(0xFFFFDF9E)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

private val BackgroundDark = Color(0xFF0E1514)
private val OnBackgroundDark = Color(0xFFDDE4E3)
private val SurfaceDark = Color(0xFF0E1514)
private val OnSurfaceDark = Color(0xFFDDE4E3)
private val SurfaceVariantDark = Color(0xFF3F4949)
private val OnSurfaceVariantDark = Color(0xFFBEC9C8)
private val OutlineDark = Color(0xFF899393)
private val OutlineVariantDark = Color(0xFF3F4949)

private val SurfaceContainerLowestDark = Color(0xFF090F0F)
private val SurfaceContainerLowDark = Color(0xFF161D1D)
private val SurfaceContainerDark = Color(0xFF1A2121)
private val SurfaceContainerHighDark = Color(0xFF252B2B)
private val SurfaceContainerHighestDark = Color(0xFF303636)
private val SurfaceDimDark = Color(0xFF0E1514)
private val SurfaceBrightDark = Color(0xFF343A3A)

private val InverseSurfaceDark = Color(0xFFDDE4E3)
private val InverseOnSurfaceDark = Color(0xFF2B3231)
private val InversePrimaryDark = Color(0xFF00696B)
private val SurfaceTintDark = Color(0xFF80D4D5)

val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceTint = SurfaceTintLight,
    scrim = ScrimColor,
)

val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceTint = SurfaceTintDark,
    scrim = ScrimColor,
)

/**
 * AMOLED variant: pure-black surfaces so pixels switch off entirely, with the
 * container ladder flattened to near-black tones that still read as distinct
 * layers. Kept out of the colour-scheme generator because "true black" is a
 * deliberate battery decision, not a tonal one.
 */
val AmoledColors = DarkColors.copy(
    background = Color(0xFF000000),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF1A1E1E),
    onSurfaceVariant = Color(0xFFBEC9C8),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1B1B1B),
    surfaceContainerHighest = Color(0xFF242424),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2A2A),
)

/**
 * Brand colours for signature surfaces that must look identical across light,
 * dark and AMOLED, chiefly the home departure board. The gradient stays dark
 * enough that plain white text clears WCAG AA on every stop of the ramp.
 */
object BrandColors {
    val HeroGradientTop = Color(0xFF0C6E6D)
    val HeroGradientBottom = Color(0xFF003B3B)

    /** Warm amber used on the hero for "live" and highlight accents. */
    val HeroAccent = Color(0xFFF5C55A)

    /** Foreground that stays legible on the hero gradient. */
    val OnHero = Color(0xFFFFFFFF)
    val OnHeroMuted = Color(0xCCFFFFFF)
}
