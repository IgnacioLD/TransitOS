package com.glossostudio.transitos.core.design.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tonal elevation steps for surfaces. Material 3 in dark mode uses tonal
 * elevation (a colour shift) more than shadow elevation; in light mode we lean
 * on gentle shadow. Keep this small — the design brief is "calm", not "deep".
 */
data class Elevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

val LocalElevation = staticCompositionLocalOf { Elevation() }
