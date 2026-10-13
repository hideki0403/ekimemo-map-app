package dev.yukineko.ekimemo_map.ui.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.min

internal val Rect.corners: List<Offset> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

internal fun Rect.cornerAt(position: Offset, tolerance: Float): Offset? =
    corners.filter { (it - position).getDistance() <= tolerance }.minByOrNull { (it - position).getDistance() }

internal fun Rect.oppositeCorner(corner: Offset) = Offset(left + right - corner.x, top + bottom - corner.y)

internal fun rectOf(a: Offset, b: Offset) = Rect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))

internal fun Offset.coerceIn(bounds: Size) = Offset(x.coerceIn(0f, bounds.width), y.coerceIn(0f, bounds.height))
