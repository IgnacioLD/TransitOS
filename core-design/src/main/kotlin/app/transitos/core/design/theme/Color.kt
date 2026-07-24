package com.glossostudio.transitos.core.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material 3 colour scheme generated from the brand seed `#006A6A` (a deep
 * transit-in-motion teal). Every role is tuned, not just Primary, so the
 * system reads as a single palette instead of "teal plus defaults".
 *
 * Reference: Material Theme Builder output for `#006A6A`, hand-adjusted for
 * calm (slightly desaturated secondaries, soft warm neutrals).
 */

// ── Light ────────────────────────────────────────────────────────────────────
private val PrimaryLight = Color(0xFF006A6A)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFF6FF7F4)
private val OnPrimaryContainerLight = Color(0xFF002020)

private val SecondaryLight = Color(0xFF4A6363)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFCCE8E7)
private val OnSecondaryContainerLight = Color(0xFF051F1F)

private val TertiaryLight = Color(0xFF4B607C)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFD3E4FF)
private val OnTertiaryContainerLight = Color(0xFF041C35)

private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

private val BackgroundLight = Color(0xFFFAFDFC)
private val OnBackgroundLight = Color(0xFF191C1C)
private val SurfaceLight = Color(0xFFFAFDFC)
private val OnSurfaceLight = Color(0xFF191C1C)
private val SurfaceVariantLight = Color(0xFFDAE5E3)
private val OnSurfaceVariantLight = Color(0xFF3F4948)
private val OutlineLight = Color(0xFF6F7978)
private val OutlineVariantLight = Color(0xFFBEC9C7)

private val InverseSurfaceLight = Color(0xFF2D3131)
private val InverseOnSurfaceLight = Color(0xFFEFF1F0)
private val InversePrimaryLight = Color(0xFF4CDADA)
private val SurfaceTintLight = Color(0xFF006A6A)

// ── Dark ─────────────────────────────────────────────────────────────────────
private val PrimaryDark = Color(0xFF4CDADA)
private val OnPrimaryDark = Color(0xFF003737)
private val PrimaryContainerDark = Color(0xFF004F4F)
private val OnPrimaryContainerDark = Color(0xFF6FF7F4)

private val SecondaryDark = Color(0xFFB0CCCB)
private val OnSecondaryDark = Color(0xFF1B3534)
private val SecondaryContainerDark = Color(0xFF324B4B)
private val OnSecondaryContainerDark = Color(0xFFCCE8E7)

private val TertiaryDark = Color(0xFFB3C8E8)
private val OnTertiaryDark = Color(0xFF1C314B)
private val TertiaryContainerDark = Color(0xFF334863)
private val OnTertiaryContainerDark = Color(0xFFD3E4FF)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

private val BackgroundDark = Color(0xFF191C1C)
private val OnBackgroundDark = Color(0xFFE0E3E2)
private val SurfaceDark = Color(0xFF191C1C)
private val OnSurfaceDark = Color(0xFFE0E3E2)
private val SurfaceVariantDark = Color(0xFF3F4948)
private val OnSurfaceVariantDark = Color(0xFFBEC9C7)
private val OutlineDark = Color(0xFF889392)
private val OutlineVariantDark = Color(0xFF3F4948)

private val InverseSurfaceDark = Color(0xFFE0E3E2)
private val InverseOnSurfaceDark = Color(0xFF2D3131)
private val InversePrimaryDark = Color(0xFF006A6A)
private val SurfaceTintDark = Color(0xFF4CDADA)

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
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceTint = SurfaceTintDark,
    scrim = ScrimColor,
)

/**
 * AMOLED variant, identical to [DarkColors] but with pure-black surfaces so
 * pixels switch off entirely, maximising battery savings on OLED panels.
 */
val AmoledColors = DarkColors.copy(
    background = Color(0xFF000000),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF1A1E1E),
    onSurfaceVariant = Color(0xFFBEC9C7),
)
