package dev.yukineko.ekimemo_map.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.yukineko.ekimemo_map.MainActivity
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.formatDistance
import dev.yukineko.ekimemo_map.location.NearbyStation

fun Context.openAppIntent(): PendingIntent =
    PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

class StationNotifier(private val context: Context, private val notificationPlayer: NotificationPlayer) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.notification_channel_nearest_station))
            .setSound(null, null)
            .setVibrationEnabled(false)
            .build()
        manager.createNotificationChannel(channel)
    }

    suspend fun notifyNearest(nearest: NearbyStation, silent: Boolean, settings: Settings) = notify(
        icon = R.drawable.ic_notification_new,
        title = nearest.station.name,
        text = context.getString(R.string.notification_nearest_changed, formatDistance(nearest.distance)),
        ttsText = context.getString(R.string.tts_nearest_changed, nearest.station.nameKana),
        silent = silent,
        settings = settings,
    )

    suspend fun notifyReminder(station: Station, settings: Settings) {
        val minutes = settings.cooldownTime / 60
        val seconds = settings.cooldownTime % 60
        notify(
            icon = R.drawable.ic_notification_remind,
            title = station.name,
            text = context.getString(R.string.notification_reminder, minutes, seconds),
            ttsText = context.getString(R.string.tts_reminder, station.nameKana, minutes, seconds),
            silent = false,
            settings = settings,
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun notify(
        @DrawableRes icon: Int,
        title: String,
        text: String,
        ttsText: String,
        silent: Boolean,
        settings: Settings,
    ) {
        if (!settings.enableNotification) return

        if (manager.areNotificationsEnabled()) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(context.openAppIntent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(silent)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }

        if (!silent) notificationPlayer.playAll(settings, ttsText)
    }

    companion object {
        private const val CHANNEL_ID = "nearest_station"
        private const val NOTIFICATION_ID = 2
    }
}
