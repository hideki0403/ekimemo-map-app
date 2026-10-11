package dev.yukineko.ekimemo_map.data.station

import kotlin.math.abs
import kotlin.math.sqrt

class StationTree(data: StationData) {
    private class Node(val station: Station, val left: Node?, val right: Node?)

    private class Candidate(val station: Station, val distance: Double)

    private val root = build(data, data.treeRoot)

    fun nearest(lat: Double, lng: Double, maxResults: Int): List<Station> {
        require(maxResults > 0) { "maxResults must be positive" }
        val result = mutableListOf<Candidate>()
        search(root, 0, lat, lng, maxResults, result)
        return result.map { it.station }
    }

    private fun search(node: Node, depth: Int, lat: Double, lng: Double, maxResults: Int, result: MutableList<Candidate>) {
        val station = node.station
        val dLat = station.lat - lat
        val dLng = station.lng - lng
        val distance = sqrt(dLat * dLat + dLng * dLng)

        val index = when {
            result.isNotEmpty() && distance < result.last().distance -> result.indexOfLast { distance >= it.distance } + 1
            result.size < maxResults -> result.size
            else -> null
        }
        if (index != null) {
            result.add(index, Candidate(station, distance))
            if (result.size > maxResults) result.removeAt(result.lastIndex)
        }

        val isEven = depth % 2 == 0
        val value = if (isEven) lng else lat
        val threshold = if (isEven) station.lng else station.lat
        val (next, opposite) = if (value < threshold) node.left to node.right else node.right to node.left

        next?.let { search(it, depth + 1, lat, lng, maxResults, result) }
        if (opposite != null && (result.size < maxResults || abs(value - threshold) < result.last().distance)) {
            search(opposite, depth + 1, lat, lng, maxResults, result)
        }
    }

    private fun build(data: StationData, id: Int): Node {
        val node = data.treeNodes.getValue(id)
        return Node(
            station = data.stations.getValue(id),
            left = node.left?.let { build(data, it) },
            right = node.right?.let { build(data, it) },
        )
    }
}
