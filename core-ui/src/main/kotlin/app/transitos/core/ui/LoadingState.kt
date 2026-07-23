package app.transitos.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Centred skeleton screen — used for the very first paint of a screen before
 * any data is available. Calmer than a spinner, and the layout doesn't jump
 * when real content arrives because the placeholder is content-shaped.
 *
 * For the Home screen specifically, prefer [FavoritesSkeleton]. This
 * composable is the generic fallback for screens that don't yet have a
 * tailored skeleton.
 */
@Composable
fun LoadingState(
    label: String,
    modifier: Modifier = Modifier,
) {
    // Centred label + shimmer; kept simple so any screen can use it without
    // knowing the eventual content shape.
    FavoritesSkeleton(modifier = modifier)
}
