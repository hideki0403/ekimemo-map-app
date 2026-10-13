package dev.yukineko.ekimemo_map.ui.route

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.gpx.readGpxTrackPoints
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationData
import dev.yukineko.ekimemo_map.data.station.StationTree
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.time.measureTimedValue

data class RouteStation(val station: Station, val line: Line?)

data class RouteSearchResult(val fileName: String, val pointCount: Int, val stations: List<RouteStation>, val elapsedMillis: Long)

class RouteSearchViewModel(private val contentResolver: ContentResolver, private val data: StateFlow<StationData?>) : ViewModel() {
    private val logger = Logger("RouteSearchViewModel")

    private val _result = MutableStateFlow<RouteSearchResult?>(null)
    val result: StateFlow<RouteSearchResult?> = _result.asStateFlow()

    suspend fun search(uri: Uri): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val points = contentResolver.openInputStream(uri)!!.use(::readGpxTrackPoints)
            val data = data.value ?: error("Station data is not loaded")
            val tree = StationTree(data)
            val (stations, elapsed) = measureTimedValue {
                points.map { tree.nearest(it.lat, it.lng, 1).first() }
                    .distinctBy { it.id }
                    .map { RouteStation(it, it.lines.firstOrNull()?.let(data.lines::get)) }
            }
            RouteSearchResult(displayName(uri), points.size, stations, elapsed.inWholeMilliseconds)
        }
            .onSuccess { _result.value = it }
            .onFailure { logger.error("Failed to search route", it) }
            .isSuccess
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                RouteSearchViewModel(app.contentResolver, app.stationDataRepository.data)
            }
        }
    }
}
