package dev.yukineko.ekimemo_map.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.github.ReleaseAsset
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.update.AppInstaller
import dev.yukineko.ekimemo_map.data.update.AppUpdater
import dev.yukineko.ekimemo_map.data.update.InstallResult
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateTarget { APP, STATION_DATA }

sealed interface UpdateState {
    val target: UpdateTarget

    data class Confirm(override val target: UpdateTarget, val asset: ReleaseAsset) : UpdateState
    data class Downloading(override val target: UpdateTarget, val asset: ReleaseAsset, val bytes: Long) : UpdateState
    data class Installing(override val target: UpdateTarget) : UpdateState
    data class Completed(override val target: UpdateTarget) : UpdateState
    data class Failed(override val target: UpdateTarget) : UpdateState
}

class UpdateViewModel(
    private val appUpdater: AppUpdater,
    private val appInstaller: AppInstaller,
    private val stationDataRepository: StationDataRepository,
) : ViewModel() {
    private val logger = Logger("Update")
    private val _state = MutableStateFlow<UpdateState?>(null)
    val state: StateFlow<UpdateState?> = _state.asStateFlow()
    private var job: Job? = null

    fun confirm(target: UpdateTarget, asset: ReleaseAsset) {
        _state.value = UpdateState.Confirm(target, asset)
    }

    fun start(target: UpdateTarget, asset: ReleaseAsset) {
        _state.value = UpdateState.Downloading(target, asset, 0)
        job = viewModelScope.launch {
            runCatching { update(target, asset) }
                .onSuccess { _state.value = it }
                .onFailure {
                    if (it is CancellationException) {
                        _state.value = null
                        throw it
                    }
                    logger.error("Failed to update $target", it)
                    _state.value = UpdateState.Failed(target)
                }
        }
    }

    private suspend fun update(target: UpdateTarget, asset: ReleaseAsset): UpdateState? = when (target) {
        UpdateTarget.STATION_DATA -> {
            stationDataRepository.update(asset, ::updateProgress)
            logger.info("Updated station data to ${asset.version}")
            UpdateState.Completed(target)
        }
        UpdateTarget.APP -> {
            val apk = appUpdater.download(asset, ::updateProgress)
            _state.value = UpdateState.Installing(target)
            when (val result = appInstaller.install(apk)) {
                InstallResult.Success -> UpdateState.Completed(target)
                InstallResult.Aborted -> null
                is InstallResult.Failure -> {
                    logger.error("Failed to install app: status=${result.status}, message=${result.message}")
                    UpdateState.Failed(target)
                }
            }
        }
    }

    private fun updateProgress(bytes: Long) {
        _state.update { if (it is UpdateState.Downloading) it.copy(bytes = bytes) else it }
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
                UpdateViewModel(app.appUpdater, app.appInstaller, app.stationDataRepository)
            }
        }
    }
}
