package dev.yukineko.ekimemo_map.ui.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.NotificationPlayer
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: DataStore<Settings>,
    private val player: NotificationPlayer,
) : ViewModel() {
    private val logger = Logger("SettingsViewModel")

    val settings: StateFlow<Settings?> = store.data.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                SettingsViewModel(app.settings, app.notificationPlayer)
            }
        }
    }
}
