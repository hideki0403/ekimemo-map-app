package dev.yukineko.ekimemo_map.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yukineko.ekimemo_map.BuildConfig
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.backup.CsvImportResult
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.ui.update.AppUpdateDialog
import dev.yukineko.ekimemo_map.ui.update.StationDataUpdateDialog
import dev.yukineko.ekimemo_map.ui.update.StationDataUpdateViewModel
import dev.yukineko.ekimemo_map.ui.IconSwitch
import dev.yukineko.ekimemo_map.ui.InputDialog
import dev.yukineko.ekimemo_map.ui.PlaceholderScreen
import dev.yukineko.ekimemo_map.ui.SectionTitle
import dev.yukineko.ekimemo_map.ui.SelectDialog
import dev.yukineko.ekimemo_map.ui.listItemColors
import dev.yukineko.ekimemo_map.ui.navigation.AssistantFlowRoute
import dev.yukineko.ekimemo_map.ui.navigation.LicenseRoute
import dev.yukineko.ekimemo_map.ui.navigation.LogRoute
import dev.yukineko.ekimemo_map.ui.soundName
import dev.yukineko.ekimemo_map.ui.theme.AppFont
import dev.yukineko.ekimemo_map.ui.theme.ThemeMode
import dev.yukineko.ekimemo_map.ui.vibrationName
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class SettingsDialog {
    MAX_RESULTS, UPDATE_FREQUENCY, MAX_ACCURACY, COOLDOWN, SOUND, VOLUME, VIBRATION, MIN_DISTANCE,
    THEME_MODE, SEED_COLOR, FONT, MAP_RENDERING_LIMIT, RESTORE, IMPORT_CSV, DELETE_STATION_DATA, DELETE_DATABASE,
    DEBUG_PACKAGE_NAME,
}

private typealias SettingsItem = @Composable (ListItemShapes) -> Unit

@Composable
fun SettingsScreen(
    onNavigate: (Any) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    stationDataUpdateViewModel: StationDataUpdateViewModel = viewModel(factory = StationDataUpdateViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsState()
    val ttsAvailable by viewModel.ttsAvailable.collectAsState()
    val stationData by viewModel.stationData.collectAsState()
    val assistant by viewModel.assistantSettings.collectAsState()
    val assistantConnected by viewModel.assistantConnected.collectAsState()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    var appTitleTaps by rememberSaveable { mutableIntStateOf(0) }
    val update = viewModel::update

    val station = stringResource(R.string.settings_unit_station)
    val second = stringResource(R.string.settings_unit_second)
    val meter = stringResource(R.string.settings_unit_meter)
    val percent = stringResource(R.string.settings_unit_percent)
    val none = stringResource(R.string.settings_none)

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val backupSucceeded = stringResource(R.string.settings_backup_succeeded)
    val backupFailed = stringResource(R.string.settings_backup_failed)
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { snackbar.showSnackbar(if (viewModel.backup(uri)) backupSucceeded else backupFailed) }
    }
    var importClear by rememberSaveable { mutableStateOf(false) }
    val restoreSucceeded = stringResource(R.string.settings_restore_succeeded)
    val restoreFailed = stringResource(R.string.settings_restore_failed)
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { snackbar.showSnackbar(if (viewModel.restore(uri, importClear)) restoreSucceeded else restoreFailed) }
    }
    var csvImportResult by remember { mutableStateOf<CsvImportResult?>(null) }
    val importUnsupported = stringResource(R.string.settings_import_unsupported)
    val importFailed = stringResource(R.string.settings_import_failed)
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val outcome = viewModel.importCsv(uri, importClear)) {
                CsvImportOutcome.UnsupportedFile -> snackbar.showSnackbar(importUnsupported)
                CsvImportOutcome.Failed -> snackbar.showSnackbar(importFailed)
                is CsvImportOutcome.Imported -> csvImportResult = outcome.result
            }
        }
    }
    var checkingAppUpdate by remember { mutableStateOf(false) }
    var appUpdate by remember { mutableStateOf<ReleaseAsset?>(null) }
    val appUpToDate = stringResource(R.string.app_update_latest)
    val appUpdateFailed = stringResource(R.string.app_update_check_failed)
    fun checkAppUpdate() = scope.launch {
        checkingAppUpdate = true
        val result = viewModel.checkAppUpdate()
        checkingAppUpdate = false
        result
            .onSuccess { if (it == null) snackbar.showSnackbar(appUpToDate) else appUpdate = it }
            .onFailure { snackbar.showSnackbar(appUpdateFailed) }
    }
    var checkingStationData by remember { mutableStateOf(false) }
    val stationDataUpToDate = stringResource(R.string.station_data_latest)
    val stationDataCheckFailed = stringResource(R.string.station_data_update_check_failed)
    fun checkStationDataUpdate() = scope.launch {
        checkingStationData = true
        val result = viewModel.checkStationDataUpdate()
        checkingStationData = false
        result
            .onSuccess { if (it == null) snackbar.showSnackbar(stationDataUpToDate) else stationDataUpdateViewModel.confirm(it) }
            .onFailure { snackbar.showSnackbar(stationDataCheckFailed) }
    }

    PlaceholderScreen(R.string.title_settings, onBack, snackbarHost = { SnackbarHost(snackbar) }) {
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
        val dynamicColor = DynamicColorSupported && s.useDynamicColor
        SettingsSection(
            stringResource(R.string.settings_section_theme),
            listOfNotNull<SettingsItem>(
                { ClickItem(it, stringResource(R.string.settings_theme_mode), themeModeName(s.themeMode)) { dialog = SettingsDialog.THEME_MODE } },
                if (DynamicColorSupported) {
                    {
                        SwitchItem(it, stringResource(R.string.settings_dynamic_color), stringResource(R.string.settings_dynamic_color_summary), dynamicColor) { v ->
                            update { it.copy(useDynamicColor = v) }
                        }
                    }
                } else {
                    null
                },
                {
                    val summary = stringResource(if (dynamicColor) R.string.settings_seed_color_disabled else R.string.settings_seed_color_summary)
                    ClickItem(
                        it, stringResource(R.string.settings_seed_color), summary, enabled = !dynamicColor,
                        trailing = if (dynamicColor) null else ({ ColorSwatch(Color(s.themeSeedColor)) }),
                    ) { dialog = SettingsDialog.SEED_COLOR }
                },
                { ClickItem(it, stringResource(R.string.settings_font), fontName(s.font)) { dialog = SettingsDialog.FONT } },
            ),
        )
        SettingsSection(
            stringResource(R.string.settings_section_map),
            listOf(
                {
                    ClickItem(it, stringResource(R.string.settings_map_rendering_limit), "${s.mapRenderingLimit}$station") {
                        dialog = SettingsDialog.MAP_RENDERING_LIMIT
                    }
                },
            ),
        )
        SettingsSection(
            stringResource(R.string.settings_section_data),
            listOf(
                {
                    ClickItem(it, stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_summary)) {
                        backupLauncher.launch(backupFileName())
                    }
                },
                {
                    ClickItem(it, stringResource(R.string.settings_restore), stringResource(R.string.settings_restore_summary)) {
                        dialog = SettingsDialog.RESTORE
                    }
                },
                {
                    ClickItem(it, stringResource(R.string.settings_import), stringResource(R.string.settings_import_summary)) {
                        dialog = SettingsDialog.IMPORT_CSV
                    }
                },
            ),
        )
        SettingsSection(
            stringResource(R.string.settings_section_app),
            listOfNotNull<SettingsItem>(
                {
                    ClickItem(
                        it,
                        stringResource(R.string.settings_app_version),
                        "v${BuildConfig.VERSION_NAME}",
                        enabled = !BuildConfig.DEBUG && !checkingAppUpdate,
                        trailing = { UpdateCheckIcon(checkingAppUpdate) },
                    ) { checkAppUpdate() }
                },
                {
                    ClickItem(
                        it,
                        stringResource(R.string.settings_station_data_version),
                        stationData?.version ?: stringResource(R.string.unknown),
                        enabled = !checkingStationData,
                        trailing = { UpdateCheckIcon(checkingStationData) },
                    ) { checkStationDataUpdate() }
                },
                { ClickItem(it, stringResource(R.string.settings_license), null) { onNavigate(LicenseRoute) } },
                if (s.isDebugMode || appTitleTaps >= DEBUG_MODE_TAPS) {
                    {
                        val summary = if (BuildConfig.DEBUG) stringResource(R.string.settings_debug_mode_locked) else null
                        SwitchItem(it, stringResource(R.string.settings_debug_mode), summary, s.isDebugMode, enabled = !BuildConfig.DEBUG) { v ->
                            update { it.copy(enableDebugMode = v) }
                        }
                    }
                } else {
                    null
                },
            ),
            titleModifier = Modifier.clickable(interactionSource = null, indication = null) { appTitleTaps++ },
        )
        if (s.isDebugMode) {
            SettingsSection(
                stringResource(R.string.settings_section_dev_utilities),
                listOf(
                    { ClickItem(it, stringResource(R.string.settings_log_viewer), null) { onNavigate(LogRoute) } },
                    { ClickItem(it, stringResource(R.string.settings_delete_station_data), null) { dialog = SettingsDialog.DELETE_STATION_DATA } },
                    { ClickItem(it, stringResource(R.string.settings_delete_database), null) { dialog = SettingsDialog.DELETE_DATABASE } },
                ),
            )
            SettingsSection(
                stringResource(R.string.settings_section_state),
                listOf(
                    { InfoItem(it, stringResource(R.string.settings_commit_hash), BuildConfig.COMMIT_HASH) },
                    { InfoItem(it, stringResource(R.string.settings_build_number), BuildConfig.VERSION_CODE.toString()) },
                    { InfoItem(it, stringResource(R.string.settings_tree_root), stationData?.treeRoot?.toString() ?: stringResource(R.string.unknown)) },
                ),
            )
            val a = assistant
            if (assistantConnected && a != null) {
                SettingsSection(
                    stringResource(R.string.settings_section_assistant),
                    listOf(
                        {
                            ClickItem(it, stringResource(R.string.settings_debug_package_name), a.debugPackageName) {
                                dialog = SettingsDialog.DEBUG_PACKAGE_NAME
                            }
                        },
                        {
                            SwitchItem(it, stringResource(R.string.settings_enable_assistant_flow), null, a.enabled) { v ->
                                viewModel.updateAssistant { it.copy(enabled = v) }
                            }
                        },
                        { ClickItem(it, stringResource(R.string.settings_assistant_flow_editor), null) { onNavigate(AssistantFlowRoute) } },
                    ),
                )
            }
        }
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
            SettingsDialog.MAP_RENDERING_LIMIT -> NumberDialog(
                title = stringResource(R.string.settings_map_rendering_limit),
                icon = Icons.Rounded.Map,
                caption = stringResource(R.string.settings_map_rendering_limit_caption),
                initial = s.mapRenderingLimit,
                suffix = station,
                parse = { it.toIntOrNull()?.takeIf { v -> v >= 1 } },
                onDismiss = dismiss,
            ) { v -> update { it.copy(mapRenderingLimit = v) } }
            SettingsDialog.THEME_MODE -> SelectDialog(
                title = stringResource(R.string.settings_theme_mode),
                icon = Icons.Rounded.ColorLens,
                options = ThemeMode.entries,
                selected = s.themeMode,
                label = { themeModeName(it) },
                onPreview = {},
                onDismiss = dismiss,
                onConfirm = { v ->
                    dismiss()
                    v?.let { update { s -> s.copy(themeMode = it) } }
                },
            )
            SettingsDialog.FONT -> SelectDialog(
                title = stringResource(R.string.settings_font),
                icon = Icons.Rounded.FontDownload,
                options = AppFont.entries,
                selected = s.font,
                label = { fontName(it) },
                onPreview = {},
                onDismiss = dismiss,
                onConfirm = { v ->
                    dismiss()
                    v?.let { update { s -> s.copy(font = it) } }
                },
            )
            SettingsDialog.SEED_COLOR -> ColorPickerDialog(
                title = stringResource(R.string.settings_seed_color),
                selected = Color(s.themeSeedColor),
                onDismiss = dismiss,
            ) { color ->
                dismiss()
                update { it.copy(themeSeedColor = color.toArgb()) }
            }
            SettingsDialog.RESTORE -> ImportModeDialog(
                icon = Icons.Rounded.Restore,
                title = stringResource(R.string.settings_restore_title),
                message = stringResource(R.string.settings_restore_message),
                keepLabel = stringResource(R.string.settings_restore_keep),
                clearLabel = stringResource(R.string.settings_restore_clear),
                onDismiss = dismiss,
            ) { clear ->
                dismiss()
                importClear = clear
                restoreLauncher.launch(arrayOf("*/*"))
            }
            SettingsDialog.IMPORT_CSV -> ImportModeDialog(
                icon = Icons.Rounded.UploadFile,
                title = stringResource(R.string.settings_import_title),
                message = stringResource(R.string.settings_import_message),
                keepLabel = stringResource(R.string.settings_import_keep),
                clearLabel = stringResource(R.string.settings_import_clear),
                onDismiss = dismiss,
            ) { clear ->
                dismiss()
                importClear = clear
                csvLauncher.launch(arrayOf("*/*"))
            }
            SettingsDialog.DELETE_STATION_DATA -> ConfirmDialog(stringResource(R.string.settings_delete_station_data_message), dismiss) {
                dismiss()
                viewModel.deleteStationData()
            }
            SettingsDialog.DELETE_DATABASE -> ConfirmDialog(stringResource(R.string.settings_delete_database_message), dismiss) {
                dismiss()
                viewModel.deleteDatabase()
            }
            SettingsDialog.DEBUG_PACKAGE_NAME -> InputDialog(
                title = stringResource(R.string.settings_package_name),
                icon = Icons.Rounded.Apps,
                initial = assistant?.debugPackageName.orEmpty(),
                parse = { it.trim().ifEmpty { null } },
                onDismiss = dismiss,
                onConfirm = { v ->
                    dismiss()
                    viewModel.updateAssistant { it.copy(debugPackageName = v) }
                },
            )
            null -> {}
        }
        csvImportResult?.let { CsvImportResultDialog(it) { csvImportResult = null } }
        appUpdate?.let { AppUpdateDialog(it) { appUpdate = null } }
        StationDataUpdateDialog(stationDataUpdateViewModel)
    }
}

@Composable
private fun ConfirmDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(30.dp)) },
        title = { Text(stringResource(R.string.delete)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ImportModeDialog(
    icon: ImageVector,
    title: String,
    message: String,
    keepLabel: String,
    clearLabel: String,
    onDismiss: () -> Unit,
    onSelect: (clear: Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp)) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { onSelect(false) }, shapes = ButtonDefaults.shapes()) { Text(keepLabel) } },
        dismissButton = { TextButton(onClick = { onSelect(true) }, shapes = ButtonDefaults.shapes()) { Text(clearLabel) } },
    )
}

@Composable
private fun CsvImportResultDialog(result: CsvImportResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(30.dp)) },
        title = { Text(stringResource(R.string.settings_import)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_import_result, result.logs.size, result.failedStations.size))
                if (result.failedStations.isNotEmpty()) {
                    Text(stringResource(R.string.settings_import_failed_stations, result.failedStations.joinToString(", ")))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.ok)) } },
    )
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
private fun SettingsSection(title: String, items: List<SettingsItem>, titleModifier: Modifier = Modifier) {
    SectionTitle(title, titleModifier)
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        items.forEachIndexed { index, item -> item(ListItemDefaults.segmentedShapes(index, items.size)) }
    }
}

@Composable
private fun themeModeName(mode: ThemeMode) = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    },
)

@Composable
private fun fontName(font: AppFont) = stringResource(
    when (font) {
        AppFont.NOTO_SANS_JP -> R.string.font_noto_sans_jp
        AppFont.M_PLUS_1P -> R.string.font_m_plus_1p
        AppFont.M_PLUS_ROUNDED_1C -> R.string.font_m_plus_rounded_1c
        AppFont.BIZ_UD_GOTHIC -> R.string.font_biz_ud_gothic
        AppFont.KOSUGI_MARU -> R.string.font_kosugi_maru
        AppFont.ZEN_MARU_GOTHIC -> R.string.font_zen_maru_gothic
    },
)

@Composable
private fun ColorSwatch(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Box(modifier.background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
}

@Composable
private fun ColorPickerDialog(title: String, selected: Color, onDismiss: () -> Unit, onSelect: (Color) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.ColorLens, contentDescription = null, modifier = Modifier.size(30.dp)) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SeedColors.chunked(COLOR_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { color ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).clickable { onSelect(color) },
                            ) {
                                ColorSwatch(color, Modifier.fillMaxSize())
                                if (color == selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White)
                                }
                            }
                        }
                        repeat(COLOR_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val COLOR_COLUMNS = 5

private const val DEBUG_MODE_TAPS = 10

private const val BACKUP_MIME_TYPE = "application/octet-stream"

private fun backupFileName() = "ekimemo_map_backup_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.bin"

private val DynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val SeedColors = listOf(
    0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688,
    0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548,
).map { Color(it) }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateCheckIcon(checking: Boolean) {
    if (checking) {
        LoadingIndicator(Modifier.size(24.dp))
    } else {
        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.update_check))
    }
}

@Composable
private fun InfoItem(shapes: ListItemShapes, title: String, summary: String) {
    SegmentedListItem(
        shapes = shapes,
        colors = listItemColors(),
        supportingContent = { Text(summary) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
    }
}

@Composable
private fun ClickItem(
    shapes: ListItemShapes,
    title: String,
    summary: String?,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        enabled = enabled,
        colors = listItemColors(),
        trailingContent = trailing,
        supportingContent = summary?.let { { Text(it) } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
    }
}

@Composable
private fun SwitchItem(
    shapes: ListItemShapes,
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = shapes,
        enabled = enabled,
        colors = listItemColors(),
        trailingContent = { IconSwitch(checked, onCheckedChange = null, enabled = enabled) },
        supportingContent = summary?.let { { Text(it) } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
    }
}
