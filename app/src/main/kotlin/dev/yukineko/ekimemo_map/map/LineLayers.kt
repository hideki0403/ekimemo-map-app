package dev.yukineko.ekimemo_map.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.yukineko.ekimemo_map.data.station.LatLng
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

private const val LINE_SOURCE = "selected_line"
private const val LINE_LAYER = "selected_line"

@Composable
fun LineLayers(style: Style, polylines: List<List<LatLng>>, color: Color) {
    val source = remember(style) { GeoJsonSource(LINE_SOURCE) }

    DisposableEffect(style) {
        style.addSource(source)
        style.addLayerBelowLabels(
            LineLayer(LINE_LAYER, LINE_SOURCE).withProperties(
                lineWidth(4f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        onDispose {
            if (!style.isFullyLoaded) return@onDispose
            style.removeLayer(LINE_LAYER)
            style.removeSource(LINE_SOURCE)
        }
    }

    LaunchedEffect(source, polylines) {
        val features = polylines.map { line -> Feature.fromGeometry(LineString.fromLngLats(line.map { Point.fromLngLat(it.lng, it.lat) })) }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(style, color) {
        style.getLayer(LINE_LAYER)?.setProperties(lineColor(color.toHex()))
    }
}
