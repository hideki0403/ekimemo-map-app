package dev.yukineko.ekimemo_map.location

import android.location.Location
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.measureDistance
import dev.yukineko.ekimemo_map.data.user.MoveLog
import dev.yukineko.ekimemo_map.data.user.MoveLogDao
import dev.yukineko.ekimemo_map.data.user.MoveLogSession
import dev.yukineko.ekimemo_map.data.user.MoveLogSessionDao

class MoveLogRecorder(
    private val sessionDao: MoveLogSessionDao,
    private val moveLogDao: MoveLogDao,
) {
    private var sessionId: Long? = null
    private var lastLocation: Location? = null

    suspend fun record(location: Location, settings: Settings) {
        if (!settings.enableMovementLog) return
        val last = lastLocation
        if (last != null && measureDistance(last.latitude, last.longitude, location.latitude, location.longitude) < settings.minimumMovementDistance) return

        val now = System.currentTimeMillis()
        val id = sessionId ?: sessionDao.insert(MoveLogSession(startTime = now)).also { sessionId = it }
        moveLogDao.insert(
            MoveLog(
                sessionId = id,
                timestamp = now,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = location.speed.toDouble(),
                accuracy = location.accuracy.toDouble(),
            ),
        )
        lastLocation = location
    }
}
