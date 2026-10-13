package dev.yukineko.ekimemo_map.ui.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RectSelectionTest {
    private val rect = Rect(100f, 100f, 300f, 400f)

    @Test
    fun findsNearestCorner() {
        assertEquals(Offset(300f, 400f), rect.cornerAt(Offset(310f, 390f), 20f))
        assertNull(rect.cornerAt(Offset(200f, 250f), 20f))
    }

    @Test
    fun oppositeCornerStaysFixedWhileDragging() {
        val anchor = rect.oppositeCorner(rect.topLeft)
        assertEquals(Offset(300f, 400f), anchor)
        assertEquals(Rect(150f, 120f, 300f, 400f), rectOf(anchor, Offset(150f, 120f)))
        assertEquals(Rect(300f, 400f, 500f, 600f), rectOf(anchor, Offset(500f, 600f)))
    }

    @Test
    fun coercesIntoImageBounds() {
        assertEquals(Offset(0f, 2400f), Offset(-10f, 3000f).coerceIn(Size(1080f, 2400f)))
    }
}
