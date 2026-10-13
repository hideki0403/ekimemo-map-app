package dev.yukineko.ekimemo_map.data.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.File

sealed interface InstallResult {
    data object Success : InstallResult
    data object Aborted : InstallResult
    data class Failure(val status: Int, val message: String?) : InstallResult
}

class AppInstaller(private val context: Context) {
    suspend fun install(apk: File): InstallResult {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        return installer.openSession(sessionId).use { session ->
            try {
                withContext(Dispatchers.IO) {
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        apk.inputStream().use { it.copyTo(output) }
                        session.fsync(output)
                    }
                }
            } catch (e: Throwable) {
                session.abandon()
                throw e
            }
            val marker = relaunchMarker(context).apply { createNewFile() }
            commit(session, sessionId).also { if (it != InstallResult.Success) marker.delete() }
        }
    }

    private suspend fun commit(session: PackageInstaller.Session, sessionId: Int): InstallResult {
        val action = "${context.packageName}.INSTALL_RESULT"
        val results = Channel<Intent>(Channel.UNLIMITED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                results.trySend(intent)
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            val intent = Intent(action).setPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            session.commit(PendingIntent.getBroadcast(context, sessionId, intent, flags).intentSender)
            while (true) {
                val result = results.receive()
                when (val status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION ->
                        IntentCompat.getParcelableExtra(result, Intent.EXTRA_INTENT, Intent::class.java)
                            ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    PackageInstaller.STATUS_SUCCESS -> return InstallResult.Success
                    PackageInstaller.STATUS_FAILURE_ABORTED -> return InstallResult.Aborted
                    else -> return InstallResult.Failure(status, result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
                }
            }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    companion object {
        private fun relaunchMarker(context: Context) = File(context.noBackupFilesDir, "relaunch_after_update")

        fun consumeRelaunchRequest(context: Context): Boolean = relaunchMarker(context).delete()
    }
}
