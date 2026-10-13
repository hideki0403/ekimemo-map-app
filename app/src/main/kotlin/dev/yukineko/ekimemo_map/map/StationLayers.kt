package dev.yukineko.ekimemo_map.map

import android.graphics.BitmapFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.ktx.harmonize
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationAttr
import dev.yukineko.ekimemo_map.ui.color
import dev.yukineko.ekimemo_map.ui.theme.isDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.switchCase
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconColor
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
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
private const val CIRCLE_LAYER = "circle"
private const val POINT_LAYER = "point"
private val ATTR_IMAGES = StationAttr.entries.associateWith { it.name.lowercase() }

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
    val colors = MaterialTheme.colorScheme
    val voronoiSource = remember(style) { GeoJsonSource(VORONOI_SOURCE) }
    val pointSource = remember(style) { GeoJsonSource(POINT_SOURCE) }

    DisposableEffect(style) {
        val images = ATTR_IMAGES.values.associateWith { "icon/$it.png" }
        images.forEach { (name, path) -> style.addImage(name, context.assets.open(path).use(BitmapFactory::decodeStream), true) }
        style.addSource(voronoiSource)
        style.addSource(pointSource)
        style.addLayerBelowLabels(FillLayer(FILL_LAYER, VORONOI_SOURCE).withProperties(fillColor(get("color"))))
        style.addLayerBelowLabels(LineLayer(LINE_LAYER, VORONOI_SOURCE).withProperties(lineColor(get("color")), lineWidth(1f)))
        style.addLayerBelowLocation(
            CircleLayer(CIRCLE_LAYER, POINT_SOURCE).withProperties(
                circleColor(get("color")),
                circleRadius(interpolate(linear(), zoom(), stop(10, 3f), stop(16, 6f))),
                circleStrokeWidth(1.5f),
            ),
        )
        style.addLayerBelowLocation(
            SymbolLayer(POINT_LAYER, POINT_SOURCE).withProperties(
                textField(get("name")),
                textFont(arrayOf("Noto Sans CJK JP Regular")),
                textSize(14f),
                textHaloWidth(2f),
                textOptional(true),
                iconImage(get("icon")),
                iconSize(interpolate(linear(), zoom(), stop(10, 0.08f), stop(16, 0.13f))),
                iconColor(get("color")),
                iconAllowOverlap(true),
            ),
        )
        onDispose {
            if (!style.isFullyLoaded) return@onDispose
            listOf(FILL_LAYER, LINE_LAYER, CIRCLE_LAYER, POINT_LAYER).forEach(style::removeLayer)
            listOf(VORONOI_SOURCE, POINT_SOURCE).forEach(style::removeSource)
            images.keys.forEach(style::removeImage)
        }
    }

    val dark = colors.isDark
    LaunchedEffect(voronoiSource, stations, accessedIds, nearbyIds, attrMode, showVoronoi, colors.primary, colors.tertiary, dark) {
        val (voronoi, points) = withContext(Dispatchers.Default) {
            val attrColors = StationAttr.entries.associateWith { it.color(dark).harmonize(colors.primary).toHex() }
            val tone = if (dark) 70.0 else 60.0
            val nearbyColor = colors.tertiary.withTone(tone).toHex()
            val defaultColor = colors.primary.withTone(tone).toHex()
            val voronoi = mutableListOf<Feature>()
            val points = mutableListOf<Feature>()
            stations.forEach { station ->
                val color = when {
                    attrMode -> attrColors.getValue(station.attr)
                    station.id in nearbyIds -> nearbyColor
                    else -> defaultColor
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
                    if (attrMode) addStringProperty("icon", ATTR_IMAGES.getValue(station.attr))
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

    LaunchedEffect(style, colors.onSurface, colors.surface) {
        style.getLayer(POINT_LAYER)?.setProperties(textColor(colors.onSurface.toHex()), textHaloColor(colors.surface.toHex()))
        style.getLayer(CIRCLE_LAYER)?.setProperties(circleStrokeColor(colors.surface.toHex()))
    }

    LaunchedEffect(style, showPins, attrMode) {
        style.getLayer(POINT_LAYER)?.setProperties(
            visibility(if (showPins) Property.VISIBLE else Property.NONE),
            textAnchor(Property.TEXT_ANCHOR_TOP),
            textOffset(arrayOf(0f, if (attrMode) 1f else 0.6f)),
        )
        style.getLayer(CIRCLE_LAYER)?.setProperties(visibility(if (showPins && !attrMode) Property.VISIBLE else Property.NONE))
    }

    LaunchedEffect(style, showAccessState) {
        style.getLayer(FILL_LAYER)?.setProperties(visibility(if (showAccessState) Property.VISIBLE else Property.NONE))
    }
}

internal fun Color.toHex() = "#%06X".format(toArgb() and 0xFFFFFF)
