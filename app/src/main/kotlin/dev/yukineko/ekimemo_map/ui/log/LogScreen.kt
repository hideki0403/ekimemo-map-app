package dev.yukineko.ekimemo_map.ui.log

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.log.LogEntry
import dev.yukineko.ekimemo_map.log.LogLevel
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.formatDateTime
import dev.yukineko.ekimemo_map.ui.listItemColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit) {
    val entries by Logger.buffer.entries.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    var levels by rememberSaveable { mutableStateOf(LogLevel.entries.toSet()) }
    var tag by rememberSaveable { mutableStateOf("") }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val filtered = remember(entries, levels, tag) {
        entries.asReversed().filter { it.level in levels && it.tag.contains(tag, ignoreCase = true) }
    }
    val copied = stringResource(R.string.log_copied)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_log)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { confirmClear = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = stringResource(R.string.log_delete_title))
                    }
                },
                colors = appBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showFilter = true }) {
                Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.log_filter))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.log_empty),
                Modifier.fillMaxWidth().padding(padding).padding(top = 36.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(filtered, key = { _, entry -> entry.id }) { index, entry ->
                    LogItem(entry, index, filtered.size) {
                        scope.launch {
                            clipboard.setClipEntry(ClipData.newPlainText(entry.tag, entry.message).toClipEntry())
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snackbar.showSnackbar(copied)
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(
            levels = levels,
            tag = tag,
            onLevelsChange = { levels = it },
            onTagChange = { tag = it },
            onDismiss = { showFilter = false },
        )
    }

    if (confirmClear) {
        val deleted = stringResource(R.string.log_deleted)
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.log_delete_title)) },
            text = { Text(stringResource(R.string.log_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        Logger.buffer.clear()
                        scope.launch { snackbar.showSnackbar(deleted) }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogItem(entry: LogEntry, index: Int, count: Int, onLongClick: () -> Unit) {
    SegmentedListItem(
        onClick = {},
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = listItemColors(),
        supportingContent = { Text(entry.message, modifier = Modifier.padding(top = 4.dp)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.level.name, style = MaterialTheme.typography.titleSmall, color = levelColor(entry.level))
            Text(entry.tag, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(formatDateTime(entry.time), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.DEBUG -> Color(0xFF9E9E9E)
    LogLevel.INFO -> Color(0xFF2196F3)
    LogLevel.WARNING -> Color(0xFFFFA000)
    LogLevel.ERROR -> Color(0xFFF44336)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    levels: Set<LogLevel>,
    tag: String,
    onLevelsChange: (Set<LogLevel>) -> Unit,
    onTagChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.log_filter), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.log_filter_level), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogLevel.entries.forEach { level ->
                    val selected = level in levels
                    FilterChip(
                        selected = selected,
                        onClick = { onLevelsChange(if (selected) levels - level else levels + level) },
                        label = { Text(level.name) },
                    )
                }
            }
            OutlinedTextField(
                value = tag,
                onValueChange = onTagChange,
                label = { Text(stringResource(R.string.log_filter_tag)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
