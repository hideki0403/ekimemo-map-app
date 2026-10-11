package dev.yukineko.ekimemo_map.data.station

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class RadarTest {
    private val fixture = Json.parseToJsonElement(
        javaClass.getResource("/voronoi_fixture.json")!!.readText(),
    ).jsonObject

    private val stations = fixture.getValue("stations").jsonArray.map {
        val (id, lat, lng, delaunay) = it.jsonArray
        Station(
            code = id.jsonPrimitive.int, id = id.jsonPrimitive.int, name = "", originalName = "", nameKana = "",
            closed = false, lat = lat.jsonPrimitive.double, lng = lng.jsonPrimitive.double, prefecture = 13,
            lines = emptyList(), attr = StationAttr.UNKNOWN, postalCode = "", address = "", openDate = null,
            voronoi = emptyList(), delaunay = delaunay.jsonArray.map { d -> d.jsonPrimitive.int },
        )
    }.associateBy { it.id }

    private val data = StationData(stations = stations, lines = emptyMap(), treeNodes = emptyMap(), treeRoot = 0)

    @Test
    fun radarMatchesFlutter() = runBlocking {
        for (case in fixture.getValue("cases").jsonArray.map { it.jsonObject }) {
            val center = stations.getValue(case.getValue("center").jsonPrimitive.int)
            val expected = case.getValue("polygons").jsonArray.map { polygon ->
                polygon.jsonArray.map {
                    val (x, y) = it.jsonArray.map { v -> v.jsonPrimitive.double }
                    LatLng(y, x)
                }
            }
            val actual = data.radar(center, case.getValue("k").jsonPrimitive.int).toList()
            assertEquals("center: ${center.id}", expected, actual)
        }
    }
}
