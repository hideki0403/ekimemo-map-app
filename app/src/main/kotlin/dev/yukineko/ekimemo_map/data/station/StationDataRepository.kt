package dev.yukineko.ekimemo_map.data.station

import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class StationDataRepository(
    private val file: File,
    private val releases: GitHubReleases,
) {
    private val mutex = Mutex()
    private val _data = MutableStateFlow<StationData?>(null)
    val data: StateFlow<StationData?> = _data.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val _available = MutableStateFlow<ReleaseAsset?>(null)
    val available: StateFlow<ReleaseAsset?> = _available.asStateFlow()

    suspend fun load() {
        mutex.withLock {
            try {
                if (file.exists()) _data.value = read(file)
            } finally {
                _loaded.value = true
            }
        }
    }

    suspend fun checkUpdate(): ReleaseAsset? {
        val latest = releases.latestAsset(REPOSITORY, ASSET_NAME)
        return latest.takeIf { _data.value?.version != it.version }.also { _available.value = it }
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
                _available.value = null
            } finally {
                temp.delete()
            }
        }
    }

    suspend fun delete() {
        mutex.withLock {
            withContext(Dispatchers.IO) { check(!file.exists() || file.delete()) { "Failed to delete ${file.name}" } }
            _data.value = null
        }
    }

    private suspend fun read(source: File) = withContext(Dispatchers.IO) {
        source.inputStream().buffered().use(::readStationData)
    }

    companion object {
        private const val REPOSITORY = "hideki0403/ekimemo-map-database"
        private const val ASSET_NAME = "station_database.msgpack"
    }
}
