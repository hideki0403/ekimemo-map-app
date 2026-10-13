package dev.yukineko.ekimemo_map.ui.home

import android.location.Location
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.BuildConfig
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.location.NearbyStation
import dev.yukineko.ekimemo_map.location.SearchResult
import dev.yukineko.ekimemo_map.location.cooldownSeconds
import dev.yukineko.ekimemo_map.ui.update.UpdateDialog
import dev.yukineko.ekimemo_map.ui.update.UpdateTarget
import dev.yukineko.ekimemo_map.ui.update.UpdateViewModel
import dev.yukineko.ekimemo_map.ui.IconSwitch
import dev.yukineko.ekimemo_map.ui.PermissionIssue
import dev.yukineko.ekimemo_map.ui.PermissionIssueDialog
import dev.yukineko.ekimemo_map.ui.StationCard
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.ToolAction
import dev.yukineko.ekimemo_map.ui.ToolActionItem
import dev.yukineko.ekimemo_map.ui.navigation.DevRoute
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.navigation.SettingsRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import dev.yukineko.ekimemo_map.ui.navigation.ToolsRoute
import dev.yukineko.ekimemo_map.ui.rememberNow
import dev.yukineko.ekimemo_map.ui.rememberSearchPermissionRequester
import dev.yukineko.ekimemo_map.ui.relativeTime
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onNavigate: (Any) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    updateViewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory),
) {
    val context = LocalContext.current
    val isRunning by LocationService.isRunning.collectAsState()
    val location by LocationService.location.collectAsState()
    val searchResult by LocationService.searchResult.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stationDataState by viewModel.stationDataState.collectAsState()
    val hasStationData = stationDataState == StationDataState.READY
    val accessLogs by viewModel.accessLogs.collectAsState()
    val appUpdate by viewModel.appUpdate.collectAsState()
    val stationDataAvailable by viewModel.stationDataAvailable.collectAsState()
    val now by rememberNow()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var issues by remember { mutableStateOf(emptyList<PermissionIssue>()) }
    val requestPermissions = rememberSearchPermissionRequester {
        issues = it
        if (it.none { issue -> issue.blocksSearch }) LocationService.start(context)
    }
    issues.firstOrNull()?.let { PermissionIssueDialog(it) { issues = issues.drop(1) } }

    UpdateDialog(updateViewModel)
    viewModel.legacyDatabaseDump.collectAsState().value?.let { path ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLegacyDatabaseDump,
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(30.dp)) },
            title = { Text(stringResource(R.string.legacy_database_discarded)) },
            text = { Text(stringResource(R.string.legacy_database_discarded_message, path)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissLegacyDatabaseDump, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) }
            },
        )
    }
    var checkingStationData by remember { mutableStateOf(false) }
    var stationDataCheckFailed by remember { mutableStateOf(false) }
    fun downloadStationData() = scope.launch {
        checkingStationData = true
        stationDataCheckFailed = false
        viewModel.checkStationDataUpdate()
            .onSuccess { asset -> asset?.let { updateViewModel.start(UpdateTarget.STATION_DATA, it) } }
            .onFailure { stationDataCheckFailed = true }
        checkingStationData = false
    }

    var dismissedAppUpdate by rememberSaveable { mutableStateOf<String?>(null) }
    var dismissedStationDataUpdate by rememberSaveable { mutableStateOf<String?>(null) }
    val showAppBanner = appUpdate != null && appUpdate?.version != dismissedAppUpdate
    val showStationDataBanner = !showAppBanner && hasStationData &&
        stationDataAvailable != null && stationDataAvailable?.version != dismissedStationDataUpdate

    fun cooldownOf(stationId: Int) = settings?.let { cooldownSeconds(accessLogs[stationId]?.lastAccess, it, now) } ?: 0

    var menuTarget by remember { mutableStateOf<IndexedValue<NearbyStation>?>(null) }
    menuTarget?.let { (index, item) ->
        val id = item.station.id
        StationMenuSheet(
            item = item,
            showAccessMenu = index != 0,
            accessed = accessLogs[id]?.accessed == true,
            timerEnabled = settings?.enableReminder == true,
            timerRunning = cooldownOf(id) > 0,
            onDismiss = { menuTarget = null },
            onSetAccessed = { viewModel.setAccessed(id, it) },
            onToggleTimer = { if (cooldownOf(id) > 0) viewModel.resetTimer(id) else viewModel.startTimer(id) },
            onOpenRadar = { onNavigate(MapRoute(radarId = id)) },
        )
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                location = location,
                searchResult = searchResult,
                now = now,
                isRunning = isRunning,
                switchEnabled = hasStationData,
                onSwitchChange = { if (it) requestPermissions() else LocationService.stop(context) },
            ) {
                UpdateBanner(
                    visible = showAppBanner,
                    icon = Icons.Rounded.SystemUpdate,
                    text = stringResource(R.string.app_update_banner, appUpdate?.version.orEmpty()),
                    onClick = { appUpdate?.let { updateViewModel.confirm(UpdateTarget.APP, it) } },
                    onDismiss = { dismissedAppUpdate = appUpdate?.version },
                )
                UpdateBanner(
                    visible = showStationDataBanner,
                    icon = Icons.Rounded.Download,
                    text = stringResource(R.string.station_data_update_banner, stationDataAvailable?.version.orEmpty()),
                    onClick = { stationDataAvailable?.let { updateViewModel.confirm(UpdateTarget.STATION_DATA, it) } },
                    onDismiss = { dismissedStationDataUpdate = stationDataAvailable?.version },
                )
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
                Button(onClick = { downloadStationData() }, shapes = ButtonDefaults.shapes(), enabled = !checkingStationData) {
                    if (checkingStationData) {
                        LoadingIndicator(Modifier.size(24.dp), color = LocalContentColor.current)
                        Text(stringResource(R.string.update_checking), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                    } else {
                        Text(stringResource(R.string.update_station_data))
                    }
                }
                if (stationDataCheckFailed) {
                    Text(stringResource(R.string.station_data_update_failed), color = MaterialTheme.colorScheme.error)
                }
            }
            stations.isEmpty() && isRunning -> EmptyMessage(padding, R.string.home_waiting_location) { LoadingIndicator() }
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
                        numberInIcon = settings?.showNumberInAttrIcon == true,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    location: Location?,
    searchResult: SearchResult?,
    now: Long,
    isRunning: Boolean,
    switchEnabled: Boolean,
    onSwitchChange: (Boolean) -> Unit,
    banner: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            TopAppBar(
                title = { Text(stringResource(R.string.title_home)) },
                actions = {
                    Text(stringResource(if (isRunning) R.string.search_on else R.string.search_off))
                    IconSwitch(
                        checked = isRunning,
                        enabled = switchEnabled,
                        onCheckedChange = onSwitchChange,
                        modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                    )
                },
                colors = appBarColors(),
            )
            AnimatedVisibility(isRunning) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val unknown = stringResource(R.string.unknown)
                    val accuracy = location?.takeIf { it.hasAccuracy() }?.let { String.format(Locale.ROOT, "%.1fm", it.accuracy) }
                    val speed = location?.takeIf { it.hasSpeed() }?.let { String.format(Locale.ROOT, "%.1fkm/h", it.speed * 3.6) }
                    StatusItem(Icons.Rounded.Speed, R.string.home_speed, speed ?: unknown)
                    StatusItem(Icons.Rounded.GpsFixed, R.string.home_accuracy, accuracy ?: unknown)
                    Spacer(Modifier.weight(1f))
                    StatusItem(Icons.Rounded.Update, R.string.home_last_update, relativeTime(searchResult?.updatedAt, now))
                }
            }
            banner()
        }
    }
}

@Composable
private fun UpdateBanner(visible: Boolean, icon: ImageVector, text: String, onClick: () -> Unit, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                IconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun StatusItem(icon: ImageVector, @StringRes label: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = stringResource(label), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationMenuSheet(
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
    val sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit): () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    val actions = buildList {
        if (showAccessMenu) {
            if (accessed) {
                add(ToolAction(R.string.mark_unaccessed, Icons.Rounded.Cancel, closeThen { onSetAccessed(false) }))
            } else {
                add(ToolAction(R.string.mark_accessed, Icons.Rounded.CheckCircle, closeThen { onSetAccessed(true) }))
            }
            if (timerEnabled) {
                if (timerRunning) {
                    add(ToolAction(R.string.reset_timer, Icons.Rounded.TimerOff, closeThen(onToggleTimer)))
                } else {
                    add(ToolAction(R.string.start_timer, Icons.Rounded.Timer, closeThen(onToggleTimer)))
                }
            }
        }
        add(ToolAction(R.string.show_radar, Icons.Rounded.Radar, closeThen(onOpenRadar)))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text(item.station.name, style = MaterialTheme.typography.titleLarge)
            item.line?.let { Text(it.name, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                actions.forEachIndexed { index, action -> ToolActionItem(action, index, actions.size) }
            }
        }
    }
}
