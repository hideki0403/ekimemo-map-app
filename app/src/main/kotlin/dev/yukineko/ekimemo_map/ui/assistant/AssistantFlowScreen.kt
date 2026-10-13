package dev.yukineko.ekimemo_map.ui.assistant

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.assistant.AssistantFlowItem
import dev.yukineko.ekimemo_map.assistant.decodeAssistantFlow
import dev.yukineko.ekimemo_map.assistant.encodeAssistantFlow
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.InputDialog
import dev.yukineko.ekimemo_map.ui.ToolAction
import dev.yukineko.ekimemo_map.ui.ToolsFabMenu
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.listItemColors
import dev.yukineko.ekimemo_map.ui.navigation.AssistantChooseRectRoute
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class FlowDialog { WAIT, WAIT_RANDOM, IMPORT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AssistantFlowScreen(
    onNavigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: AssistantFlowViewModel = viewModel(factory = AssistantFlowViewModel.Factory),
) {
    val entries by viewModel.entries.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var dialog by rememberSaveable { mutableStateOf<FlowDialog?>(null) }
    var deleting by remember { mutableStateOf<FlowEntry?>(null) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to -> viewModel.move(from.index, to.index) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun save(successMessage: Int? = null, block: suspend () -> Boolean) = scope.launch {
        val message = if (block()) successMessage else R.string.assistant_flow_save_failed
        message?.let { snackbar.showSnackbar(context.getString(it)) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_assistant_flow)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(
                        onClick = {
                            val current = entries ?: return@IconButton
                            scope.launch {
                                val code = encodeAssistantFlow(current.map(FlowEntry::item))
                                clipboard.setClipEntry(ClipData.newPlainText(null, code).toClipEntry())
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    snackbar.showSnackbar(context.getString(R.string.log_copied))
                                }
                            }
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = stringResource(R.string.assistant_flow_export))
                    }
                    IconButton(onClick = { dialog = FlowDialog.IMPORT }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.assistant_flow_import))
                    }
                },
                colors = appBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ToolsFabMenu(
                actions = listOf(
                    ToolAction(R.string.assistant_flow_tap, Icons.Rounded.TouchApp) { onNavigate(AssistantChooseRectRoute(point = true)) },
                    ToolAction(R.string.assistant_flow_tap_rect, Icons.Rounded.CropFree) { onNavigate(AssistantChooseRectRoute(point = false)) },
                    ToolAction(R.string.assistant_flow_wait, Icons.Rounded.HourglassEmpty) { dialog = FlowDialog.WAIT },
                    ToolAction(R.string.assistant_flow_wait_random, Icons.Rounded.Shuffle) { dialog = FlowDialog.WAIT_RANDOM },
                ),
                icon = Icons.Rounded.Add,
                description = R.string.assistant_flow_add,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = entries
        when {
            current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            current.isEmpty() -> Text(
                stringResource(R.string.assistant_flow_empty),
                Modifier.fillMaxWidth().padding(padding).padding(top = 36.dp),
                textAlign = TextAlign.Center,
            )
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(current, key = { _, entry -> entry.key }) { index, entry ->
                    ReorderableItem(reorderState, key = entry.key) { dragging ->
                        val onDragStopped: () -> Unit = { save { viewModel.commit() } }
                        FlowItem(
                            item = entry.item,
                            index = index,
                            count = current.size,
                            dragging = dragging,
                            modifier = Modifier.longPressDraggableHandle(onDragStopped = onDragStopped),
                            handleModifier = Modifier.draggableHandle(onDragStopped = onDragStopped),
                            onDelete = { deleting = entry },
                        )
                    }
                }
            }
        }
    }

    val dismiss = { dialog = null }
    when (dialog) {
        FlowDialog.WAIT, FlowDialog.WAIT_RANDOM -> {
            val random = dialog == FlowDialog.WAIT_RANDOM
            InputDialog(
                title = stringResource(if (random) R.string.assistant_flow_wait_random else R.string.assistant_flow_wait),
                initial = "",
                parse = { text -> text.toLongOrNull()?.takeIf { it >= 0 } },
                onDismiss = dismiss,
                onConfirm = {
                    dismiss()
                    save { viewModel.add(if (random) AssistantFlowItem.WaitRandom(it) else AssistantFlowItem.Wait(it)) }
                },
                suffix = "ms",
                keyboardType = KeyboardType.Number,
            )
        }
        FlowDialog.IMPORT -> InputDialog(
            title = stringResource(R.string.assistant_flow_import),
            initial = "",
            parse = ::decodeAssistantFlow,
            onDismiss = dismiss,
            onConfirm = {
                dismiss()
                save(R.string.assistant_flow_imported) { viewModel.replace(it) }
            },
            caption = stringResource(R.string.assistant_flow_import_caption),
        )
        null -> {}
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.assistant_flow_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = null
                        save { viewModel.remove(entry) }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FlowItem(
    item: AssistantFlowItem,
    index: Int,
    count: Int,
    dragging: Boolean,
    modifier: Modifier,
    handleModifier: Modifier,
    onDelete: () -> Unit,
) {
    val (icon, label, content) = when (item) {
        is AssistantFlowItem.Tap -> Triple(
            Icons.Rounded.TouchApp,
            R.string.assistant_flow_tap,
            item.position.run { "(${x.toInt()}, ${y.toInt()})" },
        )
        is AssistantFlowItem.TapRect -> Triple(
            Icons.Rounded.CropFree,
            R.string.assistant_flow_tap_rect,
            item.rect.run { "(${left.toInt()}, ${top.toInt()}) - (${right.toInt()}, ${bottom.toInt()})" },
        )
        is AssistantFlowItem.Wait -> Triple(
            Icons.Rounded.HourglassEmpty,
            R.string.assistant_flow_wait,
            stringResource(R.string.assistant_flow_millis, item.millis),
        )
        is AssistantFlowItem.WaitRandom -> Triple(
            Icons.Rounded.Shuffle,
            R.string.assistant_flow_wait_random,
            stringResource(R.string.assistant_flow_max_millis, item.maxMillis),
        )
    }

    val elevation = ListItemDefaults.elevation()
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = modifier,
        colors = listItemColors(),
        elevation = if (dragging) ListItemDefaults.elevation(elevation = elevation.draggedElevation) else elevation,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = { Text(content) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                }
                Icon(Icons.Rounded.DragHandle, contentDescription = null, modifier = handleModifier.padding(12.dp))
            }
        },
    ) {
        Text(stringResource(label))
    }
}
