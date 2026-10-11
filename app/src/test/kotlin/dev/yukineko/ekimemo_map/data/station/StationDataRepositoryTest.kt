package dev.yukineko.ekimemo_map.data.station

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.yukineko.ekimemo_map.data.github.GitHubReleases
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create { File(folder.root, "settings.preferences_pb") }
    }

    private fun createRepository(): StationDataRepository {
        val releases = GitHubReleases(OkHttpClient(), server.url("/api").toString())
        return StationDataRepository(file, dataStore, releases)
    }

    private fun stationData(treeRoot: Int) = msgpack(
        mapOf(
            "station" to emptyList<Any>(),
            "line" to emptyList<Any>(),
            "tree" to mapOf("root" to treeRoot, "node_list" to emptyList<Any>()),
        ),
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
        enqueueAsset(stationData(treeRoot))
        repository.update(repository.checkUpdate()!!)
    }

    @Test
    fun updateReplacesDataAndSavesVersion() = runBlocking {
        installVersion("v1", treeRoot = 1)

        assertEquals(1, repository.data.value?.treeRoot)
        assertEquals("v1", repository.version.first())
        assertFalse(File(file.path + ".tmp").exists())

        val reloaded = createRepository().apply { load() }
        assertEquals(1, reloaded.data.value?.treeRoot)
    }

    @Test
    fun checkUpdateReturnsNullWhenUpToDate() = runBlocking {
        installVersion("v1", treeRoot = 1)
        enqueueRelease("v1")

        assertNull(repository.checkUpdate())
    }

    @Test
    fun invalidDownloadKeepsCurrentData() = runBlocking {
        installVersion("v1", treeRoot = 1)
        enqueueRelease("v2")
        enqueueAsset(byteArrayOf(1, 2, 3))
        val asset = repository.checkUpdate()!!

        assertThrows(Exception::class.java) { runBlocking { repository.update(asset) } }
        assertEquals(1, repository.data.value?.treeRoot)
        assertEquals("v1", repository.version.first())
        assertFalse(File(file.path + ".tmp").exists())

        val reloaded = createRepository().apply { load() }
        assertEquals(1, reloaded.data.value?.treeRoot)
    }
}
