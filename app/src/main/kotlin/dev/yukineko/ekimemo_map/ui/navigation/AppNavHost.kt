package dev.yukineko.ekimemo_map.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.navigateUp() }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            PlaceholderScreen(R.string.title_home) {
                listOf(
                    SettingsRoute, AssistantFlowRoute, AssistantChooseRectRoute, LogRoute, ToolsRoute,
                    SearchRoute, HistoryRoute, RouteSearchRoute, IntervalTimerRoute, MovementLogRoute,
                    LicenseRoute, MapRoute(stationId = 1130101, sessionIds = listOf(1, 2)),
                    StationDetailRoute(1130101), LineDetailRoute(11302),
                ).forEach { route ->
                    TextButton(onClick = { navController.navigate(route) }) { Text(route.toString()) }
                }
            }
        }
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
