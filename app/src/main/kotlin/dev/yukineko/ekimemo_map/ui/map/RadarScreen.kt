package dev.yukineko.ekimemo_map.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.map.MapLibreMapView
import dev.yukineko.ekimemo_map.map.RadarLayers
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    radarId: Int,
    onBack: () -> Unit,
    viewModel: RadarViewModel = viewModel(factory = RadarViewModel.factory(radarId)),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val mapStyle by viewModel.mapStyle.collectAsState()
    val showLocation = remember {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }
    var tracking by rememberSaveable { mutableStateOf(false) }
    var range by rememberSaveable(state.k) { mutableIntStateOf(state.k) }
    var selectedOnly by rememberSaveable { mutableStateOf(true) }
    var resetCount by remember { mutableIntStateOf(0) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showKEditor by rememberSaveable { mutableStateOf(false) }
    val cameraPadding = with(LocalDensity.current) { 32.dp.roundToPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.station?.let { stringResource(R.string.radar_title, it.name) } ?: stringResource(R.string.station_open_radar)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { showHelp = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = stringResource(R.string.radar_help_title))
                    }
                },
                colors = appBarColors(),
            )
        },
        bottomBar = {
            RadarControls(
                state = state,
                range = range,
                onRangeChange = { range = it },
                selectedOnly = selectedOnly,
                onSelectedOnlyChange = { selectedOnly = it },
                onEditK = { showKEditor = true },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(onClick = { resetCount++ }) {
                    Icon(Icons.Rounded.CenterFocusStrong, contentDescription = stringResource(R.string.map_reset_camera))
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
        MapLibreMapView(
            style = style,
            showLocation = showLocation,
            tracking = tracking,
            onTrackingDismissed = { tracking = false },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { map, loadedStyle ->
            RadarLayers(
                style = loadedStyle,
                polygons = state.polygons,
                range = if (state.computing) state.k else range,
                selectedOnly = !state.computing && selectedOnly,
            )
            LaunchedEffect(state.polygons.size, resetCount) {
                val polygon = state.polygons.lastOrNull() ?: return@LaunchedEffect
                val bounds = LatLngBounds.Builder().includes(polygon.map { LatLng(it.lat, it.lng) }).build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, cameraPadding))
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.radar_help_title)) },
            text = { Text(stringResource(R.string.radar_help_message, state.k)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) }
            },
        )
    }

    if (showKEditor) {
        KEditorDialog(
            initial = state.k,
            onDismiss = { showKEditor = false },
            onConfirm = {
                showKEditor = false
                viewModel.compute(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RadarControls(
    state: RadarState,
    range: Int,
    onRangeChange: (Int) -> Unit,
    selectedOnly: Boolean,
    onSelectedOnlyChange: (Boolean) -> Unit,
    onEditK: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            when {
                state.failed -> Text(stringResource(R.string.radar_failed), color = MaterialTheme.colorScheme.error)
                state.computing -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LoadingIndicator(Modifier.size(32.dp))
                    Text(stringResource(R.string.radar_computing, state.polygons.size, state.k))
                }
                else -> {
                    Text(stringResource(R.string.radar_range, range), style = MaterialTheme.typography.titleSmall)
                    if (state.k > 1) {
                        val sliderState = rememberSliderState(range.toFloat(), steps = state.k - 2, trackRange = 1f..state.k.toFloat())
                        sliderState.value = range.toFloat()
                        Slider(state = sliderState, onValueChange = { onRangeChange(it.roundToInt()) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedOnly,
                    onClick = { onSelectedOnlyChange(!selectedOnly) },
                    label = { Text(stringResource(R.string.radar_selected_only)) },
                    leadingIcon = if (selectedOnly) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                    enabled = !state.computing,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEditK, shapes = ButtonDefaults.shapes(), enabled = !state.computing) {
                    Text(stringResource(R.string.radar_change_k))
                }
            }
        }
    }
}

@Composable
private fun KEditorDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()?.takeIf { it >= 1 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.radar_k_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                isError = value == null,
                suffix = { Text(stringResource(R.string.radar_k_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, shapes = ButtonDefaults.shapes(), enabled = value != null) {
                Text(stringResource(R.string.radar_recompute))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
    )
}
