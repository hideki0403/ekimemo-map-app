package dev.yukineko.ekimemo_map.ui.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlin.time.measureTimedValue

data class SearchResult(val stations: List<Station>, val lines: List<Line>, val elapsedMillis: Long)

class SearchViewModel(data: Flow<StationData?>) : ViewModel() {
    val query = TextFieldState()

    val result: StateFlow<SearchResult?> = combine(snapshotFlow { query.text.toString() }, data) { query, data ->
        if (query.isEmpty() || data == null) return@combine null
        val (hits, elapsed) = measureTimedValue {
            data.stations.values.filter { query in it.originalName } to data.lines.values.filter { query in it.name }
        }
        SearchResult(hits.first, hits.second, elapsed.inWholeMilliseconds)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                SearchViewModel(app.stationDataRepository.data)
            }
        }
    }
}
