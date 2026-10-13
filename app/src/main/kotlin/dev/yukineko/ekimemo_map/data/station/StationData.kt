package dev.yukineko.ekimemo_map.data.station

data class LatLng(val lat: Double, val lng: Double)

data class Bounds(val north: Double, val east: Double, val south: Double, val west: Double)

enum class StationAttr { ECO, HEAT, COOL, UNKNOWN }

data class Station(
    val code: Int,
    val id: Int,
    val name: String,
    val originalName: String,
    val nameKana: String,
    val closed: Boolean,
    val lat: Double,
    val lng: Double,
    val prefecture: Int,
    val lines: List<Int>,
    val attr: StationAttr,
    val postalCode: String,
    val address: String,
    val openDate: String?,
    val voronoi: List<LatLng>,
    val delaunay: List<Int>,
)

data class Line(
    val code: Int,
    val id: Int,
    val name: String,
    val nameKana: String,
    val nameFormal: String?,
    val stationSize: Int,
    val companyCode: Int?,
    val closed: Boolean,
    val closedDate: String?,
    val color: String?,
    val symbol: String?,
    val stationList: List<Int>,
    val polylines: List<List<LatLng>>,
    val bounds: Bounds?,
)

data class TreeNode(val id: Int, val left: Int?, val right: Int?)

class StationData(
    val stations: Map<Int, Station>,
    val lines: Map<Int, Line>,
    val treeNodes: Map<Int, TreeNode>,
    val treeRoot: Int,
    val version: String?,
)
