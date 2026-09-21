package com.glossostudio.transitos.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The TransitOS brand mark: the real launcher artwork (teal gradient, tram with
 * amber headlights) rendered as a rounded tile.
 *
 * It is the same source SVG as the launcher icon, so in-app branding can never
 * drift from the icon on the home screen. Decorative by default: callers pair
 * it with the visible "TransitOS" wordmark or an accessibility label.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = 12.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_brand_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
    )
}
