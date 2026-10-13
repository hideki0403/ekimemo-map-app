package dev.yukineko.ekimemo_map.ui

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.ui.navigation.AssistantChooseRectRoute
import dev.yukineko.ekimemo_map.ui.navigation.AssistantFlowRoute
import dev.yukineko.ekimemo_map.ui.navigation.HistoryRoute
import dev.yukineko.ekimemo_map.ui.navigation.IntervalTimerRoute
import dev.yukineko.ekimemo_map.ui.navigation.LicenseRoute
import dev.yukineko.ekimemo_map.ui.navigation.LineDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.LogRoute
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.navigation.MovementLogRoute
import dev.yukineko.ekimemo_map.ui.navigation.RouteSearchRoute
import dev.yukineko.ekimemo_map.ui.navigation.SearchRoute
import dev.yukineko.ekimemo_map.ui.navigation.SettingsRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.ToolsRoute

@Composable
fun DevScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit) {
    val location by LocationService.location.collectAsState()
    val searchResult by LocationService.searchResult.collectAsState()

    PlaceholderScreen(R.string.title_dev, onBack) {
        Text(location.toString())
        Text("processing: ${searchResult?.processingMillis}ms")
        searchResult?.stations?.forEach { Text("${it.station.name} ${it.distance}m ${it.line?.name}") }
        listOf(
            SettingsRoute, AssistantFlowRoute, AssistantChooseRectRoute(point = false), LogRoute, ToolsRoute,
            SearchRoute, HistoryRoute, RouteSearchRoute, IntervalTimerRoute, MovementLogRoute,
            LicenseRoute, MapRoute(stationId = 1130101, sessionIds = listOf(1, 2)),
            StationDetailRoute(1130101), LineDetailRoute(11302),
        ).forEach { route ->
            TextButton(onClick = { onNavigate(route) }, shapes = ButtonDefaults.shapes()) { Text(route.toString()) }
        }
    }
}
