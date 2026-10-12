package dev.yukineko.ekimemo_map.map

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationAttr
import dev.yukineko.ekimemo_map.ui.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconColor
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val VORONOI_SOURCE = "voronoi"
private const val POINT_SOURCE = "point"
private const val FILL_LAYER = "fill"
private const val LINE_LAYER = "line"
internal const val POINT_LAYER = "point"
private const val PIN_IMAGE = "pin"
private val ATTR_IMAGES = StationAttr.entries.associateWith { it.name.lowercase() }
private const val NEARBY_COLOR = "#2196F3"
private const val DEFAULT_COLOR = "#F44336"

@Composable
fun StationLayers(
    style: Style,
    stations: List<Station>,
    accessedIds: Set<Int>,
    nearbyIds: Set<Int>,
    showPins: Boolean,
    showAccessState: Boolean,
    attrMode: Boolean,
    showVoronoi: Boolean = true,
) {
    val context = LocalContext.current
    val voronoiSource = remember(style) { GeoJsonSource(VORONOI_SOURCE) }
    val pointSource = remember(style) { GeoJsonSource(POINT_SOURCE) }

    DisposableEffect(style) {
        val images = ATTR_IMAGES.values.associateWith { "icon/$it.png" } + (PIN_IMAGE to "icon/location.png")
        images.forEach { (name, path) -> style.addImage(name, context.assets.open(path).use(BitmapFactory::decodeStream), true) }
        style.addSource(voronoiSource)
        style.addSource(pointSource)
        style.addLayer(
            FillLayer(FILL_LAYER, VORONOI_SOURCE).withProperties(fillColor(get("color"))),
        )
        style.addLayer(LineLayer(LINE_LAYER, VORONOI_SOURCE).withProperties(lineColor(get("color")), lineWidth(1f)))
        style.addLayer(
            SymbolLayer(POINT_LAYER, POINT_SOURCE).withProperties(
                textField(get("name")),
                textFont(arrayOf("migu2m-regular")),
                textSize(14f),
                textColor("#444444"),
                textHaloColor("#ffffff"),
                textHaloWidth(2f),
                textOffset(arrayOf(0f, 2f)),
                iconImage(get("icon")),
                iconSize(0.15f),
                iconColor(get("color")),
            ),
        )
        onDispose {
            if (!style.isFullyLoaded) return@onDispose
            listOf(FILL_LAYER, LINE_LAYER, POINT_LAYER).forEach(style::removeLayer)
            listOf(VORONOI_SOURCE, POINT_SOURCE).forEach(style::removeSource)
            images.keys.forEach(style::removeImage)
        }
    }

    LaunchedEffect(voronoiSource, stations, accessedIds, nearbyIds, attrMode, showVoronoi) {
        val (voronoi, points) = withContext(Dispatchers.Default) {
            val voronoi = mutableListOf<Feature>()
            val points = mutableListOf<Feature>()
            stations.forEach { station ->
                val color = when {
                    attrMode -> station.attr.color(dark = false).toHex()
                    station.id in nearbyIds -> NEARBY_COLOR
                    else -> DEFAULT_COLOR
                }
                if (showVoronoi) {
                    val polygon = Polygon.fromLngLats(listOf(station.voronoi.map { Point.fromLngLat(it.lng, it.lat) }))
                    voronoi += Feature.fromGeometry(polygon).apply {
                        addStringProperty("color", color)
                        addBooleanProperty("accessed", station.id in accessedIds)
                    }
                }
                points += Feature.fromGeometry(Point.fromLngLat(station.lng, station.lat)).apply {
                    addStringProperty("color", color)
                    addStringProperty("name", station.name)
                    addStringProperty("icon", if (attrMode) ATTR_IMAGES.getValue(station.attr) else PIN_IMAGE)
                }
            }
            FeatureCollection.fromFeatures(voronoi) to FeatureCollection.fromFeatures(points)
        }
        voronoiSource.setGeoJson(voronoi)
        pointSource.setGeoJson(points)
    }

    LaunchedEffect(style, attrMode) {
        val opacity = if (attrMode) literal(0.3f) else switchCase(get("accessed"), literal(0.3f), literal(0f))
        style.getLayer(FILL_LAYER)?.setProperties(fillOpacity(opacity))
    }

    LaunchedEffect(style, showPins) {
        style.getLayer(POINT_LAYER)?.setProperties(visibility(if (showPins) Property.VISIBLE else Property.NONE))
    }

    LaunchedEffect(style, showAccessState) {
        style.getLayer(FILL_LAYER)?.setProperties(visibility(if (showAccessState) Property.VISIBLE else Property.NONE))
    }
}

internal fun Color.toHex() = "#%06X".format(toArgb() and 0xFFFFFF)
