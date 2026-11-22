package dev.rogerbertan.weatherfish.ui

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navOptions
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.ui.details.DetailsScreen
import dev.rogerbertan.weatherfish.ui.favorites.FavoritesScreen
import dev.rogerbertan.weatherfish.ui.map.MapScreen
import dev.rogerbertan.weatherfish.ui.search.SearchScreen
import dev.rogerbertan.weatherfish.ui.share.ImportPlaceDialog
import dev.rogerbertan.weatherfish.ui.share.ShareScreen
import dev.rogerbertan.weatherfish.ui.today.TodayScreen
import dev.rogerbertan.weatherfish.ui.week.WeekScreen
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable data object TodayGraph
@Serializable data object TodayRoute
@Serializable data object DetailsRoute
@Serializable data object FavoritesRoute
@Serializable data object WeekRoute
@Serializable data object SearchRoute
@Serializable data object MapRoute
@Serializable data object ShareRoute
@Serializable data class ImportPlaceRoute(val name: String = "", val lat: String = "", val lon: String = "")

enum class TopLevelDestination(
    val route: KClass<*>,
    val link: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Today(TodayGraph::class, DeepLinks.TODAY, R.string.nav_today, Icons.Outlined.WbSunny),
    Week(WeekRoute::class, DeepLinks.WEEK, R.string.nav_week, Icons.Outlined.CalendarMonth),
    Search(SearchRoute::class, DeepLinks.SEARCH, R.string.nav_search, Icons.Outlined.Search),
    Map(MapRoute::class, DeepLinks.MAP, R.string.nav_map, Icons.Outlined.Map),
    Share(ShareRoute::class, DeepLinks.SHARE, R.string.nav_share, Icons.Outlined.Share),
}

@Composable
fun WeatherFishApp(newIntents: Flow<Intent>, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val tabDestination = if (currentDestination is FloatingWindow) {
        navController.previousBackStackEntry?.destination
    } else {
        currentDestination
    }
    LaunchedEffect(navController) {
        newIntents.collect { intent -> intent.dataString?.let(navController::openLink) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = tabDestination.isIn(destination.route),
                        onClick = { navController.navigateToTopLevel(destination.link) },
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
            navigation<TodayGraph>(startDestination = TodayRoute, deepLinks = linkTo<TodayGraph>(DeepLinks.TODAY)) {
                composable<TodayRoute> {
                    TodayScreen(
                        onOpenFavorites = { navController.navigateTo(DeepLinks.FAVORITES) },
                        onOpenDetails = { navController.navigateTo(DeepLinks.DETAILS) },
                    )
                }
                composable<DetailsRoute>(deepLinks = linkTo<DetailsRoute>(DeepLinks.DETAILS)) {
                    DetailsScreen(onBack = navController::navigateUp)
                }
                composable<FavoritesRoute>(deepLinks = linkTo<FavoritesRoute>(DeepLinks.FAVORITES)) {
                    FavoritesScreen(onBack = navController::navigateUp)
                }
            }
            composable<WeekRoute>(deepLinks = linkTo<WeekRoute>(DeepLinks.WEEK)) { WeekScreen() }
            composable<SearchRoute>(deepLinks = linkTo<SearchRoute>(DeepLinks.SEARCH)) {
                SearchScreen(
                    onPlaceSelected = {
                        navController.navigateToTopLevel(DeepLinks.TODAY)
                        navController.popBackStack<TodayRoute>(inclusive = false)
                    },
                )
            }
            composable<MapRoute>(deepLinks = linkTo<MapRoute>(DeepLinks.MAP)) { MapScreen() }
            composable<ShareRoute>(deepLinks = linkTo<ShareRoute>(DeepLinks.SHARE)) {
                ShareScreen(onOpenLink = navController::navigateTo)
            }
            dialog<ImportPlaceRoute>(
                deepLinks = linkTo<ImportPlaceRoute>(DeepLinks.PLACE),
                dialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            ) {
                ImportPlaceDialog(onDismiss = navController::popBackStack)
            }
        }
    }
}

private inline fun <reified T : Any> linkTo(basePath: String) = listOf(navDeepLink<T>(basePath = basePath))

private fun NavController.openLink(link: String) {
    when {
        TopLevelDestination.entries.any { it.link == link } -> navigateToTopLevel(link)
        graph.hasDeepLink(link.toUri()) -> navigateTo(link)
    }
}

private fun NavController.navigateTo(link: String, navOptions: NavOptions? = null) {
    navigate(NavDeepLinkRequest.Builder.fromUri(link.toUri()).build(), navOptions)
}

private fun NavController.navigateToTopLevel(link: String) {
    navigateTo(
        link,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        },
    )
}

private fun NavDestination?.isIn(route: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(route) } == true