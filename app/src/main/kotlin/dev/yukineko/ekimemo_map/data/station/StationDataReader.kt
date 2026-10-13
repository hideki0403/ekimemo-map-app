package dev.yukineko.ekimemo_map.data.station

import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker
import org.msgpack.value.ValueType
import java.io.InputStream

fun readStationData(input: InputStream): StationData =
    MessagePack.newDefaultUnpacker(input).use { it.readData() }

private fun MessageUnpacker.readData(): StationData {
    var stations: List<Station>? = null
    var lines: List<Line>? = null
    var treeRoot: Int? = null
    var treeNodes: List<TreeNode>? = null
    var version: String? = null
    forEachKey { key ->
        when (key) {
            "version" -> version = unpackString()
            "station" -> stations = readList { readStation() }
            "line" -> lines = readList { readLine() }
            "tree" -> forEachKey {
                when (it) {
                    "root" -> treeRoot = unpackInt()
                    "node_list" -> treeNodes = readList { readTreeNode() }
                    else -> skipValue()
                }
            }
            else -> skipValue()
        }
    }
    return StationData(
        stations = required("station", stations).associateBy { it.id },
        lines = required("line", lines).associateBy { it.code },
        treeNodes = required("tree.node_list", treeNodes).associateBy { it.id },
        treeRoot = required("tree.root", treeRoot),
        version = version,
    )
}

private fun MessageUnpacker.readStation(): Station {
    var code: Int? = null
    var id: Int? = null
    var name: String? = null
    var originalName: String? = null
    var nameKana: String? = null
    var closed: Boolean? = null
    var lat: Double? = null
    var lng: Double? = null
    var prefecture: Int? = null
    var lines: List<Int>? = null
    var attr: StationAttr? = null
    var postalCode: String? = null
    var address: String? = null
    var openDate: String? = null
    var voronoi: List<LatLng>? = null
    var delaunay: List<Int>? = null
    forEachKey { key ->
        when (key) {
            "code" -> code = unpackInt()
            "id" -> id = unpackInt()
            "name" -> name = unpackString()
            "original_name" -> originalName = unpackString()
            "name_kana" -> nameKana = unpackString()
            "closed" -> closed = unpackBoolean()
            "lat" -> lat = readDouble()
            "lng" -> lng = readDouble()
            "prefecture" -> prefecture = unpackInt()
            "lines" -> lines = readList { unpackInt() }
            "attr" -> attr = StationAttr.valueOf(unpackString().uppercase())
            "postal_code" -> postalCode = unpackString()
            "address" -> address = unpackString()
            "open_date" -> openDate = readNullable { unpackString() }
            "voronoi" -> voronoi = readVoronoi()
            "delaunay" -> delaunay = readList { unpackInt() }
            else -> skipValue()
        }
    }
    return Station(
        code = required("station.code", code),
        id = required("station.id", id),
        name = required("station.name", name),
        originalName = required("station.original_name", originalName),
        nameKana = required("station.name_kana", nameKana),
        closed = required("station.closed", closed),
        lat = required("station.lat", lat),
        lng = required("station.lng", lng),
        prefecture = required("station.prefecture", prefecture),
        lines = required("station.lines", lines),
        attr = required("station.attr", attr),
        postalCode = required("station.postal_code", postalCode),
        address = required("station.address", address),
        openDate = openDate,
        voronoi = required("station.voronoi", voronoi),
        delaunay = required("station.delaunay", delaunay),
    )
}

private fun MessageUnpacker.readLine(): Line {
    var code: Int? = null
    var id: Int? = null
    var name: String? = null
    var nameKana: String? = null
    var nameFormal: String? = null
    var stationSize: Int? = null
    var companyCode: Int? = null
    var closed: Boolean? = null
    var closedDate: String? = null
    var color: String? = null
    var symbol: String? = null
    var stationList: List<Int>? = null
    var polylines: List<List<LatLng>> = emptyList()
    var bounds: Bounds? = null
    forEachKey { key ->
        when (key) {
            "code" -> code = unpackInt()
            "id" -> id = unpackInt()
            "name" -> name = unpackString()
            "name_kana" -> nameKana = unpackString()
            "name_formal" -> nameFormal = readNullable { unpackString() }
            "station_size" -> stationSize = unpackInt()
            "company_code" -> companyCode = readNullable { unpackInt() }
            "closed" -> closed = unpackBoolean()
            "closed_date" -> closedDate = readNullable { unpackString() }
            "color" -> color = readNullable { unpackString() }
            "symbol" -> symbol = readNullable { unpackString() }
            "station_list" -> stationList = readList { unpackInt() }
            "polyline_list" -> readNullable {
                forEachKey {
                    when (it) {
                        "features" -> polylines = readList { readLineString() }
                        "properties" -> bounds = readBounds()
                        else -> skipValue()
                    }
                }
            }
            else -> skipValue()
        }
    }
    return Line(
        code = required("line.code", code),
        id = required("line.id", id),
        name = required("line.name", name),
        nameKana = required("line.name_kana", nameKana),
        nameFormal = nameFormal,
        stationSize = required("line.station_size", stationSize),
        companyCode = companyCode,
        closed = required("line.closed", closed),
        closedDate = closedDate,
        color = color,
        symbol = symbol,
        stationList = required("line.station_list", stationList),
        polylines = polylines,
        bounds = bounds,
    )
}

private fun MessageUnpacker.readTreeNode(): TreeNode {
    var id: Int? = null
    var left: Int? = null
    var right: Int? = null
    forEachKey { key ->
        when (key) {
            "id" -> id = unpackInt()
            "left" -> left = readNullable { unpackInt() }
            "right" -> right = readNullable { unpackInt() }
            else -> skipValue()
        }
    }
    return TreeNode(required("tree_node.id", id), left, right)
}

private fun MessageUnpacker.readVoronoi(): List<LatLng> = readGeometry { readList { readList { readLatLng() } }.first() }

private fun MessageUnpacker.readLineString(): List<LatLng> = readGeometry { readList { readLatLng() } }

private fun <T : Any> MessageUnpacker.readGeometry(readCoordinates: MessageUnpacker.() -> T): T {
    var coordinates: T? = null
    forEachKey { key ->
        if (key == "geometry") {
            forEachKey { if (it == "coordinates") coordinates = readCoordinates() else skipValue() }
        } else {
            skipValue()
        }
    }
    return required("geometry.coordinates", coordinates)
}

private fun MessageUnpacker.readLatLng(): LatLng {
    check(unpackArrayHeader() == 2) { "Invalid position" }
    val lng = readDouble()
    val lat = readDouble()
    return LatLng(lat, lng)
}

private fun MessageUnpacker.readBounds(): Bounds? {
    var north: Double? = null
    var east: Double? = null
    var south: Double? = null
    var west: Double? = null
    forEachKey { key ->
        when (key) {
            "north" -> north = readNullable { readDouble() }
            "east" -> east = readNullable { readDouble() }
            "south" -> south = readNullable { readDouble() }
            "west" -> west = readNullable { readDouble() }
            else -> skipValue()
        }
    }
    return Bounds(north ?: return null, east ?: return null, south ?: return null, west ?: return null)
}

private fun MessageUnpacker.readDouble(): Double =
    if (nextFormat.valueType == ValueType.INTEGER) unpackLong().toDouble() else unpackDouble()

private inline fun MessageUnpacker.forEachKey(block: (String) -> Unit) {
    repeat(unpackMapHeader()) { block(unpackString()) }
}

private inline fun <T> MessageUnpacker.readList(item: MessageUnpacker.() -> T): List<T> =
    List(unpackArrayHeader()) { item() }

private inline fun <T> MessageUnpacker.readNullable(value: MessageUnpacker.() -> T): T? =
    if (tryUnpackNil()) null else value()

private fun <T : Any> required(name: String, value: T?): T =
    checkNotNull(value) { "Missing field: $name" }
