package dev.yukineko.ekimemo_map.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.user.AccessLog
import dev.yukineko.ekimemo_map.data.user.AccessLogDao
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StationDataState { LOADING, MISSING, READY }

enum class StationDataUpdateState { IDLE, RUNNING, FAILED }

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val accessLogDao: AccessLogDao,
    private val stationDataRepository: StationDataRepository,
    private val settingsFlow: Flow<Settings>,
) : ViewModel() {
    private val logger = Logger("HomeViewModel")

    val settings: StateFlow<Settings?> = settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stationDataState: StateFlow<StationDataState> =
        combine(stationDataRepository.data, stationDataRepository.loaded) { data, loaded ->
            when {
                data != null -> StationDataState.READY
                loaded -> StationDataState.MISSING
                else -> StationDataState.LOADING
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StationDataState.LOADING)

    val accessLogs: StateFlow<Map<Int, AccessLog>> = LocationService.searchResult
        .map { result -> result?.stations.orEmpty().map { it.station.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids -> accessLogDao.observeByStationIds(ids) }
        .map { logs -> logs.associateBy { it.stationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _stationDataUpdate = MutableStateFlow(StationDataUpdateState.IDLE)
    val stationDataUpdate: StateFlow<StationDataUpdateState> = _stationDataUpdate.asStateFlow()

    fun setAccessed(stationId: Int, accessed: Boolean) = viewModelScope.launch {
        runCatching { accessLogDao.setAccessed(stationId, accessed, settingsFlow.first()) }
            .onFailure { logger.error("Failed to update access log", it) }
    }

    fun startTimer(stationId: Int) = viewModelScope.launch {
        updateAccessLog(stationId, System.currentTimeMillis(), null)
    }

    fun resetTimer(stationId: Int) = viewModelScope.launch {
        updateAccessLog(stationId, System.currentTimeMillis() - settingsFlow.first().cooldownTime * 1000L, null)
    }

    private suspend fun updateAccessLog(stationId: Int, lastAccess: Long, accessed: Boolean?) {
        runCatching { accessLogDao.updateAccess(stationId, lastAccess, accessed) }
            .onFailure { logger.error("Failed to update access log", it) }
    }

    fun updateStationData() {
        if (_stationDataUpdate.value == StationDataUpdateState.RUNNING) return
        _stationDataUpdate.value = StationDataUpdateState.RUNNING
        viewModelScope.launch {
            runCatching { stationDataRepository.checkUpdate()?.let { stationDataRepository.update(it) } }
                .onSuccess { _stationDataUpdate.value = StationDataUpdateState.IDLE }
                .onFailure {
                    logger.error("Failed to update station data", it)
                    _stationDataUpdate.value = StationDataUpdateState.FAILED
                }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                HomeViewModel(app.userDatabase.accessLogDao(), app.stationDataRepository, app.settings.data)
            }
        }
    }
}
