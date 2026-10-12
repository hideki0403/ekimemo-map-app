package dev.yukineko.ekimemo_map.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.station.formatDistance
import dev.yukineko.ekimemo_map.ui.StationAttrIcon
import dev.yukineko.ekimemo_map.ui.formatDateTime
import dev.yukineko.ekimemo_map.ui.navigation.StationDetailRoute
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigate: (Any) -> Unit, onBack: () -> Unit, viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)) {
    val items by viewModel.items.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        val deleted = stringResource(R.string.history_deleted)
        ClearDialog(
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                scope.launch { if (viewModel.deleteAll()) snackbar.showSnackbar(deleted) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = stringResource(R.string.history_delete_title))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Text(
                    stringResource(R.string.history_count, items?.size ?: 0),
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = items
        when {
            current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            current.isEmpty() -> Text(
                stringResource(R.string.history_empty),
                Modifier.fillMaxWidth().padding(padding).padding(top = 36.dp),
                textAlign = TextAlign.Center,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(current, key = { it.log.id }) { item ->
                    HistoryCard(item, onClick = { onNavigate(StationDetailRoute(item.log.stationId)) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    val log = item.log
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.station?.let { StationAttrIcon(it.attr) }
                Text(
                    item.station?.name ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.history_distance, formatDistance(log.distance)))
                Text(stringResource(R.string.history_accuracy, String.format(Locale.ROOT, "%.1fm", log.accuracy)))
                Text(stringResource(R.string.history_speed, String.format(Locale.ROOT, "%.1fkm/h", log.speed * 3.6)))
            }
            Text(formatDateTime(log.timestamp), Modifier.alpha(0.8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ClearDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_delete_title)) },
        text = { Text(stringResource(R.string.history_delete_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
