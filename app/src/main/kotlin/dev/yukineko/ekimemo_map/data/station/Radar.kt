package dev.yukineko.ekimemo_map.data.station

import dev.yukineko.ekimemo_map.diagram.Point
import dev.yukineko.ekimemo_map.diagram.containerOf
import dev.yukineko.ekimemo_map.diagram.highOrderVoronoi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val JP_RANGE = containerOf(left = 127.0, top = 46.0, right = 146.0, bottom = 26.0)

fun StationData.radar(center: Station, k: Int): Flow<List<LatLng>> =
    highOrderVoronoi(
        center = center.id,
        container = JP_RANGE,
        level = k,
        location = { id -> stations.getValue(id).let { Point(it.lng, it.lat) } },
        neighbors = { id -> stations.getValue(id).delaunay },
    )
        .asFlow()
        .map { polygon -> polygon.map { LatLng(it.y, it.x) } }
        .flowOn(Dispatchers.Default)
