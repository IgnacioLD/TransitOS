package com.glossostudio.transitos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.glossostudio.transitos.R

/**
 * Top-level destinations shown in the bottom navigation bar. Adding a tab means
 * adding an entry here plus a `composable(...)` in the NavHost.
 *
 * Each destination carries an outlined icon for the resting state and a filled
 * one for the selected state, the standard Material 3 way of making the current
 * tab unmistakable without relying on colour alone.
 */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(
        route = "home",
        labelRes = R.string.nav_home,
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
    ),
    SEARCH(
        route = "search",
        labelRes = R.string.nav_search,
        icon = Icons.Outlined.Search,
        selectedIcon = Icons.Filled.Search,
    ),
    PLANNER(
        route = "planner",
        labelRes = R.string.nav_planner,
        icon = Icons.AutoMirrored.Outlined.AltRoute,
        selectedIcon = Icons.AutoMirrored.Filled.AltRoute,
    ),
    MAP(
        route = "map",
        labelRes = R.string.nav_map,
        icon = Icons.Outlined.Map,
        selectedIcon = Icons.Filled.Map,
    ),
}

