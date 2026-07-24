package com.glossostudio.transitos.core.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Picks black or white for foreground text on [background] so the contrast
 * stays legible regardless of the operator's brand colour. Uses Compose's
 * [Color.luminance] (rec. 709 perceptual); the 0.5 threshold matches WCAG
 * informal guidance for "which neutral reads better on this colour".
 *
 * Used by line badges, occupancy chips and anywhere we paint arbitrary
 * operator-supplied colours.
 */
fun onColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White
