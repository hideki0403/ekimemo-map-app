package dev.yukineko.ekimemo_map.diagram

import kotlin.math.sqrt

data class Point(val x: Double, val y: Double) : Comparable<Point> {
    override fun compareTo(other: Point) = compareValuesBy(this, other, Point::x, Point::y)
}

class Line(val a: Double, val b: Double, val c: Double)

class Triangle(val a: Point, val b: Point, val c: Point)

fun lineThrough(p1: Point, p2: Point): Line {
    require(p1 != p2) { "duplicated point: $p1" }
    if (p1.x == p2.x) return Line(1.0, 0.0, -(p1.x + p2.x) / 2)
    return Line((p2.y - p1.y) / (p1.x - p2.x), 1.0, (p2.x * p1.y - p1.x * p2.y) / (p1.x - p2.x))
}

fun perpendicularBisector(p1: Point, p2: Point): Line {
    val a = p1.x - p2.x
    val b = p1.y - p2.y
    val c = (-p1.x * p1.x - p1.y * p1.y + p2.x * p2.x + p2.y * p2.y) / 2
    if (b == 0.0) {
        require(a != 0.0) { "duplicated point: $p1" }
        return Line(1.0, 0.0, c / a)
    }
    return Line(a / b, 1.0, c / b)
}

fun intersection(l1: Line, l2: Line): Point? {
    val det = l1.a * l2.b - l2.a * l1.b
    if (det == 0.0) return null

    var x = (l1.b * l2.c - l2.b * l1.c) / det
    var y = (l2.a * l1.c - l1.a * l2.c) / det
    if (l1.b == 0.0) x = -l1.c
    if (l2.b == 0.0) x = -l2.c
    if (l1.a == 0.0) y = -l1.c
    if (l2.a == 0.0) y = -l2.c
    return Point(x, y)
}

fun onSameSide(line: Line, p1: Point, p2: Point): Boolean {
    val v1 = line.a * p1.x + line.b * p1.y + line.c
    val v2 = line.a * p2.x + line.b * p2.y + line.c
    return v1 * v2 >= 0
}

fun Triangle.contains(p: Point, error: Double): Boolean {
    val x1 = a.x - p.x
    val y1 = a.y - p.y
    val x2 = b.x - p.x
    val y2 = b.y - p.y
    val x3 = c.x - p.x
    val y3 = c.y - p.y
    val v1 = x1 * y2 - y1 * x2
    val v2 = x2 * y3 - y2 * x3
    val v3 = x3 * y1 - y3 * x1
    return (v1 > -error && v2 > -error && v3 > -error) || (v1 < error && v2 < error && v3 < error)
}

fun containerOf(left: Double, top: Double, right: Double, bottom: Double): Triangle {
    require(left < right && bottom < top) { "Invalid rect: $left, $top, $right, $bottom" }
    val x = (left + right) / 2
    val y = (top + bottom) / 2
    val r = sqrt((left - right) * (left - right) + (top - bottom) * (top - bottom))
    return Triangle(
        Point(x - sqrt(3.0) * r, y + r),
        Point(x + sqrt(3.0) * r, y + r),
        Point(x, y - 2 * r),
    )
}
