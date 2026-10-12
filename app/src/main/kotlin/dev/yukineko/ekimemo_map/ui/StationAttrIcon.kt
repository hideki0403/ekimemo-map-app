package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import dev.yukineko.ekimemo_map.data.station.StationAttr
import dev.yukineko.ekimemo_map.ui.theme.isDark

fun StationAttr.color(dark: Boolean): Color = when (this) {
    StationAttr.ECO -> if (dark) Color(0xFF81C784) else Color(0xFF66BB6A)
    StationAttr.HEAT -> if (dark) Color(0xFFE57373) else Color(0xFFEF5350)
    StationAttr.COOL -> if (dark) Color(0xFF64B5F6) else Color(0xFF42A5F5)
    StationAttr.UNKNOWN -> if (dark) Color(0xFFBDBDBD) else Color(0xFF757575)
}

private val StationAttr.icon
    get() = when (this) {
        StationAttr.ECO -> Icons.Rounded.EnergySavingsLeaf
        StationAttr.HEAT -> Icons.Rounded.Whatshot
        StationAttr.COOL -> Icons.Rounded.WaterDrop
        StationAttr.UNKNOWN -> Icons.Rounded.Adjust
    }

@Composable
fun StationAttrIcon(attr: StationAttr, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tint = remember(attr, colors) { attr.color(colors.isDark).harmonize(colors.primary) }
    Icon(attr.icon, contentDescription = null, tint = tint, modifier = modifier.size(20.dp))
}
