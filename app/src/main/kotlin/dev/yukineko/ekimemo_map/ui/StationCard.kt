package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.formatDistance
import dev.yukineko.ekimemo_map.ui.theme.isDark

private val AccessedColor = Color(0xFF4CAF50)

@Composable
fun listItemContainerColor(): Color = MaterialTheme.colorScheme.run { if (isDark) surfaceContainerHigh else surface }

@Composable
fun listItemColors(): ListItemColors = ListItemDefaults.segmentedColors(containerColor = listItemContainerColor())

@Composable
fun highlightedListItemColors(containerColor: Color, contentColor: Color): ListItemColors =
    ListItemDefaults.segmentedColors(
        containerColor = containerColor,
        contentColor = contentColor,
        leadingContentColor = contentColor,
        trailingContentColor = contentColor,
        supportingContentColor = contentColor,
    )

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
    numberInIcon: Boolean = false,
) {
    SegmentedListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = listItemColors(),
        modifier = modifier,
        leadingContent = {
            if (numberInIcon) {
                StationAttrIcon(station.attr, size = 36.dp, number = index + 1)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$count", modifier = Modifier.alpha(0f).clearAndSetSemantics {})
                        Text("${index + 1}")
                    }
                    StationAttrIcon(station.attr, size = 36.dp)
                }
            }
        },
        supportingContent = { Text(line?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = distance?.let {
            {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatDistance(it), style = MaterialTheme.typography.titleMediumEmphasized)
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
        colors = listItemColors(),
        modifier = modifier,
        leadingContent = if (showAttr) {
            { StationAttrIcon(station.attr) }
        } else {
            null
        },
        trailingContent = if (accessed) {
            { Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.accessed), tint = AccessedColor.harmonize(MaterialTheme.colorScheme.primary)) }
        } else {
            null
        },
    ) {
        Text(station.name, style = MaterialTheme.typography.titleMedium)
    }
}
