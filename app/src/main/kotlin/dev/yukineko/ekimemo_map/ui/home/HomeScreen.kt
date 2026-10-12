package dev.yukineko.ekimemo_map.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.BuildConfig
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.location.NearbyStation
import dev.yukineko.ekimemo_map.location.cooldownSeconds
import dev.yukineko.ekimemo_map.ui.PermissionIssue
import dev.yukineko.ekimemo_map.ui.PermissionIssueDialog
import dev.yukineko.ekimemo_map.ui.StationCard
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.ToolAction
import dev.yukineko.ekimemo_map.ui.navigation.DevRoute
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.navigation.SettingsRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.ToolsRoute
import dev.yukineko.ekimemo_map.ui.rememberNow
import dev.yukineko.ekimemo_map.ui.rememberSearchPermissionRequester
import dev.yukineko.ekimemo_map.ui.relativeTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(onNavigate: (Any) -> Unit, viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val context = LocalContext.current
    val isRunning by LocationService.isRunning.collectAsState()
    val location by LocationService.location.collectAsState()
    val searchResult by LocationService.searchResult.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stationDataState by viewModel.stationDataState.collectAsState()
    val hasStationData = stationDataState == StationDataState.READY
    val accessLogs by viewModel.accessLogs.collectAsState()
    val stationDataUpdate by viewModel.stationDataUpdate.collectAsState()
    val now by rememberNow()
    val listState = rememberLazyListState()

    var issues by remember { mutableStateOf(emptyList<PermissionIssue>()) }
    val requestPermissions = rememberSearchPermissionRequester {
        issues = it
        if (it.none { issue -> issue.blocksSearch }) LocationService.start(context)
    }
    issues.firstOrNull()?.let { PermissionIssueDialog(it) { issues = issues.drop(1) } }

    fun cooldownOf(stationId: Int) = settings?.let { cooldownSeconds(accessLogs[stationId]?.lastAccess, it, now) } ?: 0

    var menuTarget by remember { mutableStateOf<IndexedValue<NearbyStation>?>(null) }
    menuTarget?.let { (index, item) ->
        val id = item.station.id
        val dismiss = { menuTarget = null }
        StationMenuDialog(
            item = item,
            showAccessMenu = index != 0,
            accessed = accessLogs[id]?.accessed == true,
            timerEnabled = settings?.enableReminder == true,
            timerRunning = cooldownOf(id) > 0,
            onDismiss = dismiss,
            onSetAccessed = { viewModel.setAccessed(id, it); dismiss() },
            onToggleTimer = { if (cooldownOf(id) > 0) viewModel.resetTimer(id) else viewModel.startTimer(id); dismiss() },
            onOpenRadar = { dismiss(); onNavigate(MapRoute(radarId = id)) },
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    TopAppBar(title = { Text(stringResource(R.string.title_home)) }, colors = appBarColors())
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val unknown = stringResource(R.string.unknown)
                        Column(Modifier.weight(1f)) {
                            val accuracy = location?.takeIf { it.hasAccuracy() }?.let { String.format(Locale.ROOT, "%.1fm", it.accuracy) }
                            val speed = location?.takeIf { it.hasSpeed() }?.let { String.format(Locale.ROOT, "%.1fkm/h", it.speed * 3.6) }
                            Text(stringResource(R.string.home_accuracy, accuracy ?: unknown))
                            Text(stringResource(R.string.home_speed, speed ?: unknown))
                        }
                        Text(stringResource(if (isRunning) R.string.search_on else R.string.search_off))
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isRunning,
                            enabled = hasStationData,
                            onCheckedChange = { if (it) requestPermissions() else LocationService.stop(context) },
                            thumbContent = {
                                Icon(
                                    if (isRunning) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (isRunning) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.home_last_update, relativeTime(searchResult?.updatedAt, now)), Modifier.weight(1f))
                        searchResult?.let {
                            Text("${it.processingMillis}ms", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            HorizontalFloatingToolbar(expanded = true, colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()) {
                if (BuildConfig.DEBUG) {
                    IconButton(onClick = { onNavigate(DevRoute) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.BugReport, contentDescription = stringResource(R.string.title_dev))
                    }
                }
                IconButton(onClick = { onNavigate(ToolsRoute) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Widgets, contentDescription = stringResource(R.string.title_tools))
                }
                IconButton(onClick = { onNavigate(MapRoute()) }, shapes = IconButtonDefaults.shapes(), enabled = hasStationData) {
                    Icon(Icons.Rounded.Map, contentDescription = stringResource(R.string.title_map))
                }
                IconButton(onClick = { onNavigate(SettingsRoute) }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.title_settings))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        val stations = searchResult?.stations.orEmpty()
        val stationIds = stations.map { it.station.id }
        val previousStationIds = remember { mutableStateOf(stationIds) }
        SideEffect {
            if (previousStationIds.value != stationIds) {
                previousStationIds.value = stationIds
                listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
        }
        when {
            stationDataState == StationDataState.LOADING -> EmptyMessage(padding, R.string.home_loading_station_data) { LoadingIndicator() }
            stationDataState == StationDataState.MISSING -> EmptyMessage(padding, R.string.home_no_station_data, R.string.home_no_station_data_message) {
                Button(onClick = viewModel::updateStationData, shapes = ButtonDefaults.shapes(), enabled = stationDataUpdate != StationDataUpdateState.RUNNING) {
                    if (stationDataUpdate == StationDataUpdateState.RUNNING) {
                        LoadingIndicator(Modifier.size(24.dp), color = LocalContentColor.current)
                    } else {
                        Text(stringResource(R.string.update_station_data))
                    }
                }
                if (stationDataUpdate == StationDataUpdateState.FAILED) {
                    Text(stringResource(R.string.station_data_update_failed), color = MaterialTheme.colorScheme.error)
                }
            }
            stations.isEmpty() && isRunning -> EmptyMessage(padding, R.string.home_waiting_location)
            stations.isEmpty() -> EmptyMessage(padding, R.string.home_search_off, R.string.home_search_off_message)
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(stations, key = { _, item -> item.station.id }) { index, item ->
                    StationCard(
                        station = item.station,
                        line = item.line,
                        index = index,
                        count = stations.size,
                        distance = item.distance,
                        cooldown = cooldownOf(item.station.id),
                        onClick = { onNavigate(StationDetailRoute(item.station.id)) },
                        onLongClick = { menuTarget = IndexedValue(index, item) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(
    padding: PaddingValues,
    @StringRes title: Int,
    @StringRes message: Int? = null,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(padding).padding(start = 12.dp, end = 12.dp, top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMediumEmphasized, textAlign = TextAlign.Center)
        message?.let { Text(stringResource(it), textAlign = TextAlign.Center) }
        content()
    }
}

@Composable
private fun StationMenuDialog(
    item: NearbyStation,
    showAccessMenu: Boolean,
    accessed: Boolean,
    timerEnabled: Boolean,
    timerRunning: Boolean,
    onDismiss: () -> Unit,
    onSetAccessed: (Boolean) -> Unit,
    onToggleTimer: () -> Unit,
    onOpenRadar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PinDrop, contentDescription = null) },
        title = { Text(item.station.name) },
        text = {
            Column {
                item.line?.let {
                    Text(it.name, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                }
                val actions = buildList {
                    if (showAccessMenu) {
                        if (accessed) {
                            add(ToolAction(R.string.mark_unaccessed, Icons.Rounded.Cancel) { onSetAccessed(false) })
                        } else {
                            add(ToolAction(R.string.mark_accessed, Icons.Rounded.CheckCircle) { onSetAccessed(true) })
                        }
                        if (timerEnabled) {
                            if (timerRunning) {
                                add(ToolAction(R.string.reset_timer, Icons.Rounded.TimerOff, onToggleTimer))
                            } else {
                                add(ToolAction(R.string.start_timer, Icons.Rounded.Timer, onToggleTimer))
                            }
                        }
                    }
                    add(ToolAction(R.string.show_radar, Icons.Rounded.Radar, onOpenRadar))
                }
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    actions.forEachIndexed { index, action -> MenuItem(action, index, actions.size) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun MenuItem(action: ToolAction, index: Int, count: Int) {
    SegmentedListItem(
        onClick = action.onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = { Icon(action.icon, contentDescription = null) },
    ) {
        Text(stringResource(action.text))
    }
}
