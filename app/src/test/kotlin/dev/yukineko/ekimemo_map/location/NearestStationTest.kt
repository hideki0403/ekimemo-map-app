package dev.yukineko.ekimemo_map.location

import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.data.station.Line
import dev.yukineko.ekimemo_map.data.station.Station
import dev.yukineko.ekimemo_map.data.station.StationAttr
import org.junit.Assert.assertEquals
import org.junit.Test

class NearestStationTest {
    private fun station(id: Int, vararg lines: Int) = Station(
        code = id, id = id, name = "$id", originalName = "$id", nameKana = "$id", closed = false,
        lat = 0.0, lng = 0.0, prefecture = 13, lines = lines.toList(), attr = StationAttr.UNKNOWN,
        postalCode = "", address = "", openDate = null, voronoi = emptyList(), delaunay = emptyList(),
    )

    private fun line(id: Int) = Line(
        code = id, id = id, name = "$id", nameKana = "$id", nameFormal = null, stationSize = 0,
        companyCode = null, closed = false, closedDate = null, color = null, symbol = null,
        stationList = emptyList(), polylines = emptyList(), bounds = null,
    )

    private val lines = (1..4).associateWith(::line)

    @Test
    fun selectsMostFrequentLineOfNearestStation() {
        val stations = listOf(station(1, 1, 2), station(2, 2, 3), station(3, 3), station(4, 3, 4))
        assertEquals(listOf(2, 2, 3, 3), selectLines(stations, lines).map { it?.id })
    }

    @Test
    fun prefersMasterLineOverHigherRankedLine() {
        val stations = listOf(station(1, 1), station(2, 1, 2), station(3, 2), station(4, 2))
        assertEquals(listOf(1, 1, 2, 2), selectLines(stations, lines).map { it?.id })
    }

    @Test
    fun returnsNullForUnknownLine() {
        assertEquals(listOf(null), selectLines(listOf(station(1, 99)), lines))
    }

    @Test
    fun cooldownIsZeroWhenReminderDisabled() {
        assertEquals(0, cooldownSeconds(0, Settings(enableReminder = false, cooldownTime = 300), 1000))
    }

    @Test
    fun cooldownCountsDownFromLastAccess() {
        val settings = Settings(enableReminder = true, cooldownTime = 300)
        assertEquals(0, cooldownSeconds(null, settings, 0))
        assertEquals(300, cooldownSeconds(0, settings, 999))
        assertEquals(200, cooldownSeconds(0, settings, 100_000))
        assertEquals(0, cooldownSeconds(0, settings, 300_000))
        assertEquals(0, cooldownSeconds(0, settings, 400_000))
    }
}
