package dev.yukineko.ekimemo_map.data.backup

import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationAttr
import dev.yukineko.ekimemo_map.data.user.AccessLog
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvImportTest {
    private fun station(id: Int, code: Int, originalName: String) = Station(
        code = code, id = id, name = originalName, originalName = originalName, nameKana = "", closed = false,
        lat = 0.0, lng = 0.0, prefecture = 13, lines = emptyList(), attr = StationAttr.UNKNOWN,
        postalCode = "", address = "", openDate = null, voronoi = emptyList(), delaunay = emptyList(),
    )

    private val stations = listOf(
        station(id = 1, code = 1001, originalName = "東京"),
        station(id = 2, code = 1002, originalName = "新宿"),
        station(id = 3, code = 1003, originalName = "渋谷"),
    )

    private fun log(id: Int) = AccessLog(id, firstAccess = NOW, lastAccess = NOW, accessCount = 1, accessed = true)

    @Test
    fun matchesByNameThenCode() {
        val csv = "﻿# code,name\r\n9999,東京\r\n1002,旧新宿\r\n1003,渋谷\r\n9999,不明\r\n"

        val result = parseAccessLogCsv(csv, stations, NOW)

        assertEquals(CsvImportResult(listOf(log(1), log(2), log(3)), listOf("不明")), result)
    }

    @Test
    fun skipsCommentsAndLinesWithoutComma() {
        val csv = "#1001,東京\n\nheader\n1002,新宿,extra\n"

        val result = parseAccessLogCsv(csv, stations, NOW)

        assertEquals(CsvImportResult(listOf(log(2)), emptyList()), result)
    }

    @Test
    fun skipsExportHeader() {
        val csv = """
            ExportFile
            # ファイル名
            ファイル1
            # コード,駅名,色コード
            1001,東京,3
            1003,渋谷,3
        """.trimIndent() + "\n"

        assertEquals(CsvImportResult(listOf(log(1), log(3)), emptyList()), parseAccessLogCsv(csv, stations, NOW))
    }

    @Test
    fun resolvesSameNameStationsByCode() {
        val stations = listOf(
            station(id = 1, code = 2001, originalName = "大宮"),
            station(id = 2, code = 2002, originalName = "大宮"),
        )
        val csv = "2002,大宮\n2001,大宮\n9999,大宮\n"

        assertEquals(listOf(log(2), log(1)), parseAccessLogCsv(csv, stations, NOW).logs)
    }

    @Test
    fun removesDuplicateStations() {
        val csv = "1001,東京\n1001,東京\n"

        assertEquals(listOf(log(1)), parseAccessLogCsv(csv, stations, NOW).logs)
    }

    @Test
    fun treatsNonNumericCodeAsFailure() {
        val csv = "code,name\n"

        assertEquals(CsvImportResult(emptyList(), listOf("name")), parseAccessLogCsv(csv, stations, NOW))
    }

    private companion object {
        const val NOW = 1_700_000_000_000
    }
}
