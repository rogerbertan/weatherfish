package dev.rogerbertan.weatherfish.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.ui.details.DetailsScreen
import dev.rogerbertan.weatherfish.ui.favorites.FavoritesScreen
import dev.rogerbertan.weatherfish.ui.map.MapScreen
import dev.rogerbertan.weatherfish.ui.search.SearchScreen
import dev.rogerbertan.weatherfish.ui.share.ShareScreen
import dev.rogerbertan.weatherfish.ui.today.TodayScreen
import dev.rogerbertan.weatherfish.ui.week.WeekScreen
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable data object TodayGraph
@Serializable data object TodayRoute
@Serializable data object DetailsRoute
@Serializable data object FavoritesRoute
@Serializable data object WeekRoute
@Serializable data object SearchRoute
@Serializable data object MapRoute
@Serializable data object ShareRoute

enum class TopLevelDestination(val route: Any, @StringRes val label: Int, val icon: ImageVector) {
    Today(TodayGraph, R.string.nav_today, Icons.Outlined.WbSunny),
    Week(WeekRoute, R.string.nav_week, Icons.Outlined.CalendarMonth),
    Search(SearchRoute, R.string.nav_search, Icons.Outlined.Search),
    Map(MapRoute, R.string.nav_map, Icons.Outlined.Map),
    Share(ShareRoute, R.string.nav_share, Icons.Outlined.Share),
}

@Composable
fun WeatherFishApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination.isIn(destination.route::class),
                        onClick = { navController.navigateToTopLevel(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayGraph,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation<TodayGraph>(startDestination = TodayRoute) {
                composable<TodayRoute> {
                    TodayScreen(
                        onOpenFavorites = { navController.navigate(FavoritesRoute) },
                        onOpenDetails = { navController.navigate(DetailsRoute) },
                    )
                }
                composable<DetailsRoute> {
                    DetailsScreen(onBack = navController::popBackStack)
                }
                composable<FavoritesRoute> {
                    FavoritesScreen(onBack = navController::popBackStack)
                }
            }
            composable<WeekRoute> { WeekScreen() }
            composable<SearchRoute> {
                SearchScreen(
                    onPlaceSelected = {
                        navController.navigateToTopLevel(TodayGraph)
                        navController.popBackStack<TodayRoute>(inclusive = false)
                    },
                )
            }
            composable<MapRoute> { MapScreen() }
            composable<ShareRoute> { ShareScreen() }
        }
    }
}

private fun NavController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.isIn(route: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(route) } == true