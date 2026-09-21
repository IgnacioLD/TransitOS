package com.glossostudio.transitos.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * Opacity tokens for surface layering. Centralised so the app's "calm" feel
 * stays consistent (no 0.08f / 0.083f drift across files). Only the tokens
 * the app actually uses are declared here.
 */
object Alpha {
    /** Skeleton shimmer floor; the sweep oscillates up from this. */
    const val SKELETON = 0.08f

    /** Skeleton shimmer peak. */
    const val SKELETON_HIGH = 0.16f
}

/** Material 3 scrim colour used behind modal sheets and dialogs. */
val ScrimColor = Color(0xFF000000)
