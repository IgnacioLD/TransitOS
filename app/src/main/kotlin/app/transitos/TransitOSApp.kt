package com.glossostudio.transitos

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.glossostudio.transitos.feature.home.HomeRoute
import com.glossostudio.transitos.feature.planner.PlannerRoute
import com.glossostudio.transitos.feature.planner.PrefilledPlannerRoute
import com.glossostudio.transitos.feature.search.SearchRoute
import com.glossostudio.transitos.feature.settings.SettingsRoute
import com.glossostudio.transitos.map.NetworkMapRoute
import com.glossostudio.transitos.map.OSMNetworkMapRoute
import com.glossostudio.transitos.navigation.TopLevelDestination
import com.glossostudio.transitos.navigation.TransitOSBottomBar

@Composable
fun TransitOSApp() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            val currentRoute = navController
                .currentBackStackEntryAsState()
                .value
                ?.destination
                ?.route
            val isTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }
                || currentRoute?.startsWith("planner/") == true
            if (isTopLevel) {
                TransitOSBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateToTopLevelDestination(route) },
                )
            }
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
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally { it / 14 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally { it / 14 } },
    ) {
        composable(TopLevelDestination.HOME.route) {
            HomeRoute(
                onNavigateToPlanner = { originId, destId ->
                    navController.navigate("planner/$originId/$destId")
                },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToSearch = {
                    navController.navigateToTopLevelDestination(TopLevelDestination.SEARCH.route)
                },
            )
        }
        composable(TopLevelDestination.SEARCH.route) {
            SearchRoute()
        }
        composable(TopLevelDestination.PLANNER.route) {
            PlannerRoute()
        }
        composable("planner/{originId}/{destId}") { backStackEntry ->
            val originId = backStackEntry.arguments?.getString("originId") ?: return@composable
            val destId = backStackEntry.arguments?.getString("destId") ?: return@composable
            PrefilledPlannerRoute(
                originStopId = originId,
                destinationStopId = destId,
            )
        }
        composable(TopLevelDestination.MAP.route) {
            OSMNetworkMapRoute(
                onBack = { navController.popBackStack() },
                onOpenPdf = { navController.navigate("map/pdf") },
            )
        }
        composable("map/pdf") {
            NetworkMapRoute(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsRoute(
                onBack = { navController.popBackStack() },
            )
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
