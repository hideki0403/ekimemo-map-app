package dev.yukineko.ekimemo_map.ui.timer

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.user.IntervalTimer
import dev.yukineko.ekimemo_map.data.user.IntervalTimerDao
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.NotificationPlayer
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.timer.IntervalTimerRunner
import dev.yukineko.ekimemo_map.timer.TimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerItem(val timer: IntervalTimer, val state: TimerState?)

class IntervalTimerViewModel(
    private val dao: IntervalTimerDao,
    private val runner: IntervalTimerRunner,
    private val settings: DataStore<Settings>,
    private val player: NotificationPlayer,
    private val newTimerName: String,
) : ViewModel() {
    private val logger = Logger("IntervalTimerViewModel")

    val items: StateFlow<List<TimerItem>?> = combine(dao.observeAll(), runner.states) { timers, states ->
        timers.map { TimerItem(it, states[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun add(): Boolean = save("add") {
        val current = settings.data.first()
        dao.insert(
            IntervalTimer(
                name = newTimerName,
                durationSec = current.cooldownTime,
                enableNotification = current.enableNotification,
                enableTts = current.enableTts,
                notificationSound = current.notificationSound.takeIf { current.enableNotificationSound },
                vibrationPattern = current.vibrationPattern.takeIf { current.enableVibration },
            ),
        )
    }

    suspend fun update(timer: IntervalTimer): Boolean = save("update") { dao.update(timer) }

    suspend fun setDuration(timer: IntervalTimer, durationSec: Int): Boolean = save("update") {
        dao.update(timer.copy(durationSec = durationSec))
        runner.reset(timer.id, durationSec)
    }

    suspend fun delete(timer: IntervalTimer): Boolean = save("delete") {
        runner.stop(timer.id)
        dao.delete(timer)
    }

    fun start(timer: IntervalTimer) = runner.start(timer)

    fun pause(timer: IntervalTimer) = runner.pause(timer.id)

    fun stop(timer: IntervalTimer) = runner.stop(timer.id)

    fun reset(timer: IntervalTimer) = runner.reset(timer.id, timer.durationSec)

    fun previewSound(sound: NotificationSound) {
        viewModelScope.launch { player.playSound(sound, settings.data.first().notificationSoundVolume) }
    }

    fun previewVibration(pattern: VibrationPattern) = player.vibrate(pattern)

    private suspend fun save(action: String, block: suspend () -> Unit): Boolean =
        runCatching { block() }
            .onFailure { logger.error("Failed to $action interval timer", it) }
            .isSuccess

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                IntervalTimerViewModel(
                    app.userDatabase.intervalTimerDao(),
                    app.intervalTimerRunner,
                    app.settings,
                    app.notificationPlayer,
                    app.getString(R.string.interval_timer_new_name),
                )
            }
        }
    }
}
