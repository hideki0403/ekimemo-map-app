package dev.yukineko.ekimemo_map.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogBufferTest {
    private fun LogBuffer.add(message: String) = add(LogLevel.INFO, "Test", message, 0)

    @Test
    fun dropsOldestEntriesWhenCapacityIsExceeded() {
        val buffer = LogBuffer(capacity = 3)
        (1..5).forEach { buffer.add("$it") }

        assertEquals(listOf("3", "4", "5"), buffer.entries.value.map { it.message })
    }

    @Test
    fun clearRemovesAllEntries() {
        val buffer = LogBuffer(capacity = 3)
        buffer.add("1")
        buffer.clear()

        assertTrue(buffer.entries.value.isEmpty())
    }

    @Test
    fun assignsUniqueIdsAcrossClear() {
        val buffer = LogBuffer(capacity = 3)
        buffer.add("1")
        val first = buffer.entries.value.single().id
        buffer.clear()
        buffer.add("2")

        assertNotEquals(first, buffer.entries.value.single().id)
    }
}
