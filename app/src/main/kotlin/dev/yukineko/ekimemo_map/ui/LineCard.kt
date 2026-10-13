package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.ui.theme.isDark
import java.util.Locale
import kotlin.math.min

data class LineProgress(val accessed: Int, val total: Int) {
    val isComplete get() = accessed == total
    val percent get() = String.format(Locale.ROOT, "%.1f%%", if (total == 0) 0.0 else accessed * 100.0 / total)
}

fun parseLineColor(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor("#" + it.removePrefix("#"))) }.getOrNull() }

data class LineColors(val container: Color, val content: Color, val accent: Color)

private const val MAX_LINE_CHROMA = 40.0

@Composable
fun rememberLineColors(hex: String?): LineColors? {
    val dark = MaterialTheme.colorScheme.isDark
    return remember(hex, dark) {
        parseLineColor(hex)?.toHct()?.let {
            val base = it.withChroma(min(it.chroma, MAX_LINE_CHROMA))
            LineColors(
                container = base.withTone(if (dark) 30.0 else 90.0).toColor(),
                content = base.withTone(if (dark) 80.0 else 40.0).toColor(),
                accent = base.withTone(base.tone.coerceIn(if (dark) 60.0..80.0 else 50.0..75.0)).toColor(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LineIcon(line: Line, modifier: Modifier = Modifier) {
    val colors = rememberLineColors(line.color)
        ?: MaterialTheme.colorScheme.let { LineColors(it.surfaceContainerHighest, it.onSurfaceVariant, it.surfaceContainerHighest) }
    Box(
        modifier = modifier.size(40.dp).background(colors.container, MaterialShapes.Square.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        if (line.symbol != null) {
            InkCenteredText(line.symbol, colors.content, MaterialTheme.typography.titleMediumEmphasized)
        } else {
            Icon(Icons.Rounded.Train, contentDescription = null, tint = colors.content, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun LineCard(line: Line, index: Int, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier, progress: LineProgress? = null) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = if (progress?.isComplete == true) {
            highlightedListItemColors(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        } else {
            listItemColors()
        },
        modifier = modifier,
        leadingContent = { LineIcon(line) },
        trailingContent = progress?.let {
            {
                Text("${it.accessed}/${it.total} (${it.percent})")
            }
        },
    ) {
        Text(line.name, style = MaterialTheme.typography.titleMedium)
    }
}
