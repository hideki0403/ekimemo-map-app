package dev.yukineko.ekimemo_map.ui.line

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.user.AccessLogDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class LineDetailViewModel(
    lineId: Int,
    stationDataRepository: StationDataRepository,
    accessLogDao: AccessLogDao,
) : ViewModel() {
    private val data = stationDataRepository.data

    val line: StateFlow<Line?> = data.map { it?.lines?.get(lineId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stations: StateFlow<List<Station>> = data
        .map { data -> data?.lines?.get(lineId)?.stationList?.mapNotNull { data.stations[it] }.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accessedStationIds: StateFlow<Set<Int>> = line
        .flatMapLatest { line -> accessLogDao.observeByStationIds(line?.stationList.orEmpty()) }
        .map { logs -> logs.filter { it.accessed }.mapTo(HashSet()) { it.stationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    companion object {
        fun factory(lineId: Int) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                LineDetailViewModel(lineId, app.stationDataRepository, app.userDatabase.accessLogDao())
            }
        }
    }
}
