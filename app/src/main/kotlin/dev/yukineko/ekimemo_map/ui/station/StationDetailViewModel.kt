package dev.yukineko.ekimemo_map.ui.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.user.AccessLog
import dev.yukineko.ekimemo_map.data.user.AccessLogDao
import dev.yukineko.ekimemo_map.ui.LineProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class StationDetailViewModel(
    stationId: Int,
    stationDataRepository: StationDataRepository,
    accessLogDao: AccessLogDao,
    settings: Flow<Settings>,
) : ViewModel() {
    private val data = stationDataRepository.data

    val station: StateFlow<Station?> = data.map { it?.stations?.get(stationId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lines: StateFlow<List<Line>> = data
        .map { data -> data?.stations?.get(stationId)?.lines?.mapNotNull { data.lines[it] }.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accessLog: StateFlow<AccessLog?> = accessLogDao.observe(stationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lineProgress: StateFlow<Map<Int, LineProgress>> = lines
        .flatMapLatest { lines ->
            accessLogDao.observeByStationIds(lines.flatMap { it.stationList }.distinct()).map { logs ->
                val accessed = logs.filter { it.accessed }.mapTo(HashSet()) { it.stationId }
                lines.associate { line -> line.code to LineProgress(line.stationList.count { it in accessed }, line.stationList.size) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val debugMode: StateFlow<Boolean> = settings.map { it.isDebugMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    companion object {
        fun factory(stationId: Int) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                StationDetailViewModel(stationId, app.stationDataRepository, app.userDatabase.accessLogDao(), app.settings.data)
            }
        }
    }
}
