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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.glossostudio.transitos.feature.home.HomeRoute
import com.glossostudio.transitos.feature.onboarding.OnboardingRoute
import com.glossostudio.transitos.feature.planner.PlannerRoute
import com.glossostudio.transitos.feature.planner.PrefilledPlannerRoute
import com.glossostudio.transitos.feature.search.SearchRoute
import com.glossostudio.transitos.feature.settings.LicenseRoute
import com.glossostudio.transitos.feature.settings.PrivacyPolicyRoute
import com.glossostudio.transitos.feature.settings.SettingsRoute
import com.glossostudio.transitos.map.NetworkMapRoute
import com.glossostudio.transitos.map.OSMNetworkMapRoute
import com.glossostudio.transitos.navigation.TopLevelDestination
import com.glossostudio.transitos.navigation.TransitOSBottomBar
import org.koin.androidx.compose.koinViewModel

private const val ONBOARDING_ROUTE = "onboarding"
private const val SETTINGS_ROUTE = "settings"
private const val PRIVACY_ROUTE = "settings/privacy"
private const val LICENSE_ROUTE = "settings/license"

@Composable
fun TransitOSApp(
    appViewModel: AppViewModel = koinViewModel(),
) {
    val onboardingCompleted by appViewModel.onboardingCompleted.collectAsStateWithLifecycle()
    // Frozen at first composition: the first launch opens the tour, later
    // launches open the home. Replays are a navigation from Settings.
    val startDestination = remember {
        if (onboardingCompleted) TopLevelDestination.HOME.route else ONBOARDING_ROUTE
    }
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
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun TransitOSNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally { it / 14 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally { it / 14 } },
    ) {
        composable(ONBOARDING_ROUTE) {
            OnboardingRoute(
                onFinish = {
                    // First launch: the tour is the start destination, so there
                    // is nothing to return to and we open the home. Replay from
                    // Settings: pop back to the screen that launched it.
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(TopLevelDestination.HOME.route) {
                            popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(TopLevelDestination.HOME.route) {
            HomeRoute(
                onNavigateToPlanner = { originId, destId ->
                    navController.navigate("planner/$originId/$destId")
                },
                onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) },
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
        composable(SETTINGS_ROUTE) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onReplayOnboarding = { navController.navigate(ONBOARDING_ROUTE) },
                onOpenPrivacyPolicy = { navController.navigate(PRIVACY_ROUTE) },
                onOpenLicense = { navController.navigate(LICENSE_ROUTE) },
            )
        }
        composable(PRIVACY_ROUTE) {
            PrivacyPolicyRoute(onBack = { navController.popBackStack() })
        }
        composable(LICENSE_ROUTE) {
            LicenseRoute(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateToTopLevelDestination(route: String) {
    // The first launch starts on the onboarding route, so the graph's start
    // destination is not always Home. Home is the real tab root once the tour is
    // done, so pop back to it explicitly.
    navigate(route) {
        popUpTo(TopLevelDestination.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
