package com.glossostudio.transitos.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Per-role corner radii, leaning into Material 3 Expressive: softer and more
 * generous than the baseline so content reads as tactile and modern.
 *
 * The jump from `medium` (rows, inputs) to `large` (cards) to `extraLarge`
 * (hero surfaces, sheets) is deliberate, it stages the hierarchy so the eye
 * can tell a container from a control.
 */
val TransitOSShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Fully rounded shape for chips, badges and status pills. */
val PillShape = RoundedCornerShape(percent = 50)

/** Signature shape for hero surfaces such as the home departure board. */
val HeroShape = RoundedCornerShape(32.dp)

/** Shape for the smaller nested panels that sit inside a hero or a card. */
val PanelShape = RoundedCornerShape(20.dp)
