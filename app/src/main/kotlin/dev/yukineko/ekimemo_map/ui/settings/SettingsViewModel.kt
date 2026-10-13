package dev.yukineko.ekimemo_map.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
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
