package com.glossostudio.transitos.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Localised direction (headsign) for an arrival, for example "Hacia Betera".
 *
 * A departure row must always state where the train is going, so a missing
 * headsign falls back to a clear, localised "no destination" label instead of
 * rendering an empty or line-only label.
 */
@Composable
internal fun arrivalDirection(destination: String): String = directionText(
    destination = destination,
    blankLabel = stringResource(R.string.arrival_no_destination),
    template = stringResource(R.string.board_direction),
)

/**
 * Pure formatting behind [arrivalDirection], kept separate so the blank
 * fallback can be unit tested without a Compose runtime.
 */
internal fun directionText(destination: String, blankLabel: String, template: String): String =
    if (destination.isBlank()) blankLabel else template.format(destination)
