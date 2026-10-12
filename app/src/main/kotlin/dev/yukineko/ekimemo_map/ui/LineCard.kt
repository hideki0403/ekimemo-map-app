package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.data.station.Line
import java.util.Locale

data class LineProgress(val accessed: Int, val total: Int) {
    val isComplete get() = accessed == total
}

fun parseLineColor(hex: String?): Color =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor("#" + it.removePrefix("#"))) }.getOrNull() }
        ?: Color.Transparent

@Composable
fun LineCard(line: Line, onClick: () -> Unit, modifier: Modifier = Modifier, progress: LineProgress? = null) {
    Card(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape(4.dp)
            Box(
                Modifier
                    .size(width = 8.dp, height = 30.dp)
                    .background(parseLineColor(line.color), shape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), shape),
            )
            Spacer(Modifier.width(12.dp))
            Text(line.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (progress != null) {
                val percent = if (progress.total == 0) 0.0 else progress.accessed * 100.0 / progress.total
                Text(
                    String.format(Locale.ROOT, "%d/%d (%.1f%%)", progress.accessed, progress.total, percent),
                    color = if (progress.isComplete) MaterialTheme.colorScheme.primary else Color.Unspecified,
                )
            }
        }
    }
}
