package dev.yukineko.ekimemo_map

import android.app.Application
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.legacy.LegacyDatabaseImporter
import dev.yukineko.ekimemo_map.data.legacy.LegacyImportResult
import dev.yukineko.ekimemo_map.data.settings.AssistantSettings
import dev.yukineko.ekimemo_map.data.settings.JsonSerializer
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.user.UserDatabase
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import kotlin.system.measureTimeMillis

private val Context.dataStore by preferencesDataStore(name = "settings")
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
        StationDataRepository(File(filesDir, "station_database.msgpack"), dataStore, gitHubReleases)
    }
    val settings get() = settingsStore
    val assistantSettings get() = assistantSettingsStore
    val userDatabase by lazy {
        Room.databaseBuilder(this, UserDatabase::class.java, "user.db").build()
    }

    lateinit var legacyImport: Deferred<LegacyImportResult>
        private set

    override fun onCreate() {
        super.onCreate()
        legacyImport = scope.async {
            val importer = LegacyDatabaseImporter(getDatabasePath("database.db"), userDatabase, assistantSettings)
            runCatching { importer.import() }
                .onSuccess { logger.info("Legacy database import: $it") }
                .onFailure { logger.error("Failed to import legacy database", it) }
                .getOrThrow()
        }
        scope.launch {
            runCatching { measureTimeMillis { stationDataRepository.load() } }
                .onSuccess { if (stationDataRepository.data.value != null) logger.info("Loaded station data in ${it}ms") }
                .onFailure { logger.error("Failed to load station data", it) }
        }
    }
}
