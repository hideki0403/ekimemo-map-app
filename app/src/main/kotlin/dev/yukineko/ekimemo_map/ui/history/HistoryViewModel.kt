package dev.yukineko.ekimemo_map.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationData
import dev.yukineko.ekimemo_map.data.user.PassingLog
import dev.yukineko.ekimemo_map.data.user.PassingLogDao
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryItem(val log: PassingLog, val station: Station?)

class HistoryViewModel(private val passingLogDao: PassingLogDao, data: Flow<StationData?>) : ViewModel() {
    private val logger = Logger("HistoryViewModel")

    val items: StateFlow<List<HistoryItem>?> = combine(passingLogDao.observeAll(), data) { logs, data ->
        logs.map { HistoryItem(it, data?.stations?.get(it.stationId)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun deleteAll(): Boolean =
        runCatching { passingLogDao.deleteAll() }
            .onFailure { logger.error("Failed to clear passing logs", it) }
            .isSuccess

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                HistoryViewModel(app.userDatabase.passingLogDao(), app.stationDataRepository.data)
            }
        }
    }
}
