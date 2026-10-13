package dev.yukineko.ekimemo_map.data.update

import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppUpdater(
    private val releases: GitHubReleases,
    private val currentVersion: String,
) {
    private val _available = MutableStateFlow<ReleaseAsset?>(null)
    val available: StateFlow<ReleaseAsset?> = _available.asStateFlow()

    suspend fun checkUpdate(): ReleaseAsset? {
        val latest = releases.latestAsset(REPOSITORY, ASSET_NAME)
        return latest.takeIf { it.version != currentVersion }.also { _available.value = it }
    }

    companion object {
        private const val REPOSITORY = "hideki0403/ekimemo-map-app"
        private const val ASSET_NAME = "app-release.apk"
    }
}
