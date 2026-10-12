package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.formatDistance

@Composable
fun segmentedCardColors(): ListItemColors =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

@Composable
fun StationCard(
    station: Station,
    line: Line?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    distance: Int? = null,
    cooldown: Int = 0,
    onLongClick: (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segmentedCardColors(),
        modifier = modifier,
        leadingContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${index + 1}")
                StationAttrIcon(station.attr)
            }
        },
        supportingContent = { Text(line?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = distance?.let {
            {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatDistance(it), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMediumEmphasized)
                    if (cooldown > 0) Text(formatSeconds(cooldown))
                }
            }
        },
    ) {
        Text(station.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SimpleStationCard(
    station: Station,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessed: Boolean = false,
    showAttr: Boolean = false,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segmentedCardColors(),
        modifier = modifier,
        leadingContent = if (showAttr) {
            { StationAttrIcon(station.attr) }
        } else {
            null
        },
        trailingContent = if (accessed) {
            { Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.accessed), tint = Color(0xFF4CAF50)) }
        } else {
            null
        },
    ) {
        Text(station.name, style = MaterialTheme.typography.titleMedium)
    }
}
