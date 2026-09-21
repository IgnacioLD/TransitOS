package com.glossostudio.transitos.core.design.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared motion vocabulary, so the app feels alive but never twitchy.
 *
 * Springs are reserved for things that should feel physical (the departure
 * counter, selection, expanding panels); tweens for fades and colour changes.
 * Keeping the specs here means every screen moves at the same tempo.
 */
object Motion {
    /** Micro feedback: ripples, icon swaps. */
    const val FAST = 140

    /** Default enter/exit, sheet content, chips. */
    const val MEDIUM = 260

    /** Bigger choreography: hero, timeline reveal. */
    const val SLOW = 420

    /** Default easing, decelerates into place. */
    val Expressive: Easing = FastOutSlowInEasing

    /** For continuous sweeps such as the skeleton shimmer. */
    val Linear: Easing = LinearEasing

    /** Bouncy but controlled, selection and counters. */
    fun <T> springy(): AnimationSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Quick, minimal overshoot, expanding panels and reveals. */
    fun <T> snappy(): AnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium,
    )

    /** No overshoot, colour and alpha transitions. */
    fun <T> gentle(): AnimationSpec<T> = spring(
        dampingRatio = 1f,
        stiffness = Spring.StiffnessLow,
    )

    /** Simple tween helper wired to the shared easing curve. */
    fun <T> standard(durationMillis: Int = MEDIUM): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = Expressive)
}
