package dev.yukineko.ekimemo_map.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.ui.InputDialog
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.SelectDialog
import dev.yukineko.ekimemo_map.ui.listItemColors
import dev.yukineko.ekimemo_map.ui.soundName
import dev.yukineko.ekimemo_map.ui.vibrationName

private enum class SettingsDialog { MAX_RESULTS, UPDATE_FREQUENCY, MAX_ACCURACY, COOLDOWN, SOUND, VOLUME, VIBRATION, MIN_DISTANCE }

private typealias SettingsItem = @Composable (ListItemShapes) -> Unit

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val settings by viewModel.settings.collectAsState()
    val ttsAvailable by viewModel.ttsAvailable.collectAsState()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val update = viewModel::update

    val station = stringResource(R.string.settings_unit_station)
    val second = stringResource(R.string.settings_unit_second)
    val meter = stringResource(R.string.settings_unit_meter)
    val percent = stringResource(R.string.settings_unit_percent)
    val none = stringResource(R.string.settings_none)

    PlaceholderScreen(R.string.title_settings, onBack) {
        val s = settings ?: return@PlaceholderScreen

        SettingsSection(
            stringResource(R.string.settings_section_search),
            listOf(
                { ClickItem(it, stringResource(R.string.settings_max_results), "${s.maxResults}$station") { dialog = SettingsDialog.MAX_RESULTS } },
                { ClickItem(it, stringResource(R.string.settings_update_frequency), "${s.updateFrequency}$second") { dialog = SettingsDialog.UPDATE_FREQUENCY } },
                {
                    val accuracy = if (s.maxAcceptableAccuracy == 0) none else "${s.maxAcceptableAccuracy}$meter"
                    ClickItem(it, stringResource(R.string.settings_max_accuracy), accuracy) { dialog = SettingsDialog.MAX_ACCURACY }
                },
            ),
        )
        SettingsSection(
            stringResource(R.string.settings_section_notification),
            listOf(
                {
                    SwitchItem(it, stringResource(R.string.settings_enable_notification), stringResource(R.string.settings_enable_notification_summary), s.enableNotification) { v ->
                        update { it.copy(enableNotification = v) }
                    }
                },
                {
                    SwitchItem(it, stringResource(R.string.settings_enable_reminder), stringResource(R.string.settings_enable_reminder_summary), s.enableReminder) { v ->
                        update { it.copy(enableReminder = v) }
                    }
                },
                {
                    val duration = stringResource(R.string.settings_duration, s.cooldownTime / 60, s.cooldownTime % 60)
                    ClickItem(it, stringResource(R.string.settings_cooldown), duration) { dialog = SettingsDialog.COOLDOWN }
                },
                {
                    SwitchItem(
                        it, stringResource(R.string.settings_notify_during_cooldown),
                        stringResource(R.string.settings_notify_during_cooldown_summary), s.enableNotificationDuringCooldown,
                    ) { v -> update { it.copy(enableNotificationDuringCooldown = v) } }
                },
                {
                    SwitchItem(it, stringResource(R.string.settings_enable_sound), stringResource(R.string.settings_enable_sound_summary), s.enableNotificationSound) { v ->
                        update { it.copy(enableNotificationSound = v) }
                    }
                },
                { ClickItem(it, stringResource(R.string.settings_sound), soundName(s.notificationSound)) { dialog = SettingsDialog.SOUND } },
                { ClickItem(it, stringResource(R.string.settings_volume), "${s.notificationSoundVolume}$percent") { dialog = SettingsDialog.VOLUME } },
                {
                    SwitchItem(it, stringResource(R.string.settings_enable_vibration), stringResource(R.string.settings_enable_vibration_summary), s.enableVibration) { v ->
                        update { it.copy(enableVibration = v) }
                    }
                },
                { ClickItem(it, stringResource(R.string.settings_vibration_pattern), vibrationName(s.vibrationPattern)) { dialog = SettingsDialog.VIBRATION } },
                {
                    val summary = stringResource(if (ttsAvailable == false) R.string.settings_tts_unavailable else R.string.settings_tts_summary)
                    SwitchItem(it, stringResource(R.string.settings_tts), summary, s.enableTts, enabled = ttsAvailable == true) { v ->
                        update { it.copy(enableTts = v) }
                    }
                },
            ),
        )
        SettingsSection(
            stringResource(R.string.settings_section_move_log),
            listOf(
                {
                    SwitchItem(it, stringResource(R.string.settings_move_log), stringResource(R.string.settings_move_log_summary), s.enableMovementLog) { v ->
                        update { it.copy(enableMovementLog = v) }
                    }
                },
                {
                    val distance = if (s.minimumMovementDistance == 0) none else "${s.minimumMovementDistance}$meter"
                    ClickItem(it, stringResource(R.string.settings_min_distance), distance) { dialog = SettingsDialog.MIN_DISTANCE }
                },
            ),
        )
        Spacer(Modifier.height(24.dp))

        val dismiss = { dialog = null }
        when (dialog) {
            SettingsDialog.MAX_RESULTS -> NumberDialog(
                title = stringResource(R.string.settings_max_results),
                icon = Icons.Rounded.Search,
                caption = stringResource(R.string.settings_max_results_caption),
                initial = s.maxResults,
                suffix = station,
                parse = { it.toIntOrNull()?.takeIf { v -> v >= 1 } },
                onDismiss = dismiss,
            ) { v -> update { it.copy(maxResults = v) } }
            SettingsDialog.UPDATE_FREQUENCY -> NumberDialog(
                title = stringResource(R.string.settings_update_frequency),
                icon = Icons.Rounded.Update,
                caption = stringResource(R.string.settings_update_frequency_caption),
                initial = s.updateFrequency,
                suffix = second,
                parse = { it.toDoubleOrNull()?.takeIf { v -> v >= 1.0 } },
                onDismiss = dismiss,
                keyboardType = KeyboardType.Decimal,
            ) { v -> update { it.copy(updateFrequency = v) } }
            SettingsDialog.MAX_ACCURACY -> NumberDialog(
                title = stringResource(R.string.settings_max_accuracy),
                icon = Icons.Rounded.Place,
                caption = stringResource(R.string.settings_max_accuracy_caption),
                initial = s.maxAcceptableAccuracy,
                suffix = meter,
                parse = { it.toIntOrNull()?.takeIf { v -> v >= 0 } },
                onDismiss = dismiss,
            ) { v -> update { it.copy(maxAcceptableAccuracy = v) } }
            SettingsDialog.COOLDOWN -> NumberDialog(
                title = stringResource(R.string.settings_cooldown),
                icon = Icons.Rounded.Alarm,
                caption = stringResource(R.string.settings_cooldown_caption),
                initial = s.cooldownTime,
                suffix = second,
                parse = { it.toIntOrNull()?.takeIf { v -> v >= 10 } },
                onDismiss = dismiss,
            ) { v -> update { it.copy(cooldownTime = v) } }
            SettingsDialog.VOLUME -> NumberDialog(
                title = stringResource(R.string.settings_volume),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                caption = stringResource(R.string.settings_volume_caption),
                initial = s.notificationSoundVolume,
                suffix = percent,
                parse = { it.toIntOrNull()?.takeIf { v -> v in 0..100 } },
                onDismiss = dismiss,
            ) { v -> update { it.copy(notificationSoundVolume = v) } }
            SettingsDialog.MIN_DISTANCE -> NumberDialog(
                title = stringResource(R.string.settings_min_distance),
                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                caption = stringResource(R.string.settings_min_distance_caption),
                initial = s.minimumMovementDistance,
                suffix = meter,
                parse = { it.toIntOrNull()?.takeIf { v -> v >= 0 } },
                onDismiss = dismiss,
            ) { v -> update { it.copy(minimumMovementDistance = v) } }
            SettingsDialog.SOUND -> SelectDialog(
                title = stringResource(R.string.settings_sound),
                icon = Icons.Rounded.NotificationsActive,
                options = NotificationSound.entries,
                selected = s.notificationSound,
                label = { soundName(it) },
                onPreview = viewModel::previewSound,
                onDismiss = dismiss,
                onConfirm = { v ->
                    dismiss()
                    v?.let { update { s -> s.copy(notificationSound = it) } }
                },
            )
            SettingsDialog.VIBRATION -> SelectDialog(
                title = stringResource(R.string.settings_vibration_pattern),
                icon = Icons.Rounded.Vibration,
                options = VibrationPattern.entries,
                selected = s.vibrationPattern,
                label = { vibrationName(it) },
                onPreview = viewModel::previewVibration,
                onDismiss = dismiss,
                onConfirm = { v ->
                    dismiss()
                    v?.let { update { s -> s.copy(vibrationPattern = it) } }
                },
            )
            null -> {}
        }
    }
}

@Composable
private fun <T : Number> NumberDialog(
    title: String,
    icon: ImageVector,
    caption: String,
    initial: T,
    suffix: String,
    parse: (String) -> T?,
    onDismiss: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    onConfirm: (T) -> Unit,
) {
    InputDialog(
        title = title,
        icon = icon,
        initial = initial.toString(),
        parse = parse,
        onDismiss = onDismiss,
        onConfirm = {
            onDismiss()
            onConfirm(it)
        },
        suffix = suffix,
        caption = caption,
        keyboardType = keyboardType,
    )
}

@Composable
private fun SettingsSection(title: String, items: List<SettingsItem>) {
    SectionTitle(title)
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        items.forEachIndexed { index, item -> item(ListItemDefaults.segmentedShapes(index, items.size)) }
    }
}

@Composable
private fun ClickItem(shapes: ListItemShapes, title: String, summary: String, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = listItemColors(),
        supportingContent = { Text(summary) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
    }
}

@Composable
private fun SwitchItem(
    shapes: ListItemShapes,
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = shapes,
        enabled = enabled,
        colors = listItemColors(),
        trailingContent = { Switch(checked, onCheckedChange = null, enabled = enabled) },
        supportingContent = { Text(summary) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
    }
}
