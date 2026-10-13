package dev.yukineko.ekimemo_map.ui.map

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.ui.IconSwitch
import dev.yukineko.ekimemo_map.ui.highlightedListItemColors

enum class MapMode(@StringRes val label: Int, val icon: ImageVector) {
    NORMAL(R.string.map_mode_normal, Icons.Rounded.Map),
    ATTRIBUTE(R.string.map_mode_attribute, Icons.Rounded.Category),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLayersSheet(
    mode: MapMode,
    onModeChange: (MapMode) -> Unit,
    showPins: Boolean,
    onShowPinsChange: (Boolean) -> Unit,
    showAccessState: Boolean,
    onShowAccessStateChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val attrMode = mode == MapMode.ATTRIBUTE
    val toggles = listOf(
        Triple(if (attrMode) R.string.map_show_attr_icons else R.string.map_show_pins, showPins, onShowPinsChange),
        Triple(if (attrMode) R.string.map_show_fill else R.string.map_show_access_state, showAccessState, onShowAccessStateChange),
    )
    val itemColors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            SheetSectionTitle(R.string.map_mode_title)
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                MapMode.entries.forEachIndexed { index, item ->
                    val selected = item == mode
                    SegmentedListItem(
                        onClick = { onModeChange(item) },
                        shapes = ListItemDefaults.segmentedShapes(index, MapMode.entries.size),
                        colors = if (selected) {
                            highlightedListItemColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        } else {
                            itemColors
                        },
                        leadingContent = { Icon(item.icon, contentDescription = null) },
                        trailingContent = if (selected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    ) {
                        Text(stringResource(item.label))
                    }
                }
            }
            SheetSectionTitle(R.string.map_display_title, Modifier.padding(top = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                toggles.forEachIndexed { index, (label, checked, onChange) ->
                    SegmentedListItem(
                        onClick = { onChange(!checked) },
                        shapes = ListItemDefaults.segmentedShapes(index, toggles.size),
                        colors = itemColors,
                        trailingContent = { IconSwitch(checked = checked, onCheckedChange = onChange) },
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSectionTitle(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall, modifier = modifier.padding(bottom = 8.dp))
}
