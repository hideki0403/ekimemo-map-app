package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.data.station.Line
import java.util.Locale

data class LineProgress(val accessed: Int, val total: Int) {
    val isComplete get() = accessed == total
    val percent get() = String.format(Locale.ROOT, "%.1f%%", if (total == 0) 0.0 else accessed * 100.0 / total)
}

fun parseLineColor(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor("#" + it.removePrefix("#"))) }.getOrNull() }

@Composable
fun LineCard(line: Line, index: Int, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier, progress: LineProgress? = null) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segmentedCardColors(),
        modifier = modifier,
        leadingContent = {
            val shape = RoundedCornerShape(4.dp)
            Box(
                Modifier
                    .size(width = 8.dp, height = 30.dp)
                    .background(parseLineColor(line.color) ?: Color.Transparent, shape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), shape),
            )
        },
        trailingContent = progress?.let {
            {
                Text(
                    "${it.accessed}/${it.total} (${it.percent})",
                    color = if (it.isComplete) MaterialTheme.colorScheme.primary else Color.Unspecified,
                )
            }
        },
    ) {
        Text(line.name, style = MaterialTheme.typography.titleMedium)
    }
}
