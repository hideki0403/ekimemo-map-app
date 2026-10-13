package dev.yukineko.ekimemo_map.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import dev.yukineko.ekimemo_map.data.station.StationAttr
import dev.yukineko.ekimemo_map.ui.theme.isDark
import kotlin.math.roundToInt

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StationAttrIcon(attr: StationAttr, modifier: Modifier = Modifier, size: Dp = 40.dp, number: Int? = null) {
    val colors = MaterialTheme.colorScheme
    val tint = remember(attr, colors) { attr.color(colors.isDark).harmonize(colors.primary) }
    Box(
        modifier = modifier.size(size).background(tint.copy(alpha = 0.2f), MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        if (number != null) {
            InkCenteredText("$number", tint, MaterialTheme.typography.titleMediumEmphasized)
        } else {
            Icon(attr.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.6f))
        }
    }
}

@Composable
fun InkCenteredText(text: String, color: Color, style: TextStyle) {
    val typeface by LocalFontFamilyResolver.current.resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal)
    val textSize = with(LocalDensity.current) { style.fontSize.toPx() }
    val inkBounds = remember(text, typeface, textSize) {
        val paint = Paint().apply {
            this.typeface = typeface as Typeface
            this.textSize = textSize
        }
        Rect().also { paint.getTextBounds(text, 0, text.length, it) }
    }
    Text(
        text,
        color = color,
        style = style,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val inkCenter = placeable[FirstBaseline] + inkBounds.exactCenterY()
            layout(placeable.width, placeable.height) {
                placeable.place(0, (placeable.height / 2f - inkCenter).roundToInt())
            }
        },
    )
}
