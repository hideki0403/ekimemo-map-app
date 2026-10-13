package dev.yukineko.ekimemo_map.timer

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.user.IntervalTimer
import dev.yukineko.ekimemo_map.data.user.IntervalTimerDao
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.NotificationPlayer
import dev.yukineko.ekimemo_map.notification.openAppIntent
import dev.yukineko.ekimemo_map.ui.formatSeconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerState(val remaining: Int, val running: Boolean, val tickedAt: Long = 0)

class IntervalTimerRunner(
    private val context: Context,
    dao: IntervalTimerDao,
    private val settings: DataStore<Settings>,
    private val player: NotificationPlayer,
) {
    private val logger = Logger("IntervalTimerRunner")
    private val scope = MainScope()
    private val manager = NotificationManagerCompat.from(context)
    private val jobs = mutableMapOf<Long, Job>()

    private val timers: StateFlow<Map<Long, IntervalTimer>> = dao.observeAll()
        .map { timers -> timers.associateBy { it.id } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    private val _states = MutableStateFlow<Map<Long, TimerState>>(emptyMap())
    val states: StateFlow<Map<Long, TimerState>> = _states.asStateFlow()

    init {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.title_interval_timer))
            .setSound(null, null)
            .setVibrationEnabled(false)
            .build()
        manager.createNotificationChannel(channel)
    }

    fun start(timer: IntervalTimer) {
        if (jobs[timer.id]?.isActive == true) return
        val startedAt = SystemClock.elapsedRealtime()
        setState(timer.id) { (it ?: TimerState(timer.durationSec, false)).copy(running = true, tickedAt = startedAt) }
        IntervalTimerService.start(context)
        jobs[timer.id] = scope.launch {
            var next = startedAt
            while (true) {
                next += 1000
                delay(next - SystemClock.elapsedRealtime())
                tick(timers.value[timer.id] ?: break, next)
            }
            stop(timer.id)
        }
    }

    fun pause(id: Long) {
        jobs.remove(id)?.cancel()
        setState(id) { it?.copy(running = false) }
    }

    fun stop(id: Long) {
        jobs.remove(id)?.cancel()
        setState(id) { null }
    }

    fun reset(id: Long, durationSec: Int) {
        setState(id) { it?.copy(remaining = durationSec) }
    }

    private fun tick(timer: IntervalTimer, tickedAt: Long) {
        val remaining = (states.value[timer.id]?.remaining ?: timer.durationSec) - 1
        setState(timer.id) { it?.copy(remaining = if (remaining > 0) remaining else timer.durationSec, tickedAt = tickedAt) }
        if (remaining > 0) return
        scope.launch {
            runCatching { notify(timer) }.onFailure { logger.error("Failed to notify interval timer", it) }
        }
    }

    private fun setState(id: Long, transform: (TimerState?) -> TimerState?) {
        _states.update { states ->
            val state = transform(states[id])
            if (state == null) states - id else states + (id to state)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun notify(timer: IntervalTimer) {
        if (timer.enableNotification && manager.areNotificationsEnabled()) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.title_interval_timer))
                .setContentText(context.getString(R.string.interval_timer_finished, timer.name, formatSeconds(timer.durationSec)))
                .setContentIntent(context.openAppIntent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }
        val volume = settings.data.first().notificationSoundVolume
        timer.vibrationPattern?.let(player::vibrate)
        timer.notificationSound?.let { delay(player.playSound(it, volume) + 500L) }
        if (timer.enableTts) player.speak(context.getString(R.string.tts_interval_timer_finished, timer.name), volume)
    }

    companion object {
        private const val CHANNEL_ID = "interval_timer"
        private const val NOTIFICATION_ID = 3
    }
}
