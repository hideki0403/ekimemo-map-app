package dev.yukineko.ekimemo_map.data.station

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class StationDataRepository(
    private val file: File,
    private val dataStore: DataStore<Preferences>,
    private val releases: GitHubReleases,
) {
    private val mutex = Mutex()
    private val _data = MutableStateFlow<StationData?>(null)
    val data: StateFlow<StationData?> = _data.asStateFlow()
    val version: Flow<String?> = dataStore.data.map { it[VERSION] }

    suspend fun load() {
        mutex.withLock {
            if (file.exists()) _data.value = read(file)
        }
    }

    suspend fun checkUpdate(): ReleaseAsset? {
        val latest = releases.latestAsset(REPOSITORY, ASSET_NAME)
        return latest.takeIf { _data.value == null || version.first() != it.version }
    }

    suspend fun update(asset: ReleaseAsset, onProgress: (Long) -> Unit = {}) {
        mutex.withLock {
            val temp = File(file.path + ".tmp")
            try {
                releases.download(asset, temp, onProgress)
                val data = read(temp)
                file.delete()
                check(temp.renameTo(file)) { "Failed to replace ${file.name}" }
                _data.value = data
                dataStore.edit { it[VERSION] = asset.version }
            } finally {
                temp.delete()
            }
        }
    }

    private suspend fun read(source: File) = withContext(Dispatchers.IO) {
        source.inputStream().buffered().use(::readStationData)
    }

    companion object {
        private const val REPOSITORY = "hideki0403/ekimemo-map-database"
        private const val ASSET_NAME = "station_database.msgpack"
        private val VERSION = stringPreferencesKey("station_data_version")
    }
}
