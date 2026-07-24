package com.glossostudio.transitos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.glossostudio.transitos.R

/**
 * Top-level destinations shown in the bottom navigation bar. Adding a tab means
 * adding an entry here plus a `composable(...)` in the NavHost.
 */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(route = "home", labelRes = R.string.nav_home, icon = Icons.Outlined.Home),
    SEARCH(route = "search", labelRes = R.string.nav_search, icon = Icons.Outlined.Search),
    PLANNER(route = "planner", labelRes = R.string.nav_planner, icon = Icons.AutoMirrored.Outlined.AltRoute),
    MAP(route = "map", labelRes = R.string.nav_map, icon = Icons.Outlined.Map),
}
