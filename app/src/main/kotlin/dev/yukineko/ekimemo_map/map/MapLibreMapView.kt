package dev.yukineko.ekimemo_map.map

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.yukineko.ekimemo_map.location.locationUpdates
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private val initialCamera = CameraPosition.Builder().target(LatLng(35.681236, 139.767125)).zoom(10.0).build()

@SuppressLint("MissingPermission")
@Composable
fun MapLibreMapView(
    style: MapStyle,
    showLocation: Boolean,
    tracking: Boolean,
    onTrackingDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onCameraIdle: (LatLngBounds) -> Unit = {},
    content: @Composable (MapLibreMap, Style) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context, MapLibreMapOptions.createFromAttributes(context).camera(initialCamera).textureMode(true))
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var locationReady by remember { mutableStateOf(false) }
    val currentOnTrackingDismissed by rememberUpdatedState(onTrackingDismissed)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        mapView.getMapAsync {
            it.addOnMapClickListener { latLng -> currentOnMapClick(latLng); true }
            it.addOnMapLongClickListener { latLng -> currentOnMapLongClick(latLng); true }
            it.addOnCameraIdleListener { currentOnCameraIdle(it.projection.visibleRegion.latLngBounds) }
            map = it
        }
        onDispose {
            lifecycle.removeObserver(observer)
            val state = lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            if (state.isAtLeast(Lifecycle.State.CREATED)) mapView.onDestroy()
        }
    }

    LaunchedEffect(map, style) {
        val map = map ?: return@LaunchedEffect
        loadedStyle = null
        map.setStyle(Style.Builder().fromUri("asset://${style.assetPath}")) { loaded ->
            loadedStyle = loaded
            currentOnCameraIdle(map.projection.visibleRegion.latLngBounds)
            val component = map.locationComponent
            if (!showLocation || component.isLocationComponentActivated) return@setStyle
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, loaded).useDefaultLocationEngine(false).build(),
            )
            component.isLocationComponentEnabled = true
            component.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
                override fun onCameraTrackingDismissed() = currentOnTrackingDismissed()
                override fun onCameraTrackingChanged(currentMode: Int) {}
            })
            locationReady = true
        }
    }

    LaunchedEffect(locationReady) {
        val component = map?.locationComponent?.takeIf { locationReady } ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            context.locationUpdates(1000).collect { component.forceLocationUpdate(it) }
        }
    }

    LaunchedEffect(locationReady, tracking) {
        val component = map?.locationComponent?.takeIf { locationReady } ?: return@LaunchedEffect
        component.cameraMode = if (tracking) CameraMode.TRACKING else CameraMode.NONE
    }

    AndroidView(factory = { mapView }, modifier = modifier)
    map?.let { map -> loadedStyle?.let { content(map, it) } }
}
