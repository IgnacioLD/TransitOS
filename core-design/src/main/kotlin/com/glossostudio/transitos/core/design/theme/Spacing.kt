package com.glossostudio.transitos.core.design.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spatial rhythm of the app. Every padding, gap and inset resolves through
 * [LocalSpacing], no hard-coded `.dp` in feature code. Two-rule ladder:
 *  - the spacing scale is small and easy to internalise (xxs..xxl);
 *  - meaningful names win over magic numbers (`Spacing.lg`, not `16.dp`).
 */
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
) {
    /** Horizontal content gutter used by every screen edge. */
    val screenGutter: Dp get() = lg
    /** Vertical space between cards in a section list. */
    val cardGap: Dp get() = md
    /** Vertical breathing room inside a card. */
    val cardPadding: Dp get() = lg
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }
