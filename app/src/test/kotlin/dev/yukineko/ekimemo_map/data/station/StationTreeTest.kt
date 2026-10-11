package dev.yukineko.ekimemo_map.data.station

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class StationTreeTest {
    private val fixture = Json.parseToJsonElement(
        javaClass.getResource("/station_search_fixture.json")!!.readText(),
    ).jsonObject

    private fun JsonArray.ints() = map { it.jsonPrimitive.int }

    private fun station(id: Int, lat: Double, lng: Double) = Station(
        code = id, id = id, name = "$id", originalName = "$id", nameKana = "$id", closed = false,
        lat = lat, lng = lng, prefecture = 13, lines = emptyList(), attr = StationAttr.UNKNOWN,
        postalCode = "", address = "", openDate = null, voronoi = emptyList(), delaunay = emptyList(),
    )

    private val stations = fixture.getValue("stations").jsonArray.map {
        val (id, lat, lng) = it.jsonArray.map { v -> v.jsonPrimitive }
        station(id.int, lat.double, lng.double)
    }

    private val tree = StationTree(
        StationData(
            stations = stations.associateBy { it.id },
            lines = emptyMap(),
            treeNodes = fixture.getValue("nodes").jsonArray.associate {
                val (id, left, right) = it.jsonArray.map { v -> v.jsonPrimitive }
                id.int to TreeNode(id.int, left.intOrNull, right.intOrNull)
            },
            treeRoot = fixture.getValue("root").jsonPrimitive.int,
        ),
    )

    private val queries = fixture.getValue("queries").jsonArray.map { it.jsonObject }

    private val JsonObject.lat get() = getValue("lat").jsonPrimitive.double
    private val JsonObject.lng get() = getValue("lng").jsonPrimitive.double

    @Test
    fun nearestMatchesBruteForce() {
        for (query in queries) {
            val result = tree.nearest(query.lat, query.lng, query.getValue("k").jsonPrimitive.int)
            assertEquals("$query", query.getValue("ids").jsonArray.ints(), result.map { it.id })
        }
    }

    @Test
    fun inRectMatchesBruteForce() {
        val random = Random(0)
        repeat(200) {
            val south = random.nextDouble(34.7, 36.1)
            val west = random.nextDouble(138.8, 140.1)
            val north = south + random.nextDouble(0.0, 0.6)
            val east = west + random.nextDouble(0.0, 0.6)
            val expected = stations.filter { it.lat in south..north && it.lng in west..east }.map { it.id }.sorted()
            val actual = tree.inRect(north, east, south, west, stations.size).map { it.id }.sorted()
            assertEquals("$north $east $south $west", expected, actual)
        }
    }

    @Test
    fun inRectStopsAtMaxResults() {
        val result = tree.inRect(90.0, 180.0, -90.0, -180.0, 10)
        assertEquals(10, result.size)
        assertEquals(10, result.distinctBy { it.id }.size)
    }

    @Test
    fun inRectIncludesStationOnBoundary() {
        for (station in stations) {
            val result = tree.inRect(station.lat, station.lng, station.lat, station.lng, stations.size)
            assertEquals(listOf(station.id), result.map { it.id })
        }
    }

    @Test
    fun measureDistanceMatchesFlutter() {
        val byId = stations.associateBy { it.id }
        for (query in queries) {
            val expected = query.getValue("distances").jsonArray.ints()
            val actual = query.getValue("ids").jsonArray.ints().map {
                val station = byId.getValue(it)
                measureDistance(query.lat, query.lng, station.lat, station.lng)
            }
            assertEquals("$query", expected, actual)
        }
    }
}
