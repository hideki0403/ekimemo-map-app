package dev.yukineko.ekimemo_map.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Bounds
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.formatDistance
import dev.yukineko.ekimemo_map.location.locationUpdates
import dev.yukineko.ekimemo_map.map.LineLayers
import dev.yukineko.ekimemo_map.map.MapLibreMapView
import dev.yukineko.ekimemo_map.map.StationLayers
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.StationAttrIcon
import dev.yukineko.ekimemo_map.ui.StationCard
import dev.yukineko.ekimemo_map.ui.ToolAction
import dev.yukineko.ekimemo_map.ui.ToolActionItem
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.parseLineColor
import dev.yukineko.ekimemo_map.ui.navigation.MapRoute
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    stationId: Int?,
    lineId: Int?,
    onNavigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.factory(stationId, lineId)),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val mapStyle by viewModel.mapStyle.collectAsState()
    val visibleStations by viewModel.visibleStations.collectAsState()
    val accessedIds by viewModel.accessedIds.collectAsState()
    val nearbyIds by viewModel.nearbyIds.collectAsState()
    val focus by viewModel.focus.collectAsState()
    val showLocation = remember {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }
    var tracking by rememberSaveable { mutableStateOf(false) }
    var cameraInitialized by rememberSaveable { mutableStateOf(false) }
    var showPins by rememberSaveable { mutableStateOf(true) }
    var showAccessState by rememberSaveable { mutableStateOf(stationId == null) }
    var mode by rememberSaveable { mutableStateOf(MapMode.NORMAL) }
    var showLayers by rememberSaveable { mutableStateOf(false) }
    var tapped by remember { mutableStateOf<TappedPoint?>(null) }
    val motion = MaterialTheme.motionScheme
    val cameraPadding = with(LocalDensity.current) { 32.dp.roundToPx() }

    fun showMessage(message: String) = scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }

    fun toggleAccessed(station: Station) = scope.launch {
        val accessed = viewModel.toggleAccessed(station) ?: return@launch
        showMessage(context.getString(if (accessed) R.string.map_marked_accessed else R.string.map_marked_unaccessed, station.name))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(focus?.station?.name ?: focus?.line?.name ?: stringResource(R.string.title_map)) },
                navigationIcon = { BackButton(onBack) },
                colors = appBarColors(),
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(onClick = { showLayers = true }) {
                    Icon(Icons.Rounded.Layers, contentDescription = stringResource(R.string.map_layers))
                }
                if (showLocation) {
                    FloatingActionButton(onClick = { tracking = !tracking }) {
                        Icon(
                            if (tracking) Icons.Rounded.GpsFixed else Icons.Rounded.GpsNotFixed,
                            contentDescription = stringResource(R.string.map_follow_location),
                            tint = if (tracking) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                }
            }
        },
    ) { padding ->
        val style = mapStyle ?: return@Scaffold
        val focusedStations = focus?.stations
        Box(Modifier.fillMaxSize().padding(padding)) {
            MapLibreMapView(
                style = style,
                showLocation = showLocation,
                tracking = tracking,
                onTrackingDismissed = { tracking = false },
                modifier = Modifier.fillMaxSize(),
                onMapClick = { latLng ->
                    if (focusedStations == null) scope.launch { tapped = viewModel.tap(latLng.latitude, latLng.longitude) }
                },
                onMapLongClick = { latLng ->
                    if (focusedStations != null) return@MapLibreMapView
                    val station = viewModel.nearestStation(latLng.latitude, latLng.longitude) ?: return@MapLibreMapView
                    if (showAccessState) toggleAccessed(station) else showMessage(station.name)
                },
                onCameraIdle = { viewModel.onCameraIdle(Bounds(it.latitudeNorth, it.longitudeEast, it.latitudeSouth, it.longitudeWest)) },
            ) { map, loadedStyle ->
                StationLayers(
                    style = loadedStyle,
                    stations = focusedStations ?: (visibleStations as? VisibleStations.Ready)?.stations.orEmpty(),
                    accessedIds = accessedIds,
                    nearbyIds = nearbyIds,
                    showPins = showPins,
                    showAccessState = showAccessState,
                    attrMode = mode == MapMode.ATTRIBUTE,
                    showVoronoi = focus?.line == null,
                )
                focus?.line?.let { line ->
                    LineLayers(
                        style = loadedStyle,
                        polylines = line.polylines,
                        color = parseLineColor(line.color) ?: MaterialTheme.colorScheme.primary,
                    )
                }
                LaunchedEffect(focus) {
                    val focus = focus
                    if (cameraInitialized || focus == null) return@LaunchedEffect
                    val station = focus.station
                    val bounds = focus.line?.bounds
                    val update = when {
                        station != null -> CameraUpdateFactory.newLatLngZoom(LatLng(station.lat, station.lng), 14.0)
                        bounds != null -> CameraUpdateFactory.newLatLngBounds(
                            LatLngBounds.from(bounds.north, bounds.east, bounds.south, bounds.west),
                            cameraPadding,
                        )
                        showLocation -> {
                            val location = context.locationUpdates(1000).first()
                            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 12.0)
                        }
                        else -> return@LaunchedEffect
                    }
                    map.moveCamera(update)
                    cameraInitialized = true
                }
            }
            AnimatedVisibility(
                visible = focusedStations == null && visibleStations == VisibleStations.TooMany,
                modifier = Modifier.padding(8.dp),
                enter = fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.95f),
                exit = fadeOut(motion.defaultEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), targetScale = 0.95f),
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
                    Text(stringResource(R.string.map_too_many_stations), modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showLayers) {
        MapLayersSheet(
            mode = mode,
            onModeChange = { mode = it },
            showPins = showPins,
            onShowPinsChange = { showPins = it },
            showAccessState = showAccessState,
            onShowAccessStateChange = { showAccessState = it },
            onDismiss = { showLayers = false },
        )
    }

    tapped?.let { point ->
        TappedPointSheet(
            point = point,
            accessed = point.stations.first().station.id in accessedIds,
            onDismiss = { tapped = null },
            onToggleAccessed = { toggleAccessed(point.stations.first().station) },
            onNavigate = onNavigate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TappedPointSheet(
    point: TappedPoint,
    accessed: Boolean,
    onDismiss: () -> Unit,
    onToggleAccessed: () -> Unit,
    onNavigate: (Any) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit): () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    val station = point.stations.first().station
    val actions = listOf(
        if (accessed) {
            ToolAction(R.string.mark_unaccessed, Icons.Rounded.Cancel, closeThen(onToggleAccessed))
        } else {
            ToolAction(R.string.mark_accessed, Icons.Rounded.CheckCircle, closeThen(onToggleAccessed))
        },
        ToolAction(R.string.show_radar, Icons.Rounded.Radar, closeThen { onNavigate(MapRoute(radarId = station.id)) }),
        ToolAction(R.string.open_station_detail, Icons.Rounded.Info) { onNavigate(StationDetailRoute(station.id)) },
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(station.name, style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StationAttrIcon(station.attr)
                        Text(station.nameKana, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        point.distanceFromCurrent?.let {
                            Text(stringResource(R.string.map_distance_from_current, formatDistance(it)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            itemsIndexed(actions) { index, action -> ToolActionItem(action, index, actions.size) }
            item {
                Text(
                    stringResource(R.string.map_reachable_stations),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(point.stations) { index, item ->
                StationCard(
                    station = item.station,
                    line = item.line,
                    index = index,
                    count = point.stations.size,
                    distance = item.distance,
                    onClick = { onNavigate(StationDetailRoute(item.station.id)) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
