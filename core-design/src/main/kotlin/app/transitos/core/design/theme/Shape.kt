package com.glossostudio.transitos.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Per-role corner radii. Generous and consistent — the design brief calls for
 * "calm, spacious", and softer corners are most of how that reads visually.
 *
 * The jump from `medium` to `large` is deliberate: cards (medium) feel
 * touchable; sheets and dialogs (large) feel weightier.
 */
val TransitOSShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
