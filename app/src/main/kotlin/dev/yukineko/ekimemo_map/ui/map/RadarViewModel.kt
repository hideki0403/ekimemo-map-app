package dev.yukineko.ekimemo_map.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.LatLng
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.station.radar
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RadarState(
    val station: Station? = null,
    val k: Int = 0,
    val polygons: List<List<LatLng>> = emptyList(),
    val computing: Boolean = true,
    val failed: Boolean = false,
)

class RadarViewModel(
    private val radarId: Int,
    private val stationDataRepository: StationDataRepository,
    settings: Flow<Settings>,
) : ViewModel() {
    private val logger = Logger("RadarViewModel")
    private var job: Job? = null

    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state.asStateFlow()

    init {
        viewModelScope.launch { compute(settings.first().maxResults.coerceAtLeast(1)) }
    }

    fun compute(k: Int) {
        job?.cancel()
        job = viewModelScope.launch {
            val data = stationDataRepository.data.filterNotNull().first()
            val station = data.stations[radarId]
            if (station == null) {
                logger.error("Station not found: $radarId")
                _state.value = RadarState(k = k, computing = false, failed = true)
                return@launch
            }
            _state.value = RadarState(station, k)
            data.radar(station, k)
                .onEach { polygon -> _state.update { it.copy(polygons = it.polygons + listOf(polygon)) } }
                .catch {
                    logger.error("Failed to compute radar", it)
                    _state.update { state -> state.copy(failed = true) }
                }
                .collect()
            _state.update { it.copy(computing = false) }
        }
    }

    companion object {
        fun factory(radarId: Int) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                RadarViewModel(radarId, app.stationDataRepository, app.settings.data)
            }
        }
    }
}
