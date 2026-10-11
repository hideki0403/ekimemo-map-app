package dev.yukineko.ekimemo_map.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogBufferTest {
    private fun entry(message: String) = LogEntry(LogLevel.INFO, "Test", message, 0)

    @Test
    fun dropsOldestEntriesWhenCapacityIsExceeded() {
        val buffer = LogBuffer(capacity = 3)
        (1..5).forEach { buffer.add(entry("$it")) }

        assertEquals(listOf("3", "4", "5"), buffer.entries.value.map { it.message })
    }

    @Test
    fun clearRemovesAllEntries() {
        val buffer = LogBuffer(capacity = 3)
        buffer.add(entry("1"))
        buffer.clear()

        assertTrue(buffer.entries.value.isEmpty())
    }
}
