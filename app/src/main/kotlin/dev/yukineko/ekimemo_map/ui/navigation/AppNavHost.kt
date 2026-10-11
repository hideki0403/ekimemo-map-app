package dev.yukineko.ekimemo_map.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.DevScreen
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen
import dev.yukineko.ekimemo_map.ui.home.HomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.navigateUp() }
    val navigate: (Any) -> Unit = { navController.navigate(it) }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen(onNavigate = navigate) }
        composable<DevRoute> { DevScreen(onNavigate = navigate, onBack = back) }
        composable<SettingsRoute> { PlaceholderScreen(R.string.title_settings, back) }
        composable<AssistantFlowRoute> { PlaceholderScreen(R.string.title_assistant_flow, back) }
        composable<AssistantChooseRectRoute> { PlaceholderScreen(R.string.title_assistant_choose_rect, back) }
        composable<LogRoute> { PlaceholderScreen(R.string.title_log, back) }
        composable<ToolsRoute> { PlaceholderScreen(R.string.title_tools, back) }
        composable<SearchRoute> { PlaceholderScreen(R.string.title_search, back) }
        composable<HistoryRoute> { PlaceholderScreen(R.string.title_history, back) }
        composable<RouteSearchRoute> { PlaceholderScreen(R.string.title_route_search, back) }
        composable<IntervalTimerRoute> { PlaceholderScreen(R.string.title_interval_timer, back) }
        composable<MovementLogRoute> { PlaceholderScreen(R.string.title_movement_log, back) }
        composable<LicenseRoute> { PlaceholderScreen(R.string.title_license, back) }
        composable<MapRoute> {
            val route = it.toRoute<MapRoute>()
            PlaceholderScreen(R.string.title_map, back) { Text(route.toString()) }
        }
        composable<StationDetailRoute> {
            val route = it.toRoute<StationDetailRoute>()
            PlaceholderScreen(R.string.title_station_detail, back) { Text(route.toString()) }
        }
        composable<LineDetailRoute> {
            val route = it.toRoute<LineDetailRoute>()
            PlaceholderScreen(R.string.title_line_detail, back) { Text(route.toString()) }
        }
    }
}
