package com.glossostudio.transitos.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * TransitOS type scale.
 *
 * A full Material 3 scale, retuned for a transit-utility voice: titles are a
 * touch heavier and pull their letter-spacing in, body copy stays airy and
 * legible, labels are wide and quiet. The result reads as "calm but confident"
 * rather than the default Material density.
 *
 * Numbers that move (arrival countdowns, timestamps) opt into tabular figures
 * via [tabularFigures] so they never jitter as digits change width.
 *
 * The family is the platform default (Roboto on Android). Swapping in a brand
 * face is a one-line change to [Family].
 */
private val Family = FontFamily.SansSerif

/** Turns on tabular (monospaced) figures so live counters stop jittering. */
const val TabularFigures = "tnum"

private val baseline = TextStyle(fontFamily = Family)

val TransitOSTypography = Typography(
    displayLarge = baseline.copy(
        fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-1.0).sp,
    ),
    displayMedium = baseline.copy(
        fontWeight = FontWeight.Medium, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp,
    ),
    displaySmall = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp,
    ),
    headlineLarge = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp,
    ),
    headlineMedium = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp,
    ),
    headlineSmall = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp,
    ),
    titleLarge = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
    ),
    titleMedium = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.sp,
    ),
    titleSmall = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = baseline.copy(
        fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    bodyMedium = baseline.copy(
        fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = baseline.copy(
        fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.35.sp,
    ),
    labelLarge = baseline.copy(
        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = baseline.copy(
        fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelSmall = baseline.copy(
        fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
