package dev.yukineko.ekimemo_map.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.yukineko.ekimemo_map.data.user.MoveLog
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val MOVE_LOG_SOURCE = "move_log"
private const val MOVE_LOG_LAYER = "move_log"

@Composable
fun MoveLogLayers(style: Style, tracks: List<List<MoveLog>>) {
    val color = MaterialTheme.colorScheme.primary
    val source = remember(style) { GeoJsonSource(MOVE_LOG_SOURCE) }

    DisposableEffect(style) {
        style.addSource(source)
        style.addLayerBelowLabels(
            LineLayer(MOVE_LOG_LAYER, MOVE_LOG_SOURCE).withProperties(
                lineWidth(3f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        onDispose {
            if (!style.isFullyLoaded) return@onDispose
            style.removeLayer(MOVE_LOG_LAYER)
            style.removeSource(MOVE_LOG_SOURCE)
        }
    }

    LaunchedEffect(style, color) {
        style.getLayer(MOVE_LOG_LAYER)?.setProperties(lineColor(color.toHex()))
    }

    LaunchedEffect(source, tracks) {
        val features = tracks.filter { it.size >= 2 }.map { logs ->
            Feature.fromGeometry(LineString.fromLngLats(logs.map { Point.fromLngLat(it.longitude, it.latitude) }))
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}
