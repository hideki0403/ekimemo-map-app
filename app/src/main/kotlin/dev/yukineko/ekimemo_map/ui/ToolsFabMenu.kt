package dev.yukineko.ekimemo_map.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.yukineko.ekimemo_map.R

data class ToolAction(@StringRes val text: Int, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun ToolsFabMenu(actions: List<ToolAction>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(expanded) { expanded = false }

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(checked = expanded, onCheckedChange = { expanded = it }) {
                Icon(
                    if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.title_tools),
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        actions.forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = {
                    expanded = false
                    action.onClick()
                },
                text = { Text(stringResource(action.text)) },
                icon = { Icon(action.icon, contentDescription = null) },
            )
        }
    }
}

@Composable
fun ToolActionItem(action: ToolAction, index: Int, count: Int) {
    SegmentedListItem(
        onClick = action.onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        leadingContent = { Icon(action.icon, contentDescription = null) },
    ) {
        Text(stringResource(action.text))
    }
}
