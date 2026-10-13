package dev.yukineko.ekimemo_map.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import dev.yukineko.ekimemo_map.data.station.LatLng
import dev.yukineko.ekimemo_map.ui.theme.isDark
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val RADAR_SOURCE = "radar"
private const val RADAR_FILL_LAYER = "radar_fill"
private const val RADAR_LINE_LAYER = "radar_line"

@Composable
fun RadarLayers(style: Style, polygons: List<List<LatLng>>, range: Int, selectedOnly: Boolean) {
    val tone = if (MaterialTheme.colorScheme.isDark) 70.0 else 60.0
    val source = remember(style) { GeoJsonSource(RADAR_SOURCE) }

    DisposableEffect(style) {
        style.addSource(source)
        style.addLayerBelowLabels(FillLayer(RADAR_FILL_LAYER, RADAR_SOURCE).withProperties(fillColor(get("color")), fillOpacity(0.3f)))
        style.addLayerBelowLabels(LineLayer(RADAR_LINE_LAYER, RADAR_SOURCE).withProperties(lineColor(get("color")), lineWidth(1.5f)))
        onDispose {
            if (!style.isFullyLoaded) return@onDispose
            listOf(RADAR_FILL_LAYER, RADAR_LINE_LAYER).forEach(style::removeLayer)
            style.removeSource(RADAR_SOURCE)
        }
    }

    LaunchedEffect(source, polygons, range, selectedOnly, tone) {
        val features = polygons.withIndex()
            .filter { (index, _) -> if (selectedOnly) index + 1 == range else index + 1 <= range }
            .map { (index, polygon) ->
                val ring = (polygon + polygon.first()).map { Point.fromLngLat(it.lng, it.lat) }
                val color = Hct.from((index + 1.0) / range * 360.0, 80.0, tone).toColor()
                Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                    addStringProperty("color", color.toHex())
                }
            }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(style, selectedOnly) {
        style.getLayer(RADAR_FILL_LAYER)?.setProperties(visibility(if (selectedOnly) Property.VISIBLE else Property.NONE))
    }
}
