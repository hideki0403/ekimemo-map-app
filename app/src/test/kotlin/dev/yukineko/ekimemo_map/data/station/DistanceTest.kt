package dev.yukineko.ekimemo_map.data.station

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {
    @Test
    fun formatsDistance() {
        assertEquals("0m", formatDistance(0))
        assertEquals("999m", formatDistance(999))
        assertEquals("1.0km", formatDistance(1000))
        assertEquals("1.3km", formatDistance(1250))
        assertEquals("12.3km", formatDistance(12345))
    }
}
