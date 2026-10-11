package dev.yukineko.ekimemo_map.data.station

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StationDataReaderTest {
    private fun station(id: Int) = mutableMapOf<String, Any?>(
        "code" to 1110100 + id,
        "id" to id,
        "name" to "駅$id",
        "original_name" to "駅$id",
        "name_kana" to "えき$id",
        "closed" to false,
        "lat" to 41.5,
        "lng" to 140.5,
        "prefecture" to 1,
        "lines" to listOf(11101),
        "attr" to "eco",
        "postal_code" to "040-0063",
        "address" to "住所",
        "open_date" to null,
        "voronoi" to mapOf(
            "type" to "Feature",
            "geometry" to mapOf(
                "type" to "Polygon",
                "coordinates" to listOf(listOf(listOf(140.0, 41.0), listOf(141, 41.0), listOf(140.0, 42.0))),
            ),
            "properties" to emptyMap<String, Any>(),
        ),
        "delaunay" to listOf(2, 3),
    )

    private fun line(polyline: Map<String, Any?>?) = mapOf(
        "code" to 11101,
        "id" to 1,
        "name" to "路線",
        "name_kana" to "ろせん",
        "name_formal" to null,
        "station_size" to 2,
        "company_code" to null,
        "closed" to true,
        "closed_date" to "2020-01-01",
        "color" to "#0000FF",
        "symbol" to null,
        "station_list" to listOf(1, 2),
        "polyline_list" to polyline,
    )

    private val polyline = mapOf(
        "type" to "FeatureCollection",
        "features" to listOf(
            mapOf(
                "type" to "Feature",
                "geometry" to mapOf("type" to "LineString", "coordinates" to listOf(listOf(140.0, 41.0), listOf(140.1, 41.1))),
                "properties" to mapOf("start" to "駅1", "end" to "駅2"),
            ),
        ),
        "properties" to mapOf("name" to "路線", "north" to 41.1, "east" to 140.1, "south" to 41.0, "west" to 140.0),
    )

    private fun data(stations: List<Map<String, Any?>>, lines: List<Map<String, Any?>>) = mapOf(
        "station" to stations,
        "line" to lines,
        "tree" to mapOf(
            "root" to 1,
            "node_list" to listOf(mapOf("id" to 1, "left" to 2, "right" to null)),
        ),
    )

    private fun read(value: Any?) = readStationData(msgpack(value).inputStream())

    @Test
    fun readsStations() {
        val result = read(data(listOf(station(1), station(2)), emptyList()))
        val station = result.stations.getValue(1)

        assertEquals(setOf(1, 2), result.stations.keys)
        assertEquals(1110101, station.code)
        assertEquals("えき1", station.nameKana)
        assertEquals(StationAttr.ECO, station.attr)
        assertNull(station.openDate)
        assertEquals(listOf(LatLng(41.0, 140.0), LatLng(41.0, 141.0), LatLng(42.0, 140.0)), station.voronoi)
        assertEquals(listOf(2, 3), station.delaunay)
    }

    @Test
    fun readsLinesByCode() {
        val result = read(data(emptyList(), listOf(line(polyline))))
        val line = result.lines.getValue(11101)

        assertNull(line.nameFormal)
        assertNull(line.companyCode)
        assertTrue(line.closed)
        assertEquals(listOf(listOf(LatLng(41.0, 140.0), LatLng(41.1, 140.1))), line.polylines)
        assertEquals(Bounds(north = 41.1, east = 140.1, south = 41.0, west = 140.0), line.bounds)
    }

    @Test
    fun readsLineWithoutPolyline() {
        val line = read(data(emptyList(), listOf(line(null)))).lines.getValue(11101)

        assertTrue(line.polylines.isEmpty())
        assertNull(line.bounds)
    }

    @Test
    fun readsTree() {
        val result = read(data(emptyList(), emptyList()))

        assertEquals(1, result.treeRoot)
        assertEquals(TreeNode(1, 2, null), result.treeNodes.getValue(1))
    }

    @Test
    fun ignoresUnknownKeys() {
        val station = station(1).apply { put("extra", mapOf("nested" to listOf(1, 2))) }
        val result = read(data(listOf(station), emptyList()))

        assertEquals("駅1", result.stations.getValue(1).name)
    }

    @Test
    fun failsWhenRequiredFieldIsMissing() {
        val station = station(1).apply { remove("lat") }

        assertThrows(IllegalStateException::class.java) { read(data(listOf(station), emptyList())) }
    }
}
