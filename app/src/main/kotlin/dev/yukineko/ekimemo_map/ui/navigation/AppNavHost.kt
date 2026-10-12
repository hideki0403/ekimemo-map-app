package dev.yukineko.ekimemo_map.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.DevScreen
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen
import dev.yukineko.ekimemo_map.ui.history.HistoryScreen
import dev.yukineko.ekimemo_map.ui.home.HomeScreen
import dev.yukineko.ekimemo_map.ui.line.LineDetailScreen
import dev.yukineko.ekimemo_map.ui.map.MapScreen
import dev.yukineko.ekimemo_map.ui.search.SearchScreen
import dev.yukineko.ekimemo_map.ui.station.StationDetailScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.navigateUp() }
    val navigate: (Any) -> Unit = { navController.navigate(it) }

    val motion = MaterialTheme.motionScheme
    val slide = motion.defaultSpatialSpec<IntOffset>()
    val fade = motion.defaultEffectsSpec<Float>()
    val offset = { width: Int -> width / 10 }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { slideInHorizontally(slide) { offset(it) } + fadeIn(fade) },
        exitTransition = { slideOutHorizontally(slide) { -offset(it) } + fadeOut(fade) },
        popEnterTransition = { slideInHorizontally(slide) { -offset(it) } + fadeIn(fade) },
        popExitTransition = { slideOutHorizontally(slide) { offset(it) } + fadeOut(fade) },
    ) {
        composable<HomeRoute> { HomeScreen(onNavigate = navigate) }
        composable<DevRoute> { DevScreen(onNavigate = navigate, onBack = back) }
        composable<SettingsRoute> { PlaceholderScreen(R.string.title_settings, back) }
        composable<AssistantFlowRoute> { PlaceholderScreen(R.string.title_assistant_flow, back) }
        composable<AssistantChooseRectRoute> { PlaceholderScreen(R.string.title_assistant_choose_rect, back) }
        composable<LogRoute> { PlaceholderScreen(R.string.title_log, back) }
        composable<ToolsRoute> { PlaceholderScreen(R.string.title_tools, back) }
        composable<SearchRoute> { SearchScreen(onNavigate = navigate, onBack = back) }
        composable<HistoryRoute> { HistoryScreen(onNavigate = navigate, onBack = back) }
        composable<RouteSearchRoute> { PlaceholderScreen(R.string.title_route_search, back) }
        composable<IntervalTimerRoute> { PlaceholderScreen(R.string.title_interval_timer, back) }
        composable<MovementLogRoute> { PlaceholderScreen(R.string.title_movement_log, back) }
        composable<LicenseRoute> { PlaceholderScreen(R.string.title_license, back) }
        composable<MapRoute> { MapScreen(onBack = back) }
        composable<StationDetailRoute> {
            StationDetailScreen(it.toRoute<StationDetailRoute>().stationId, onNavigate = navigate, onBack = back)
        }
        composable<LineDetailRoute> {
            LineDetailScreen(it.toRoute<LineDetailRoute>().lineId, onNavigate = navigate, onBack = back)
        }
    }
}
