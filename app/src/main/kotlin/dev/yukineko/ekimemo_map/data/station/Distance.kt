package dev.yukineko.ekimemo_map.data.station

import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS = 6378137.0

fun measureDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
    val radLat1 = PI * lat1 / 180
    val radLat2 = PI * lat2 / 180
    val sinLat = sin((radLat1 - radLat2) / 2)
    val sinLng = sin((PI * lng1 / 180 - PI * lng2 / 180) / 2)
    return (EARTH_RADIUS * 2 * asin(sqrt(sinLat * sinLat + cos(radLat1) * cos(radLat2) * sinLng * sinLng))).toInt()
}

fun formatDistance(distance: Int): String =
    if (distance < 1000) "${distance}m" else String.format(Locale.ROOT, "%.1fkm", distance / 1000.0)
