package dev.yukineko.ekimemo_map.data.backup

import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.user.AccessLog

data class CsvImportResult(val logs: List<AccessLog>, val failedStations: List<String>)

fun parseAccessLogCsv(text: String, stations: Collection<Station>, now: Long): CsvImportResult {
    val byName = stations.groupBy { it.originalName }
    val byCode = stations.associateBy { it.code }
    val logs = mutableListOf<AccessLog>()
    val failedStations = mutableListOf<String>()

    text.removePrefix("﻿").lineSequence()
        .filter { !it.startsWith("#") && it.contains(',') }
        .forEach { line ->
            val values = line.split(',')
            val code = values[0].toIntOrNull()
            val candidates = byName[values[1]].orEmpty()
            val station = candidates.firstOrNull { it.code == code } ?: candidates.firstOrNull() ?: code?.let(byCode::get)
            if (station == null) {
                failedStations += values[1]
            } else {
                logs += AccessLog(station.id, firstAccess = now, lastAccess = now, accessCount = 1, accessed = true)
            }
        }

    return CsvImportResult(logs.distinctBy { it.stationId }, failedStations)
}
