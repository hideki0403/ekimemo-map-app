package dev.yukineko.ekimemo_map.data.update

import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class AppUpdater(
    private val releases: GitHubReleases,
    private val currentVersion: String,
    private val downloadDir: File,
) {
    private val _available = MutableStateFlow<ReleaseAsset?>(null)
    val available: StateFlow<ReleaseAsset?> = _available.asStateFlow()

    suspend fun checkUpdate(): ReleaseAsset? {
        val latest = releases.latestAsset(REPOSITORY, ASSET_NAME)
        val update = latest.takeIf { it.version != currentVersion }
        if (update == null) withContext(Dispatchers.IO) { downloadDir.deleteRecursively() }
        _available.value = update
        return update
    }

    suspend fun download(asset: ReleaseAsset, onProgress: (Long) -> Unit): File = withContext(Dispatchers.IO) {
        val apk = File(downloadDir, "${asset.version}.apk")
        if (apk.length() == asset.size) return@withContext apk

        downloadDir.deleteRecursively()
        downloadDir.mkdirs()
        val temp = File(downloadDir, "download.tmp")
        try {
            releases.download(asset, temp, onProgress)
            check(temp.renameTo(apk)) { "Failed to save ${apk.name}" }
        } finally {
            temp.delete()
        }
        apk
    }

    companion object {
        private const val REPOSITORY = "hideki0403/ekimemo-map-app"
        private const val ASSET_NAME = "app-release.apk"
    }
}
