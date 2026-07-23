package app.transitos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations shown in the bottom navigation bar. Adding a tab means
 * adding an entry here plus a `composable(...)` in the NavHost.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(route = "home", label = "Inicio", icon = Icons.Outlined.Home),
    SEARCH(route = "search", label = "Buscar", icon = Icons.Outlined.Search),
    PLANNER(route = "planner", label = "Planificar", icon = Icons.AutoMirrored.Outlined.AltRoute),
    MAP(route = "map", label = "Mapa", icon = Icons.Outlined.Map),
}
