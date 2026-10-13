package dev.yukineko.ekimemo_map.data.update

import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppUpdaterTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var downloadDir: File
    private lateinit var updater: AppUpdater

    @Before
    fun setUp() {
        server.start()
        downloadDir = File(folder.root, "app_update")
        updater = AppUpdater(GitHubReleases(OkHttpClient(), server.url("/api").toString()), "v2025.7.0", downloadDir)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueueRelease(version: String) {
        server.enqueue(
            MockResponse.Builder()
                .body("""{"tag_name":"$version","assets":[{"name":"app-release.apk","browser_download_url":"https://example.com/app.apk","size":1}]}""")
                .build(),
        )
    }

    private fun asset(version: String, bytes: ByteArray): ReleaseAsset {
        server.enqueue(MockResponse.Builder().body(Buffer().write(bytes)).build())
        return ReleaseAsset(version, server.url("/download/$version").toString(), bytes.size.toLong())
    }

    @Test
    fun checkUpdateReturnsNullWhenUpToDate() = runBlocking {
        enqueueRelease("v2025.7.0")

        assertNull(updater.checkUpdate())
        assertNull(updater.available.value)
        assertEquals("/api/repos/hideki0403/ekimemo-map-app/releases/latest", server.takeRequest().url.encodedPath)
    }

    @Test
    fun checkUpdateReturnsAssetWhenVersionDiffers() = runBlocking {
        enqueueRelease("v2025.8.0")

        val asset = updater.checkUpdate()

        assertEquals("v2025.8.0", asset?.version)
        assertEquals(asset, updater.available.value)
    }

    @Test
    fun checkUpdateClearsAvailableWhenUpToDate() = runBlocking {
        enqueueRelease("v2025.8.0")
        updater.checkUpdate()
        enqueueRelease("v2025.7.0")

        updater.checkUpdate()

        assertNull(updater.available.value)
    }

    @Test
    fun checkUpdateRemovesDownloadsWhenUpToDate() = runBlocking {
        updater.download(asset("v2025.7.0", byteArrayOf(1))) {}
        enqueueRelease("v2025.7.0")

        updater.checkUpdate()

        assertFalse(downloadDir.exists())
    }

    @Test
    fun downloadReusesDownloadedApk() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3)
        val asset = asset("v2025.8.0", bytes)

        val apk = updater.download(asset) {}
        val reused = updater.download(asset) {}

        assertArrayEquals(bytes, apk.readBytes())
        assertEquals(apk, reused)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun downloadRemovesOtherVersions() = runBlocking {
        val old = updater.download(asset("v2025.8.0", byteArrayOf(1))) {}

        val apk = updater.download(asset("v2025.9.0", byteArrayOf(2, 3))) {}

        assertFalse(old.exists())
        assertEquals(listOf(apk.name), downloadDir.list()?.toList())
    }
}
