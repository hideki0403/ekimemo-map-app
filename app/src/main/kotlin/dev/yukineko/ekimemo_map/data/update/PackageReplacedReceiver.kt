package dev.yukineko.ekimemo_map.data.update

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.yukineko.ekimemo_map.MainActivity
import dev.yukineko.ekimemo_map.R
import dev.yukineko.ekimemo_map.assistant.AssistantService
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.openAppIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PackageReplacedReceiver : BroadcastReceiver() {
    private val logger = Logger("PackageReplacedReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED || !AppInstaller.consumeRelaunchRequest(context)) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (canStartActivityFromBackground(context)) {
                    logger.info("Relaunching after update")
                    context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    notifyUpdated(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    // Overlay permission or BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS
    private suspend fun canStartActivityFromBackground(context: Context): Boolean = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> true
        Settings.canDrawOverlays(context) -> true
        isAssistantServiceEnabled(context) ->
            withTimeoutOrNull(ASSISTANT_BIND_TIMEOUT_MILLIS) { AssistantService.instance.first { it != null } } != null
        else -> false
    }

    private fun isAssistantServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val component = ComponentName(context, AssistantService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
    }

    @SuppressLint("MissingPermission")
    private fun notifyUpdated(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.notification_channel_app_update))
            .build()
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_update)
            .setContentTitle(context.getString(R.string.app_update_completed))
            .setContentText(context.getString(R.string.notification_app_updated))
            .setContentIntent(context.openAppIntent())
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "app_update"
        private const val NOTIFICATION_ID = 5
        private const val ASSISTANT_BIND_TIMEOUT_MILLIS = 5000L
    }
}
