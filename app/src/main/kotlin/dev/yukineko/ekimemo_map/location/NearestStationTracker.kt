package dev.yukineko.ekimemo_map.location

import android.location.Location
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationData
import dev.yukineko.ekimemo_map.data.station.StationTree
import dev.yukineko.ekimemo_map.data.station.measureDistance
import dev.yukineko.ekimemo_map.data.user.AccessLog
import dev.yukineko.ekimemo_map.data.user.AccessLogDao
import dev.yukineko.ekimemo_map.data.user.PassingLog
import dev.yukineko.ekimemo_map.data.user.PassingLogDao
import dev.yukineko.ekimemo_map.log.Logger
import kotlin.math.roundToLong

data class NearbyStation(val station: Station, val distance: Int, val line: Line?)

data class TrackResult(val stations: List<NearbyStation>, val changed: Boolean, val inCooldown: Boolean)

fun cooldownSeconds(lastAccess: Long?, settings: Settings, now: Long): Int {
    if (lastAccess == null || !settings.enableReminder) return 0
    val elapsed = ((now - lastAccess) / 1000).toInt()
    return (settings.cooldownTime - elapsed).coerceAtLeast(0)
}

fun selectLines(stations: List<Station>, lines: Map<Int, Line>): List<Line?> {
    val ranking = stations.flatMap { it.lines }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.map { it.key }
    val masterLineId = ranking.firstOrNull { it in stations.first().lines }
    return stations.map { station ->
        val lineId = masterLineId?.takeIf { it in station.lines } ?: ranking.firstOrNull { it in station.lines }
        lineId?.let(lines::get)
    }
}

class NearestStationTracker(
    private val accessLogDao: AccessLogDao,
    private val passingLogDao: PassingLogDao,
) {
    private val logger = Logger("NearestStationTracker")
    private var lastPosition: Pair<Long, Long>? = null
    private var nearest: Station? = null

    suspend fun update(data: StationData, tree: StationTree, location: Location, settings: Settings): TrackResult? {
        if (settings.maxAcceptableAccuracy != 0 && settings.maxAcceptableAccuracy < location.accuracy) return null
        if (settings.maxResults <= 0) return null

        val position = location.latitude.fixed() to location.longitude.fixed()
        if (position == lastPosition) return null
        lastPosition = position

        val stations = tree.nearest(location.latitude, location.longitude, settings.maxResults)
        val result = stations.zip(selectLines(stations, data.lines)) { station, line ->
            NearbyStation(station, measureDistance(location.latitude, location.longitude, station.lat, station.lng), line)
        }

        val current = result.first()
        if (current.station.id == nearest?.id) return TrackResult(result, changed = false, inCooldown = false)

        nearest = current.station
        logger.info("Nearest station changed: ${current.station.name} (${current.station.id})")
        val inCooldown = record(current, location, settings)
        return TrackResult(result, changed = true, inCooldown = inCooldown)
    }

    private suspend fun record(current: NearbyStation, location: Location, settings: Settings): Boolean {
        val now = System.currentTimeMillis()
        val stationId = current.station.id
        val log = accessLogDao.get(stationId)
        val inCooldown = cooldownSeconds(log?.lastAccess, settings, now) > 0
        if (!inCooldown) {
            accessLogDao.upsert(
                log?.copy(lastAccess = now, accessCount = log.accessCount + 1)
                    ?: AccessLog(stationId, firstAccess = now, lastAccess = now, accessCount = 1, accessed = true),
            )
        }
        insertPassingLog(current.station, location, now, isRenotify = false)
        return inCooldown
    }

    suspend fun recordReminder(station: Station, location: Location?) {
        val now = System.currentTimeMillis()
        if (location != null) insertPassingLog(station, location, now, isRenotify = true)
        accessLogDao.get(station.id)?.let { accessLogDao.upsert(it.copy(lastAccess = now)) }
    }

    private suspend fun insertPassingLog(station: Station, location: Location, now: Long, isRenotify: Boolean) {
        passingLogDao.insert(
            PassingLog(
                stationId = station.id,
                timestamp = now,
                latitude = location.latitude,
                longitude = location.longitude,
                speed = location.speed.toDouble(),
                accuracy = location.accuracy.toDouble(),
                distance = measureDistance(location.latitude, location.longitude, station.lat, station.lng),
                isRenotify = isRenotify,
            ),
        )
    }

    private fun Double.fixed() = (this * 100000).roundToLong()
}
