package dev.yukineko.ekimemo_map.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.map.MapLibreMapView
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onBack: () -> Unit, viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)) {
    val context = LocalContext.current
    val mapStyle by viewModel.mapStyle.collectAsState()
    val showLocation = remember {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }
    var tracking by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_map)) },
                navigationIcon = { BackButton(onBack) },
                colors = appBarColors(),
            )
        },
        floatingActionButton = {
            if (showLocation) {
                FloatingActionButton(onClick = { tracking = !tracking }) {
                    Icon(
                        if (tracking) Icons.Rounded.GpsFixed else Icons.Rounded.GpsNotFixed,
                        contentDescription = stringResource(R.string.map_follow_location),
                        tint = if (tracking) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
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
        )
    }
}
