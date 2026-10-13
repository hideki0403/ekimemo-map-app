package dev.yukineko.ekimemo_map.data.update

import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AppUpdaterTest {
    private val server = MockWebServer()
    private lateinit var updater: AppUpdater

    @Before
    fun setUp() {
        server.start()
        updater = AppUpdater(GitHubReleases(OkHttpClient(), server.url("/api").toString()), "v2025.7.0")
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
}
