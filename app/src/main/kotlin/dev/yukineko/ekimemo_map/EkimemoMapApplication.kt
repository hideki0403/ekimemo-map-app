package dev.yukineko.ekimemo_map

import android.app.Application
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.room.Room
import dev.yukineko.ekimemo_map.assistant.AssistantFlowRunner
import dev.yukineko.ekimemo_map.data.backup.AppDataBackup
import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.legacy.LegacyDatabaseImporter
import dev.yukineko.ekimemo_map.data.legacy.LegacyImportResult
import dev.yukineko.ekimemo_map.data.legacy.LegacyPreferencesImporter
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.data.settings.JsonSerializer
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.update.AppInstaller
import dev.yukineko.ekimemo_map.data.update.AppUpdater
import dev.yukineko.ekimemo_map.data.user.UserDatabase
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.notification.NotificationPlayer
import dev.yukineko.ekimemo_map.timer.IntervalTimerRunner
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import java.io.File
import kotlin.system.measureTimeMillis

private val Context.settingsStore by dataStore(
    "settings.json",
    JsonSerializer(Settings.serializer(), Settings()),
    ReplaceFileCorruptionHandler { Settings() },
)
private val Context.assistantSettingsStore by dataStore(
    "assistant_settings.json",
    JsonSerializer(AssistantSettings.serializer(), AssistantSettings()),
    ReplaceFileCorruptionHandler { AssistantSettings() },
)

class EkimemoMapApplication : Application() {
    private val logger = Logger("Application")
    private val scope = MainScope()

    val gitHubReleases by lazy { GitHubReleases(OkHttpClient()) }
    val stationDataRepository by lazy {
        StationDataRepository(File(filesDir, "station_database.msgpack"), gitHubReleases)
    }
    val appUpdater by lazy { AppUpdater(gitHubReleases, "v${BuildConfig.VERSION_NAME}", File(cacheDir, "app_update")) }
    val appInstaller by lazy { AppInstaller(this) }
    val settings get() = settingsStore
    val assistantSettings get() = assistantSettingsStore
    val assistantFlowRunner by lazy { AssistantFlowRunner(assistantSettings) }
    val userDatabase by lazy {
        Room.databaseBuilder(this, UserDatabase::class.java, UserDatabase.NAME).build()
    }
    val notificationPlayer by lazy { NotificationPlayer(this) }
    val intervalTimerRunner by lazy {
        IntervalTimerRunner(this, userDatabase.intervalTimerDao(), settings, notificationPlayer)
    }

    val legacyDatabaseDump = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        CrashActivity.install(this)
        runCatching { AppDataBackup.applyPendingRestore(this) }
            .onSuccess { if (it) logger.info("Restored user database") }
            .onFailure { logger.error("Failed to restore user database", it) }
        MapLibre.getInstance(this)
        scope.launch {
            runCatching { LegacyPreferencesImporter(this@EkimemoMapApplication, settings).import() }
                .onSuccess { if (it) logger.info("Imported legacy preferences") }
                .onFailure { logger.error("Failed to import legacy preferences", it) }
        }
        scope.launch {
            runCatching { LegacyDatabaseImporter(this@EkimemoMapApplication, userDatabase, assistantSettings).import() }
                .onSuccess {
                    logger.info("Legacy database import: $it")
                    if (it is LegacyImportResult.Discarded) legacyDatabaseDump.value = it.dumpPath
                }
                .onFailure { logger.error("Failed to import legacy database", it) }
        }
        scope.launch {
            runCatching { measureTimeMillis { stationDataRepository.load() } }
                .onSuccess { if (stationDataRepository.data.value != null) logger.info("Loaded station data in ${it}ms") }
                .onFailure { logger.error("Failed to load station data", it) }
            runCatching { stationDataRepository.checkUpdate() }
                .onSuccess { logger.info("Station data update: ${it?.version ?: "up to date"}") }
                .onFailure { logger.error("Failed to check station data update", it) }
        }
        if (!BuildConfig.DEBUG) {
            scope.launch {
                runCatching { appUpdater.checkUpdate() }
                    .onSuccess { logger.info("App update: ${it?.version ?: "up to date"}") }
                    .onFailure { logger.error("Failed to check app update", it) }
            }
        }
    }
}
