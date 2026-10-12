package dev.yukineko.ekimemo_map.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.formatDistance

@Composable
fun StationCard(
    station: Station,
    line: Line?,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    distance: Int? = null,
    cooldown: Int = 0,
    onLongClick: (() -> Unit)? = null,
) {
    Card(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${index + 1}")
                StationAttrIcon(station.attr)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(station.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(line?.name.orEmpty(), modifier = Modifier.alpha(0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (distance != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatDistance(distance), style = MaterialTheme.typography.titleMedium)
                    if (cooldown > 0) Text(formatSeconds(cooldown), modifier = Modifier.alpha(0.8f))
                }
            }
        }
    }
}

@Composable
fun SimpleStationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessed: Boolean = false,
    showAttr: Boolean = false,
) {
    Card(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showAttr) {
                StationAttrIcon(station.attr)
                Spacer(Modifier.width(20.dp))
            }
            Text(station.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (accessed) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.accessed), tint = Color(0xFF4CAF50))
        }
    }
}
