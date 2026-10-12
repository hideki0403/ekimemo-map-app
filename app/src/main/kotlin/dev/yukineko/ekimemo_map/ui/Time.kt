package dev.yukineko.ekimemo_map.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import dev.yukineko.ekimemo_map.R
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(1000)
        value = System.currentTimeMillis()
    }
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

fun formatDateTime(epochMillis: Long): String = dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatSeconds(seconds: Int): String = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)

@Composable
fun relativeTime(time: Long?, now: Long): String {
    if (time == null) return stringResource(R.string.unknown)
    val seconds = ((now - time) / 1000).coerceAtLeast(0).toInt()
    return when {
        seconds < 60 -> stringResource(R.string.seconds_ago, seconds)
        seconds < 3600 -> stringResource(R.string.minutes_ago, seconds / 60)
        seconds < 86400 -> stringResource(R.string.hours_ago, seconds / 3600)
        else -> stringResource(R.string.over_a_day_ago)
    }
}
