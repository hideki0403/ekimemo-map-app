package dev.yukineko.ekimemo_map.data.station

import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StationDataRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var file: File
    private lateinit var repository: StationDataRepository

    @Before
    fun setUp() {
        server.start()
        file = File(folder.root, "station_database.msgpack")
        repository = createRepository()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun createRepository(): StationDataRepository {
        val releases = GitHubReleases(OkHttpClient(), server.url("/api").toString())
        return StationDataRepository(file, releases)
    }

    private fun stationData(version: String?, treeRoot: Int) = msgpack(
        mapOf(
            "version" to version,
            "station" to emptyList<Any>(),
            "line" to emptyList<Any>(),
            "tree" to mapOf("root" to treeRoot, "node_list" to emptyList<Any>()),
        ).filterValues { it != null },
    )

    private fun enqueueRelease(version: String) {
        val url = server.url("/download/$version")
        server.enqueue(
            MockResponse.Builder()
                .body("""{"tag_name":"$version","assets":[{"name":"station_database.msgpack","browser_download_url":"$url","size":1}]}""")
                .build(),
        )
    }

    private fun enqueueAsset(bytes: ByteArray) {
        server.enqueue(MockResponse.Builder().body(Buffer().write(bytes)).build())
    }

    private suspend fun installVersion(version: String, treeRoot: Int) {
        enqueueRelease(version)
        enqueueAsset(stationData(version, treeRoot))
        repository.update(repository.checkUpdate()!!)
    }

    @Test
    fun updateReplacesData() = runBlocking {
        installVersion("v1", treeRoot = 1)

        assertEquals(1, repository.data.value?.treeRoot)
        assertEquals("v1", repository.data.value?.version)
        assertFalse(File(file.path + ".tmp").exists())

        val reloaded = createRepository().apply { load() }
        assertEquals(1, reloaded.data.value?.treeRoot)
        assertEquals("v1", reloaded.data.value?.version)
    }

    @Test
    fun checkUpdateReturnsNullWhenUpToDate() = runBlocking {
        installVersion("v1", treeRoot = 1)
        enqueueRelease("v1")

        assertNull(repository.checkUpdate())
    }

    @Test
    fun checkUpdateReturnsAssetWhenDataHasNoVersion() = runBlocking {
        file.writeBytes(stationData(version = null, treeRoot = 1))
        repository.load()
        enqueueRelease("v1")

        assertNotNull(repository.checkUpdate())
    }

    @Test
    fun availableFollowsCheckAndUpdate() = runBlocking {
        installVersion("v1", treeRoot = 1)
        assertNull(repository.available.value)

        enqueueRelease("v2")
        val asset = repository.checkUpdate()!!
        assertEquals(asset, repository.available.value)

        enqueueAsset(stationData("v2", treeRoot = 2))
        repository.update(asset)
        assertNull(repository.available.value)
    }

    @Test
    fun invalidDownloadKeepsCurrentData() = runBlocking {
        installVersion("v1", treeRoot = 1)
        enqueueRelease("v2")
        enqueueAsset(byteArrayOf(1, 2, 3))
        val asset = repository.checkUpdate()!!

        assertThrows(Exception::class.java) { runBlocking { repository.update(asset) } }
        assertEquals("v1", repository.data.value?.version)
        assertFalse(File(file.path + ".tmp").exists())

        val reloaded = createRepository().apply { load() }
        assertEquals("v1", reloaded.data.value?.version)
    }

    @Test
    fun deleteRemovesData() = runBlocking {
        installVersion("v1", treeRoot = 1)

        repository.delete()

        assertNull(repository.data.value)
        assertFalse(file.exists())
        assertNull(createRepository().apply { load() }.data.value)
    }
}
