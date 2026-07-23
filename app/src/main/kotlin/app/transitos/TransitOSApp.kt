package app.transitos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.transitos.feature.home.HomeRoute
import app.transitos.feature.planner.PlannerRoute
import app.transitos.feature.search.SearchRoute
import app.transitos.feature.settings.SettingsRoute
import app.transitos.navigation.TopLevelDestination
import app.transitos.navigation.TransitOSBottomBar

@Composable
fun TransitOSApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val currentRoute = navController
                .currentBackStackEntryAsState()
                .value
                ?.destination
                ?.route
            TransitOSBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTopLevelDestination(route) },
            )
        },
    ) { padding ->
        TransitOSNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun TransitOSNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeRoute()
        }
        composable(TopLevelDestination.SEARCH.route) {
            SearchRoute()
        }
        composable(TopLevelDestination.PLANNER.route) {
            PlannerRoute()
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsRoute()
        }
    }
}

private fun NavHostController.navigateToTopLevelDestination(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
