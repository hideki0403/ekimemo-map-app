package dev.yukineko.ekimemo_map

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import kotlin.system.measureTimeMillis

private val Context.dataStore by preferencesDataStore(name = "settings")

class EkimemoMapApplication : Application() {
    private val logger = Logger("Application")
    private val scope = MainScope()

    val gitHubReleases by lazy { GitHubReleases(OkHttpClient()) }
    val stationDataRepository by lazy {
        StationDataRepository(File(filesDir, "station_database.msgpack"), dataStore, gitHubReleases)
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            runCatching { measureTimeMillis { stationDataRepository.load() } }
                .onSuccess { if (stationDataRepository.data.value != null) logger.info("Loaded station data in ${it}ms") }
                .onFailure { logger.error("Failed to load station data", it) }
        }
    }
}
