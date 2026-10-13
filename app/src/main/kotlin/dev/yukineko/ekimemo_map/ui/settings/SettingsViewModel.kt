package dev.yukineko.ekimemo_map.ui.settings

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.backup.CsvImportResult
import dev.yukineko.ekimemo_map.data.backup.parseAccessLogCsv
import dev.yukineko.ekimemo_map.data.backup.readBackup
import dev.yukineko.ekimemo_map.data.backup.writeBackup
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.StationData
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.user.UserDatabase
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.NotificationPlayer
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class SettingsViewModel(
    private val store: DataStore<Settings>,
    private val player: NotificationPlayer,
    private val stationDataRepository: StationDataRepository,
    private val userDatabase: UserDatabase,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    private val logger = Logger("SettingsViewModel")

    val settings: StateFlow<Settings?> = store.data.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stationData: StateFlow<StationData?> = stationDataRepository.data

    val ttsAvailable: StateFlow<Boolean?> = flow { emit(player.isTtsAvailable()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            runCatching { store.updateData(transform) }
                .onFailure { logger.error("Failed to update settings", it) }
        }
    }

    fun previewSound(sound: NotificationSound) {
        viewModelScope.launch { player.playSound(sound, store.data.first().notificationSoundVolume) }
    }

    fun previewVibration(pattern: VibrationPattern) = player.vibrate(pattern)

    suspend fun backup(uri: Uri): Boolean =
        runCatching {
            val logs = userDatabase.accessLogDao().getAll()
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri, "wt")!!.use { writeBackup(it, logs, System.currentTimeMillis()) }
            }
        }
            .onSuccess { logger.info("Backup success: $uri") }
            .onFailure { logger.error("Failed to backup", it) }
            .isSuccess

    suspend fun restore(uri: Uri, clear: Boolean): Boolean =
        runCatching {
            val logs = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)!!.use { readBackup(it, ZoneId.systemDefault()) }
            }
            val dao = userDatabase.accessLogDao()
            if (clear) dao.replaceAll(logs) else dao.upsertAll(logs)
        }
            .onSuccess { logger.info("Restore success: $uri") }
            .onFailure { logger.error("Failed to restore", it) }
            .isSuccess

    suspend fun importCsv(uri: Uri, clear: Boolean): CsvImportOutcome {
        val name = withContext(Dispatchers.IO) {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }
        if (name?.endsWith(".csv", ignoreCase = true) != true) return CsvImportOutcome.UnsupportedFile

        return runCatching {
            val stations = checkNotNull(stationDataRepository.data.value) { "Station data is not loaded" }.stations.values
            val text = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            val result = parseAccessLogCsv(text, stations, System.currentTimeMillis())
            val dao = userDatabase.accessLogDao()
            if (clear) dao.replaceAll(result.logs) else dao.upsertAllKeepingCount(result.logs)
            result
        }
            .onSuccess { logger.info("Import success: ${it.logs.size} imported, ${it.failedStations.size} failed") }
            .onFailure { logger.error("Failed to import CSV", it) }
            .fold({ CsvImportOutcome.Imported(it) }, { CsvImportOutcome.Failed })
    }

    fun deleteStationData() {
        viewModelScope.launch {
            runCatching { stationDataRepository.delete() }
                .onSuccess { logger.info("Deleted station data") }
                .onFailure { logger.error("Failed to delete station data", it) }
        }
    }

    fun deleteDatabase() {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { userDatabase.clearAllTables() } }
                .onSuccess { logger.info("Deleted user database") }
                .onFailure { logger.error("Failed to delete user database", it) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                SettingsViewModel(app.settings, app.notificationPlayer, app.stationDataRepository, app.userDatabase, app.contentResolver)
            }
        }
    }
}

sealed interface CsvImportOutcome {
    data object UnsupportedFile : CsvImportOutcome
    data object Failed : CsvImportOutcome
    data class Imported(val result: CsvImportResult) : CsvImportOutcome
}
