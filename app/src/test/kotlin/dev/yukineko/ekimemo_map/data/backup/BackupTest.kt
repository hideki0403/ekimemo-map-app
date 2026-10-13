package dev.yukineko.ekimemo_map.data.backup

import dev.yukineko.ekimemo_map.data.user.AccessLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.value.Value
import org.msgpack.value.ValueFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneId

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

    @Test
    fun readsV2Format() {
        val logs = listOf(
            AccessLog(stationId = 1, firstAccess = 1_700_000_000_000, lastAccess = 1_700_000_100_000, accessCount = 3, accessed = true),
            AccessLog(stationId = 2, firstAccess = 1_700_000_200_000, lastAccess = 1_700_000_200_000, accessCount = 0, accessed = false),
        )
        val out = ByteArrayOutputStream()
        writeBackup(out, logs, createdAt = 1_800_000_000_000)

        assertEquals(logs, readBackup(ByteArrayInputStream(out.toByteArray()), zone))
    }

    @Test
    fun readsV1Format() {
        val out = ByteArrayOutputStream()
        MessagePack.newDefaultPacker(out).use { packer ->
            packer.packArrayHeader(2)
            packV1(packer, id = 1, firstAccess = "2024-05-01T12:34:56.789", lastAccess = "2024-05-02T08:00:00.123456", accessCount = 3, accessed = 1)
            packV1(packer, id = 2, firstAccess = "2024-05-03T00:00:00.000", lastAccess = "2024-05-03T00:00:00.000", accessCount = 0, accessed = 0)
        }

        val logs = readBackup(ByteArrayInputStream(out.toByteArray()), zone)

        assertEquals(
            listOf(
                AccessLog(1, epoch("2024-05-01T12:34:56.789"), epoch("2024-05-02T08:00:00.123"), accessCount = 3, accessed = true),
                AccessLog(2, epoch("2024-05-03T00:00:00"), epoch("2024-05-03T00:00:00"), accessCount = 0, accessed = false),
            ),
            logs,
        )
    }

    @Test
    fun rejectsUnknownVersion() {
        val out = ByteArrayOutputStream()
        MessagePack.newDefaultPacker(out).use { packer ->
            packer.packMapHeader(3)
            packer.packString("format").packString("ekimemo_map_backup")
            packer.packString("version").packInt(3)
            packer.packString("access_log").packArrayHeader(0)
        }

        assertThrows(IllegalArgumentException::class.java) { readBackup(ByteArrayInputStream(out.toByteArray()), zone) }
    }

    private val zone = ZoneId.of("Asia/Tokyo")

    private fun epoch(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun packV1(packer: MessagePacker, id: Int, firstAccess: String, lastAccess: String, accessCount: Int, accessed: Int) {
        packer.packMapHeader(5)
        packer.packString("id").packInt(id)
        packer.packString("first_access").packString(firstAccess)
        packer.packString("last_access").packString(lastAccess)
        packer.packString("access_count").packInt(accessCount)
        packer.packString("accessed").packInt(accessed)
    }
}
