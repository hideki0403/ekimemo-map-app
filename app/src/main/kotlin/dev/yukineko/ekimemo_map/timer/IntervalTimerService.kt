package dev.yukineko.ekimemo_map.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.openAppIntent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IntervalTimerService : Service() {
    private val logger = Logger("IntervalTimerService")
    private val scope = MainScope()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notification_channel_interval_timer_running))
            .build()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.title_interval_timer))
            .setContentText(getString(R.string.notification_interval_timer_running))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

        runCatching {
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        }.onFailure {
            logger.error("Failed to start foreground service", it)
            stopSelf()
            return
        }

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ekimemo_map:interval_timer")
            .apply { acquire() }

        val runner = (application as EkimemoMapApplication).intervalTimerRunner
        scope.launch {
            runner.states.first { states -> states.values.none { it.running } }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "interval_timer_running"
        private const val NOTIFICATION_ID = 4

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, IntervalTimerService::class.java))
        }
    }
}
