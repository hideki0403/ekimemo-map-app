package dev.yukineko.ekimemo_map.ui.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen
import dev.yukineko.ekimemo_map.ui.listItemColors
import dev.yukineko.ekimemo_map.ui.navigation.HistoryRoute
import dev.yukineko.ekimemo_map.ui.navigation.IntervalTimerRoute
import dev.yukineko.ekimemo_map.ui.navigation.MovementLogRoute
import dev.yukineko.ekimemo_map.ui.navigation.RouteSearchRoute
import dev.yukineko.ekimemo_map.ui.navigation.SearchRoute

private data class Tool(@StringRes val title: Int, @StringRes val description: Int, val icon: ImageVector, val route: Any)

private val tools = listOf(
    Tool(R.string.title_search, R.string.tools_search_description, Icons.Rounded.Search, SearchRoute),
    Tool(R.string.title_history, R.string.tools_history_description, Icons.Rounded.History, HistoryRoute),
    Tool(R.string.title_movement_log, R.string.tools_movement_log_description, Icons.Rounded.Route, MovementLogRoute),
    Tool(R.string.title_route_search, R.string.tools_route_search_description, Icons.Rounded.Directions, RouteSearchRoute),
    Tool(R.string.title_interval_timer, R.string.tools_interval_timer_description, Icons.Rounded.Timer, IntervalTimerRoute),
)

@Composable
fun ToolsScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(R.string.title_tools, onBack) {
        tools.forEachIndexed { index, tool ->
            SegmentedListItem(
                onClick = { onNavigate(tool.route) },
                shapes = ListItemDefaults.segmentedShapes(index, tools.size),
                colors = listItemColors(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = ListItemDefaults.SegmentedGap / 2),
                leadingContent = { Icon(tool.icon, contentDescription = null) },
                supportingContent = { Text(stringResource(tool.description)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(tool.title), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
