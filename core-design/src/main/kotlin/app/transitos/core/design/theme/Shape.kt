package com.glossostudio.transitos.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Per-role corner radii. Soft and generous, which is most of how the "calm,
 * spacious" brief reads visually. The jump from `medium` (cards) to `large`
 * (sheets, hero surfaces) is deliberate: content feels touchable, overlays
 * feel weightier.
 *
 * [pill] is not part of the Material scale but is used constantly for chips,
 * status pills and line badges, so it lives here instead of being re-derived
 * with `CircleShape`/`RoundedCornerShape(50)` in every call site.
 */
val TransitOSShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Fully rounded shape for chips, badges and status pills. */
val PillShape = RoundedCornerShape(percent = 50)
