package dev.yukineko.ekimemo_map.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.map.MapLibreMapView
import dev.yukineko.ekimemo_map.map.MoveLogLayers
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

private fun DateTimeFormatter.formatEpoch(epochMillis: Long) = format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveLogMapScreen(
    sessionIds: List<Long>,
    onBack: () -> Unit,
    viewModel: MoveLogMapViewModel = viewModel(factory = MoveLogMapViewModel.factory(sessionIds)),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val mapStyle by viewModel.mapStyle.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    var hiddenIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var resetCount by remember { mutableIntStateOf(0) }
    var showSessions by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<MoveLogTrack?>(null) }
    val cameraPadding = with(LocalDensity.current) { 32.dp.roundToPx() }
    val allTracks = tracks.orEmpty()
    val visibleTracks = allTracks.filter { it.sessionId !in hiddenIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_movement_log)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (allTracks.isNotEmpty()) {
                        IconButton(onClick = { showStats = true }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.move_log_stats))
                        }
                    }
                },
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
            if (allTracks.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SmallFloatingActionButton(onClick = { resetCount++ }) {
                        Icon(Icons.Rounded.CenterFocusStrong, contentDescription = stringResource(R.string.map_reset_camera))
                    }
                    SmallFloatingActionButton(onClick = { showSessions = true }) {
                        Icon(Icons.Rounded.Route, contentDescription = stringResource(R.string.move_log_sessions))
                    }
                }
            }
        },
    ) { padding ->
        val style = mapStyle ?: return@Scaffold
        Box(Modifier.fillMaxSize().padding(padding)) {
            MapLibreMapView(
                style = style,
                showLocation = false,
                tracking = false,
                onTrackingDismissed = {},
                modifier = Modifier.fillMaxSize(),
            ) { map, loadedStyle ->
                MoveLogLayers(style = loadedStyle, tracks = visibleTracks.map { it.logs })
                LaunchedEffect(visibleTracks.map { it.sessionId }, resetCount) {
                    val points = visibleTracks.flatMap { it.logs }.map { LatLng(it.latitude, it.longitude) }.distinct()
                    val update = when (points.size) {
                        0 -> return@LaunchedEffect
                        1 -> CameraUpdateFactory.newLatLngZoom(points.first(), 15.0)
                        else -> CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), cameraPadding)
                    }
                    map.moveCamera(update)
                }
            }
            if (tracks?.isEmpty() == true) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(stringResource(R.string.move_log_empty), modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showSessions && allTracks.isNotEmpty()) {
        SessionsSheet(
            tracks = allTracks,
            hiddenIds = hiddenIds,
            onToggle = { id -> hiddenIds = if (id in hiddenIds) hiddenIds - id else hiddenIds + id },
            onDelete = { deleting = it },
            onDismiss = { showSessions = false },
        )
    }

    if (showStats && allTracks.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showStats = false },
            title = { Text(stringResource(R.string.move_log_stats)) },
            text = {
                Text(
                    stringResource(
                        R.string.move_log_stats_message,
                        dateFormatter.formatEpoch(allTracks.first().logs.first().timestamp),
                        allTracks.size,
                        allTracks.sumOf { it.logs.size },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { showStats = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    deleting?.let { track ->
        val deleted = stringResource(R.string.move_log_deleted)
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.move_log_delete_title)) },
            text = { Text(stringResource(R.string.move_log_delete_message, track.logs.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = null
                        scope.launch { if (viewModel.delete(track.sessionId)) snackbarHostState.showSnackbar(deleted) }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsSheet(
    tracks: List<MoveLogTrack>,
    hiddenIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDelete: (MoveLogTrack) -> Unit,
    onDismiss: () -> Unit,
) {
    val itemColors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item {
                Text(
                    stringResource(R.string.move_log_sessions),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            itemsIndexed(tracks, key = { _, track -> track.sessionId }) { index, track ->
                val visible = track.sessionId !in hiddenIds
                SegmentedListItem(
                    onClick = { onToggle(track.sessionId) },
                    shapes = ListItemDefaults.segmentedShapes(index, tracks.size),
                    colors = itemColors,
                    leadingContent = { Checkbox(checked = visible, onCheckedChange = { onToggle(track.sessionId) }) },
                    supportingContent = { Text(stringResource(R.string.move_log_point_count, track.logs.size)) },
                    trailingContent = {
                        IconButton(onClick = { onDelete(track) }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.move_log_session_time,
                            timeFormatter.formatEpoch(track.logs.first().timestamp),
                            timeFormatter.formatEpoch(track.logs.last().timestamp),
                        ),
                    )
                }
            }
        }
    }
}
