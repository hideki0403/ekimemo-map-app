package dev.yukineko.ekimemo_map.ui.timer

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.user.IntervalTimer
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.ui.BackButton
import dev.yukineko.ekimemo_map.ui.IconSwitch
import dev.yukineko.ekimemo_map.ui.InputDialog
import dev.yukineko.ekimemo_map.ui.SelectDialog
import dev.yukineko.ekimemo_map.ui.appBarColors
import dev.yukineko.ekimemo_map.ui.formatSeconds
import dev.yukineko.ekimemo_map.ui.soundName
import dev.yukineko.ekimemo_map.ui.vibrationName
import dev.yukineko.ekimemo_map.ui.listItemContainerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntervalTimerScreen(onBack: () -> Unit, viewModel: IntervalTimerViewModel = viewModel(factory = IntervalTimerViewModel.Factory)) {
    val items by viewModel.items.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun save(block: suspend () -> Boolean) = scope.launch {
        if (!block()) snackbar.showSnackbar(context.getString(R.string.interval_timer_save_failed))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.title_interval_timer)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { showHelp = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = stringResource(R.string.interval_timer_help_title))
                    }
                },
                colors = appBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { save(viewModel::add) }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.interval_timer_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = items
        when {
            current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            current.isEmpty() -> Text(
                stringResource(R.string.interval_timer_empty),
                Modifier.fillMaxWidth().padding(padding).padding(top = 36.dp),
                textAlign = TextAlign.Center,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(current, key = { it.timer.id }) { item ->
                    val timer = item.timer
                    TimerCard(
                        item = item,
                        onStart = { viewModel.start(timer) },
                        onPause = { viewModel.pause(timer) },
                        onStop = { viewModel.stop(timer) },
                        onReset = { viewModel.reset(timer) },
                        onUpdate = { save { viewModel.update(it) } },
                        onDurationChange = { save { viewModel.setDuration(timer, it) } },
                        onDelete = { save { viewModel.delete(timer) } },
                        onPreviewSound = viewModel::previewSound,
                        onPreviewVibration = viewModel::previewVibration,
                    )
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.interval_timer_help_title)) },
            text = { Text(stringResource(R.string.interval_timer_help_message)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

private enum class TimerDialog { NAME, DURATION, SOUND, VIBRATION, DELETE }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerCard(
    item: TimerItem,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onUpdate: (IntervalTimer) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onPreviewSound: (NotificationSound) -> Unit,
    onPreviewVibration: (VibrationPattern) -> Unit,
) {
    val timer = item.timer
    val state = item.state
    val remaining = state?.remaining ?: timer.durationSec
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<TimerDialog?>(null) }

    val running = state?.running == true
    val progress = remember { Animatable(remaining.toFloat() / timer.durationSec) }
    LaunchedEffect(state, timer.durationSec) {
        progress.snapTo(remaining.toFloat() / timer.durationSec)
        if (state != null && state.running) {
            val millis = (state.tickedAt + 1000 - SystemClock.elapsedRealtime()).coerceIn(0, 1000).toInt()
            progress.animateTo((remaining - 1).toFloat() / timer.durationSec, tween(millis, easing = LinearEasing))
        }
    }
    val startLabel = stringResource(R.string.interval_timer_start)
    val pauseLabel = stringResource(R.string.interval_timer_pause)
    val stopLabel = stringResource(R.string.interval_timer_stop)
    val resetLabel = stringResource(R.string.interval_timer_reset)

    Surface(shape = MaterialTheme.shapes.extraLarge, color = listItemContainerColor()) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timer.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).clickable { dialog = TimerDialog.NAME },
                )
                IconButton(onClick = { showSettings = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.interval_timer_settings))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    formatSeconds(remaining),
                    style = MaterialTheme.typography.displayMedium.copy(
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .alpha(blinkAlpha(state != null && !running))
                        .clickable { dialog = TimerDialog.DURATION },
                )
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (running) {
                        action(Icons.Rounded.Pause, pauseLabel, onPause, primary = true)
                    } else {
                        action(Icons.Rounded.PlayArrow, startLabel, onStart, primary = true)
                    }
                    action(Icons.Rounded.Stop, stopLabel, onStop, enabled = state != null)
                    action(Icons.Rounded.RestartAlt, resetLabel, onReset, enabled = state != null)
                }
            }
            LinearWavyProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 12.dp),
                amplitude = { if (running) 1f else 0f },
            )
        }
    }

    if (showSettings) {
        TimerSettingsSheet(
            timer = timer,
            onDismiss = { showSettings = false },
            onEdit = { dialog = it },
            onUpdate = onUpdate,
        )
    }

    val dismiss = { dialog = null }
    when (dialog) {
        TimerDialog.NAME -> InputDialog(
            title = stringResource(R.string.interval_timer_name),
            initial = timer.name,
            parse = { it.trim().ifEmpty { null } },
            onDismiss = dismiss,
            onConfirm = {
                dismiss()
                onUpdate(timer.copy(name = it))
            },
        )
        TimerDialog.DURATION -> InputDialog(
            title = stringResource(R.string.interval_timer_duration),
            initial = timer.durationSec.toString(),
            parse = { text -> text.toIntOrNull()?.takeIf { it >= 1 } },
            onDismiss = dismiss,
            onConfirm = {
                dismiss()
                onDurationChange(it)
            },
            suffix = stringResource(R.string.interval_timer_seconds),
            keyboardType = KeyboardType.Number,
        )
        TimerDialog.SOUND -> SelectDialog(
            title = stringResource(R.string.interval_timer_sound),
            options = NotificationSound.entries,
            selected = timer.notificationSound,
            label = { soundName(it) },
            onPreview = onPreviewSound,
            onDismiss = dismiss,
            onConfirm = {
                dismiss()
                onUpdate(timer.copy(notificationSound = it))
            },
            noneLabel = stringResource(R.string.interval_timer_off),
        )
        TimerDialog.VIBRATION -> SelectDialog(
            title = stringResource(R.string.interval_timer_vibration),
            options = VibrationPattern.entries,
            selected = timer.vibrationPattern,
            label = { vibrationName(it) },
            onPreview = onPreviewVibration,
            onDismiss = dismiss,
            onConfirm = {
                dismiss()
                onUpdate(timer.copy(vibrationPattern = it))
            },
            noneLabel = stringResource(R.string.interval_timer_off),
        )
        TimerDialog.DELETE -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.interval_timer_delete_message, timer.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dismiss()
                        onDelete()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = dismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
        )
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ButtonGroupScope.action(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
) = customItem(
    buttonGroupContent = {
        val interactionSource = remember { MutableInteractionSource() }
        val width = if (primary) IconButtonDefaults.IconButtonWidthOption.Wide else IconButtonDefaults.IconButtonWidthOption.Uniform
        val modifier = Modifier.animateWidth(interactionSource).size(IconButtonDefaults.smallContainerSize(width))
        val content = @Composable { Icon(icon, contentDescription = label, modifier = Modifier.size(IconButtonDefaults.smallIconSize)) }
        if (primary) {
            FilledIconButton(onClick = onClick, modifier = modifier, enabled = enabled, shapes = IconButtonDefaults.shapes(), interactionSource = interactionSource, content = content)
        } else {
            FilledTonalIconButton(onClick = onClick, modifier = modifier, enabled = enabled, shapes = IconButtonDefaults.shapes(), interactionSource = interactionSource, content = content)
        }
    },
    menuContent = { menu ->
        DropdownMenuItem(
            text = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            enabled = enabled,
            onClick = {
                onClick()
                menu.dismiss()
            },
        )
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerSettingsSheet(
    timer: IntervalTimer,
    onDismiss: () -> Unit,
    onEdit: (TimerDialog) -> Unit,
    onUpdate: (IntervalTimer) -> Unit,
) {
    val off = stringResource(R.string.interval_timer_off)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(timer.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp))
            SettingItem(0, 6, Icons.Rounded.Edit, stringResource(R.string.interval_timer_name), timer.name) { onEdit(TimerDialog.NAME) }
            SettingItem(1, 6, Icons.Rounded.Timer, stringResource(R.string.interval_timer_duration), formatSeconds(timer.durationSec)) {
                onEdit(TimerDialog.DURATION)
            }
            SettingItem(
                2, 6, Icons.Rounded.Notifications, stringResource(R.string.interval_timer_notification),
                trailing = { IconSwitch(timer.enableNotification, onCheckedChange = null) },
            ) { onUpdate(timer.copy(enableNotification = !timer.enableNotification)) }
            SettingItem(
                3, 6, Icons.Rounded.RecordVoiceOver, stringResource(R.string.interval_timer_tts),
                trailing = { IconSwitch(timer.enableTts, onCheckedChange = null) },
            ) { onUpdate(timer.copy(enableTts = !timer.enableTts)) }
            SettingItem(4, 6, Icons.Rounded.MusicNote, stringResource(R.string.interval_timer_sound), timer.notificationSound?.let { soundName(it) } ?: off) {
                onEdit(TimerDialog.SOUND)
            }
            SettingItem(5, 6, Icons.Rounded.Vibration, stringResource(R.string.interval_timer_vibration), timer.vibrationPattern?.let { vibrationName(it) } ?: off) {
                onEdit(TimerDialog.VIBRATION)
            }
            SettingItem(
                0, 1, Icons.Rounded.Delete, stringResource(R.string.delete),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            ) { onEdit(TimerDialog.DELETE) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    headline: String,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier,
        leadingContent = { Icon(icon, contentDescription = null, tint = color.takeOrElse { LocalContentColor.current }) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = trailing,
    ) {
        Text(headline, color = color)
    }
}

@Composable
private fun blinkAlpha(enabled: Boolean): Float {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(enabled) {
        visible = true
        while (enabled) {
            delay(500)
            visible = !visible
        }
    }
    return if (visible) 1f else 0.65f
}
