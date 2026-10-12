package dev.yukineko.ekimemo_map.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.yukineko.ekimemo_map.EkimemoMapApplication
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.Bounds
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationDataRepository
import dev.yukineko.ekimemo_map.data.station.StationTree
import dev.yukineko.ekimemo_map.data.station.measureDistance
import dev.yukineko.ekimemo_map.data.user.AccessLogDao
import dev.yukineko.ekimemo_map.location.LocationService
import dev.yukineko.ekimemo_map.location.NearbyStation
import dev.yukineko.ekimemo_map.location.selectLines
import dev.yukineko.ekimemo_map.log.Logger
import dev.yukineko.ekimemo_map.map.MapStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

sealed interface VisibleStations {
    data class Ready(val stations: List<Station>) : VisibleStations
    data object TooMany : VisibleStations
}

data class MapFocus(val station: Station?, val line: Line?, val stations: List<Station>?)

data class TappedPoint(val stations: List<NearbyStation>, val distanceFromCurrent: Int?)

class MapViewModel(
    stationId: Int?,
    lineId: Int?,
    private val accessLogDao: AccessLogDao,
    stationDataRepository: StationDataRepository,
    private val settings: Flow<Settings>,
) : ViewModel() {
    private val logger = Logger("MapViewModel")

    val mapStyle: StateFlow<MapStyle?> = settings.map { it.mapStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val trees = stationDataRepository.data.filterNotNull()
        .map { it to StationTree(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val focus: StateFlow<MapFocus?> = stationDataRepository.data.filterNotNull()
        .map { data ->
            val station = stationId?.let(data.stations::get)
            val line = lineId?.let(data.lines::get)
            val stations = station?.let(::listOf) ?: line?.stationList?.mapNotNull(data.stations::get)
            MapFocus(station, line, stations)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val bounds = MutableStateFlow<Bounds?>(null)

    val visibleStations: StateFlow<VisibleStations?> = combine(
        trees.filterNotNull(),
        bounds.filterNotNull(),
        settings.map { it.mapRenderingLimit }.distinctUntilChanged(),
    ) { (_, tree), bounds, limit ->
        val margin = min(max(bounds.north - bounds.south, bounds.east - bounds.west) * 0.5, 0.5)
        val stations = tree.inRect(bounds.north + margin, bounds.east + margin, bounds.south - margin, bounds.west - margin, limit)
        when {
            stations.size >= limit -> VisibleStations.TooMany
            stations.isEmpty() -> VisibleStations.Ready(tree.nearest((bounds.north + bounds.south) / 2, (bounds.east + bounds.west) / 2, 1))
            else -> VisibleStations.Ready(stations)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accessedIds: StateFlow<Set<Int>> = accessLogDao.observeAccessedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val nearbyIds: StateFlow<Set<Int>> = LocationService.searchResult
        .map { result -> result?.stations.orEmpty().mapTo(HashSet()) { it.station.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onCameraIdle(bounds: Bounds) {
        this.bounds.value = bounds
    }

    suspend fun tap(lat: Double, lng: Double): TappedPoint? {
        val (data, tree) = trees.value ?: return null
        val maxResults = settings.first().maxResults.coerceAtLeast(1)
        val stations = withContext(Dispatchers.Default) {
            val stations = tree.nearest(lat, lng, maxResults)
            stations.zip(selectLines(stations, data.lines)) { station, line ->
                NearbyStation(station, measureDistance(lat, lng, station.lat, station.lng), line)
            }
        }
        val location = LocationService.location.value
        val nearest = stations.first().station
        val distance = location?.let { measureDistance(it.latitude, it.longitude, nearest.lat, nearest.lng) }
        return TappedPoint(stations, distance)
    }

    fun nearestStation(lat: Double, lng: Double): Station? = trees.value?.second?.nearest(lat, lng, 1)?.first()

    suspend fun toggleAccessed(station: Station): Boolean? {
        val accessed = station.id !in accessedIds.value
        return runCatching { accessLogDao.setAccessed(station.id, accessed, settings.first()) }
            .onFailure { logger.error("Failed to update access log", it) }
            .map { accessed }
            .getOrNull()
    }

    companion object {
        fun factory(stationId: Int?, lineId: Int?) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as EkimemoMapApplication
                MapViewModel(stationId, lineId, app.userDatabase.accessLogDao(), app.stationDataRepository, app.settings.data)
            }
        }
    }
}
