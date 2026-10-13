package dev.yukineko.ekimemo_map.diagram

import kotlin.math.max
import kotlin.math.min

private const val ERROR = 1.0 / (1 shl 30)

fun <T> highOrderVoronoi(
    center: T,
    container: Triangle,
    level: Int,
    location: (T) -> Point,
    neighbors: (T) -> List<T>,
): Sequence<List<Point>> = sequence {
    val voronoi = Voronoi(center, container, location, neighbors)
    repeat(level) { yield(voronoi.nextPolygon()) }
}

private enum class StepDirection {
    UP, ZERO, DOWN;

    fun invert() = when (this) {
        UP -> DOWN
        DOWN -> UP
        ZERO -> ZERO
    }
}

private class Voronoi<T>(
    private val center: T,
    private val container: Triangle,
    private val location: (T) -> Point,
    private val neighbors: (T) -> List<T>,
) {
    private val centerPoint = location(center)
    private val bisectors = mutableListOf<Bisector>()
    private val delaunayPoints = hashSetOf(centerPoint)
    private var previousPolygon: List<Node>? = null
    private var level = 0

    init {
        addBoundary(lineThrough(container.a, container.b))
        addBoundary(lineThrough(container.b, container.c))
        addBoundary(lineThrough(container.c, container.a))
    }

    fun nextPolygon(): List<Point> {
        level++
        expandDelaunayPoints()
        val polygon = traverse()
        polygon.forEach { it.onSolved(level) }
        previousPolygon = polygon
        return polygon.map { it.point }
    }

    private fun traverse(): List<Node> {
        var next: Node?
        var previous: Node
        val previousPolygon = previousPolygon

        if (previousPolygon == null) {
            val sample = bisectors[0]
            next = sample.intersections[1].node
            previous = sample.intersections[0].node
            val history = hashSetOf<Point>()
            while (history.add(next!!.point)) {
                val current: Node = next
                next = current.nextDown(previous.point)
                previous = current
            }
        } else {
            next = null
            previous = previousPolygon.last()
            for (node in previousPolygon) {
                next = node.nextUp(previous.point)
                previous = node
                if (next != null && !next.hasSolved()) break
            }
        }

        check(next != null && !next.hasSolved()) { "fail to traverse polygon" }

        val start: Node = next
        val polygon = mutableListOf(start)
        var current: Node = start
        while (true) {
            val following = current.next(previous.point)
            previous = current
            current = following
            if (start.point == current.point) break
            check(polygon.none { it.point == current.point }) { "duplicated point in polygon" }
            polygon.add(current)
        }
        return polygon
    }

    private fun expandDelaunayPoints() {
        val queue = previousPolygon?.flatMap { listOfNotNull(it.p1.line.delaunayPoint, it.p2.line.delaunayPoint) } ?: listOf(center)
        for (point in queue) {
            neighbors(point).filter { delaunayPoints.add(location(it)) }.forEach(::addBisector)
        }
    }

    private fun addBoundary(line: Line) {
        val boundary = Bisector(line, null)
        for (preexist in bisectors) {
            val p = checkNotNull(intersection(boundary.line, preexist.line)) { "no intersection" }
            connect(p, Intersection(p, boundary), Intersection(p, preexist))
        }
        bisectors.add(boundary)
    }

    private fun addBisector(site: T) {
        val bisector = Bisector(perpendicularBisector(location(site), centerPoint), site)
        for (preexist in bisectors) {
            val p = intersection(bisector.line, preexist.line)
            if (p == null || !container.contains(p, ERROR)) continue
            connect(p, Intersection(p, bisector, preexist.line), Intersection(p, preexist, bisector.line))
        }
        bisectors.add(bisector)
    }

    private fun connect(p: Point, a: Intersection, b: Intersection) {
        val node = Node(p, a, b)
        a.node = node
        b.node = node
        a.line.addIntersection(a)
        b.line.addIntersection(b)
    }

    inner class Node(val point: Point, val p1: Intersection, val p2: Intersection) {
        private val onBoundary = p1.line.isBoundary != p2.line.isBoundary
        private var index = if (p1.line.isBoundary && p2.line.isBoundary) 0.0 else -1.0

        fun hasSolved() = index >= 0

        fun next(previous: Point): Node = when {
            p1.next?.point == previous -> calcNext(p1, p2, false, p1.step.invert())
            p1.previous?.point == previous -> calcNext(p1, p2, true, p1.step)
            p2.next?.point == previous -> calcNext(p2, p1, false, p2.step.invert())
            p2.previous?.point == previous -> calcNext(p2, p1, true, p2.step)
            else -> error("no next node")
        }

        private fun calcNext(current: Intersection, other: Intersection, forward: Boolean, step: StepDirection): Node {
            if (onBoundary && index > 0) return checkNotNull(if (forward) current.next else current.previous).node
            return other.neighbor(step.invert()).node
        }

        fun nextDown(previous: Point): Node {
            val target = when {
                p1.isNeighbor(previous) -> p2
                p2.isNeighbor(previous) -> p1
                else -> error("no neighbor")
            }
            val step = if (target.hasNeighbor(StepDirection.DOWN)) StepDirection.DOWN else StepDirection.ZERO
            return target.neighbor(step).node
        }

        fun nextUp(previous: Point): Node? {
            val (t1, t2) = when {
                p1.isNeighbor(previous) -> p2 to p1
                p2.isNeighbor(previous) -> p1 to p2
                else -> error("no neighbor")
            }
            return listOf(t1, t2).firstOrNull { it.hasNeighbor(StepDirection.UP) }?.neighbor(StepDirection.UP)?.node
        }

        fun onSolved(level: Int) {
            p1.line.onIntersectionSolved(p1)
            p2.line.onIntersectionSolved(p2)
            if (index < 0) {
                index = if (p1.line.isBoundary || p2.line.isBoundary) level.toDouble() else level + 0.5
            } else if (index % 1.0 != 0.0) {
                check(index + 0.5 == level.toDouble()) { "mismatched index, current: $level, node: $index" }
            }
        }
    }

    inner class Intersection(val point: Point, val line: Bisector, other: Line? = null) {
        val step = if (other == null) StepDirection.ZERO else {
            var dx = line.line.b
            var dy = -line.line.a
            if (dx < 0 || (dx == 0.0 && dy < 0)) {
                dx *= -1
                dy *= -1
            }
            if (onSameSide(other, Point(point.x + dx, point.y + dy), centerPoint)) StepDirection.DOWN else StepDirection.UP
        }

        var previous: Intersection? = null
        var next: Intersection? = null
        var index = 0
        lateinit var node: Node

        fun isNeighbor(p: Point) = next?.point == p || previous?.point == p

        fun hasNeighbor(direction: StepDirection) = when {
            direction == StepDirection.ZERO && step == StepDirection.ZERO -> true
            direction != StepDirection.ZERO && step != StepDirection.ZERO -> (if (direction == step) next else previous) != null
            else -> false
        }

        fun neighbor(direction: StepDirection): Intersection {
            val neighbor = when {
                direction == StepDirection.ZERO && step == StepDirection.ZERO -> previous ?: next
                direction != StepDirection.ZERO && step != StepDirection.ZERO -> if (direction == step) next else previous
                else -> null
            }
            return checkNotNull(neighbor) { "no neighbor" }
        }
    }

    inner class Bisector(val line: Line, val delaunayPoint: T?) {
        val isBoundary = delaunayPoint == null
        val intersections = mutableListOf<Intersection>()
        private var solvedFrom = Int.MAX_VALUE
        private var solvedTo = -1

        fun onIntersectionSolved(intersection: Intersection) {
            solvedFrom = min(solvedFrom, intersection.index)
            solvedTo = max(solvedTo, intersection.index)
        }

        fun addIntersection(intersection: Intersection) {
            val found = intersections.binarySearch { it.point.compareTo(intersection.point) }
            check(found < 0) { "same point already added in this bisector" }
            val index = -found - 1

            intersection.previous = intersections.getOrNull(index - 1)?.also { it.next = intersection }
            intersection.next = intersections.getOrNull(index)?.also { it.previous = intersection }
            intersection.index = index
            intersections.add(index, intersection)
            for (i in index + 1..intersections.lastIndex) intersections[i].index++

            if (solvedFrom < solvedTo) {
                if (index <= solvedFrom) {
                    solvedFrom++
                    solvedTo++
                } else {
                    check(index > solvedTo) { "new intersection added to solved range" }
                }
            }
        }
    }
}
