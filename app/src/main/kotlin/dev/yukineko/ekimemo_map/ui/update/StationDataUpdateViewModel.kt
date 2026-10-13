package dev.yukineko.ekimemo_map.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StationDataUpdateState {
    data class Confirm(val asset: ReleaseAsset) : StationDataUpdateState
    data class Downloading(val asset: ReleaseAsset, val bytes: Long) : StationDataUpdateState
    data object Completed : StationDataUpdateState
    data object Failed : StationDataUpdateState
}

class StationDataUpdateViewModel(private val repository: StationDataRepository) : ViewModel() {
    private val logger = Logger("StationDataUpdate")
    private val _state = MutableStateFlow<StationDataUpdateState?>(null)
    val state: StateFlow<StationDataUpdateState?> = _state.asStateFlow()
    private var job: Job? = null

    fun confirm(asset: ReleaseAsset) {
        _state.value = StationDataUpdateState.Confirm(asset)
    }

    fun start(asset: ReleaseAsset) {
        _state.value = StationDataUpdateState.Downloading(asset, 0)
        job = viewModelScope.launch {
            runCatching { repository.update(asset, ::updateProgress) }
                .onSuccess {
                    logger.info("Updated station data to ${asset.version}")
                    _state.value = StationDataUpdateState.Completed
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    logger.error("Failed to update station data", it)
                    _state.value = StationDataUpdateState.Failed
                }
        }
    }

    private fun updateProgress(bytes: Long) {
        _state.update { if (it is StationDataUpdateState.Downloading) it.copy(bytes = bytes) else it }
    }

    fun cancel() {
        job?.cancel()
        dismiss()
    }

    fun dismiss() {
        _state.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                StationDataUpdateViewModel(app.stationDataRepository)
            }
        }
    }
}
