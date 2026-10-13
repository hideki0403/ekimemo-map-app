package dev.yukineko.ekimemo_map.data.backup

import dev.yukineko.ekimemo_map.data.user.AccessLog
import org.junit.Assert.assertEquals
import org.junit.Test
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import org.msgpack.value.ValueFactory
import java.io.ByteArrayOutputStream

class BackupTest {
    private fun Value.field(key: String): Value = asMapValue().map().getValue(ValueFactory.newString(key))

    @Test
    fun writesV2Format() {
        val logs = listOf(
            AccessLog(stationId = 1, firstAccess = 1_700_000_000_000, lastAccess = 1_700_000_100_000, accessCount = 3, accessed = true),
            AccessLog(stationId = 2, firstAccess = 1_700_000_200_000, lastAccess = 1_700_000_200_000, accessCount = 0, accessed = false),
        )
        val out = ByteArrayOutputStream()

        writeBackup(out, logs, createdAt = 1_800_000_000_000)

        val root = MessagePack.newDefaultUnpacker(out.toByteArray()).use { it.unpackValue() }
        assertEquals("ekimemo_map_backup", root.field("format").asStringValue().asString())
        assertEquals(2, root.field("version").asIntegerValue().asInt())
        assertEquals(1_800_000_000_000, root.field("created_at").asIntegerValue().asLong())

        val entries = root.field("access_log").asArrayValue().list()
        assertEquals(2, entries.size)
        val first = entries[0]
        assertEquals(1, first.field("station_id").asIntegerValue().asInt())
        assertEquals(1_700_000_000_000, first.field("first_access").asIntegerValue().asLong())
        assertEquals(1_700_000_100_000, first.field("last_access").asIntegerValue().asLong())
        assertEquals(3, first.field("access_count").asIntegerValue().asInt())
        assertEquals(true, first.field("accessed").asBooleanValue().boolean)
        assertEquals(false, entries[1].field("accessed").asBooleanValue().boolean)
    }
}
