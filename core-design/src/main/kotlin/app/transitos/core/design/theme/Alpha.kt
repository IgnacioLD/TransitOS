package app.transitos.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * Opacity tokens for surface layering, disabled affordances, scrims and the
 * realtime "live" pulse. Centralised so the app's "calm" feel stays consistent
 * (no 0.43f / 0.45f drift across files).
 */
object Alpha {
    /** Disabled controls, secondary affordances. */
    const val DISABLED = 0.38f

    /** Medium-emphasis surfaces (cards over tinted backgrounds). */
    const val MEDIUM = 0.60f

    /** High-emphasis overlays (selected states). */
    const val HIGH = 0.88f

    /** Bottom-sheet scrim behind content. */
    const val SCRIM = 0.56f

    /** Skeleton shimmer baseline; shimmer animation oscillates around this. */
    const val SKELETON = 0.12f

    /** Subtle hint of presence — used for non-interactive decorative text. */
    const val HINT = 0.72f
}

/** Material 3 scrim colour used behind modal sheets and dialogs. */
val ScrimColor = Color(0xFF000000)
