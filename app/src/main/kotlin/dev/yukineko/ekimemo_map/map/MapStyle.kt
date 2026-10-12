package dev.yukineko.ekimemo_map.map

import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.SymbolLayer

internal fun mapStyleUri(dark: Boolean) = "asset://map_style/${if (dark) "dark" else "light"}.json"

internal fun Style.addLayerBelowLabels(layer: Layer) {
    val label = layers.firstOrNull { it is SymbolLayer }
    if (label == null) addLayer(layer) else addLayerBelow(layer, label.id)
}

internal fun Style.addLayerBelowLocation(layer: Layer) {
    val location = layers.firstOrNull { it.id.startsWith("mapbox-location-") }
    if (location == null) addLayer(layer) else addLayerBelow(layer, location.id)
}

internal fun Color.withTone(tone: Double) = Hct.from(toHct().hue, 80.0, tone).toColor()
