package dev.yukineko.ekimemo_map.data.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException

data class ReleaseAsset(val version: String, val url: String, val size: Long)

class GitHubReleases(
    private val client: OkHttpClient,
    private val apiUrl: String = "https://api.github.com",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestAsset(repository: String, assetName: String): ReleaseAsset = withContext(Dispatchers.IO) {
        val release = get("$apiUrl/repos/$repository/releases/latest") { json.decodeFromString<Release>(it.string()) }
        val asset = release.assets.firstOrNull { it.name == assetName }
            ?: throw IOException("Asset not found: $assetName")
        ReleaseAsset(release.tagName, asset.browserDownloadUrl, asset.size)
    }

    suspend fun download(asset: ReleaseAsset, target: File, onProgress: (Long) -> Unit) = withContext(Dispatchers.IO) {
        get(asset.url) { body ->
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        onProgress(total)
                    }
                }
            }
        }
    }

    private fun <T> get(url: String, block: (ResponseBody) -> T): T =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            block(response.body)
        }

    @Serializable
    private class Release(
        @SerialName("tag_name") val tagName: String,
        val assets: List<Asset>,
    )

    @Serializable
    private class Asset(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
        val size: Long,
    )
}
