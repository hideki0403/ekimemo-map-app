package dev.yukineko.ekimemo_map.location

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.yukineko.ekimemo_map.MainActivity
import dev.yukineko.ekimemo_map.notification.openAppIntent
import dev.yukineko.ekimemo_map.ui.permissionIssues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SearchTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        scope = MainScope().apply {
            launch { LocationService.isRunning.collect(::updateTile) }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
    }

    override fun onClick() {
        when {
            LocationService.isRunning.value -> LocationService.stop(this)
            permissionIssues().any { it.blocksSearch } -> openApp()
            else -> LocationService.start(this)
        }
    }

    private fun updateTile(running: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private fun openApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(openAppIntent())
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
