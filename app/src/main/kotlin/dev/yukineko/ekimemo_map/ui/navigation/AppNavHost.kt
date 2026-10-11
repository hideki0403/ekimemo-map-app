package dev.yukineko.ekimemo_map.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.ui.PermissionIssue
import dev.yukineko.ekimemo_map.ui.PermissionIssueDialog
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen
import dev.yukineko.ekimemo_map.ui.rememberSearchPermissionRequester

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.navigateUp() }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val context = LocalContext.current
            val isRunning by LocationService.isRunning.collectAsState()
            val location by LocationService.location.collectAsState()
            val nearbyStations by LocationService.nearbyStations.collectAsState()
            var issues by remember { mutableStateOf(emptyList<PermissionIssue>()) }
            val requestPermissions = rememberSearchPermissionRequester {
                issues = it
                if (it.none { issue -> issue.blocksSearch }) LocationService.start(context)
            }
            issues.firstOrNull()?.let { PermissionIssueDialog(it) { issues = issues.drop(1) } }
            PlaceholderScreen(R.string.title_home) {
                TextButton(onClick = { if (isRunning) LocationService.stop(context) else requestPermissions() }) {
                    Text("Search: $isRunning")
                }
                Text(location?.let { "${it.latitude}, ${it.longitude} (${it.accuracy}m)" }.toString())
                nearbyStations.forEach { Text("${it.station.name} ${it.distance}m ${it.line?.name}") }
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
