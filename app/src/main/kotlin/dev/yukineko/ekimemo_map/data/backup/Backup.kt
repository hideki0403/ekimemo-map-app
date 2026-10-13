package dev.yukineko.ekimemo_map.data.backup

import dev.yukineko.ekimemo_map.data.legacy.parseLegacyDateTime
import dev.yukineko.ekimemo_map.data.user.AccessLog
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import org.msgpack.value.ValueFactory
import java.io.InputStream
import java.io.OutputStream
import java.time.ZoneId

private const val FORMAT = "ekimemo_map_backup"
private const val VERSION = 2

fun writeBackup(output: OutputStream, logs: List<AccessLog>, createdAt: Long) {
    MessagePack.newDefaultPacker(output).use { packer ->
        packer.packMapHeader(4)
        packer.packString("format").packString(FORMAT)
        packer.packString("version").packInt(VERSION)
        packer.packString("created_at").packLong(createdAt)
        packer.packString("access_log").packArrayHeader(logs.size)
        logs.forEach {
            packer.packMapHeader(5)
            packer.packString("station_id").packInt(it.stationId)
            packer.packString("first_access").packLong(it.firstAccess)
            packer.packString("last_access").packLong(it.lastAccess)
            packer.packString("access_count").packInt(it.accessCount)
            packer.packString("accessed").packBoolean(it.accessed)
        }
    }
}

fun readBackup(input: InputStream, zone: ZoneId): List<AccessLog> {
    val root = MessagePack.newDefaultUnpacker(input).use { it.unpackValue() }
    if (root.isArrayValue) {
        return root.asArrayValue().map {
            val log = it.fields()
            AccessLog(
                stationId = log.field("id").asIntegerValue().asInt(),
                firstAccess = parseLegacyDateTime(log.field("first_access").asStringValue().asString(), zone),
                lastAccess = parseLegacyDateTime(log.field("last_access").asStringValue().asString(), zone),
                accessCount = log.field("access_count").asIntegerValue().asInt(),
                accessed = log.field("accessed").asIntegerValue().asInt() != 0,
            )
        }
    }

    val fields = root.fields()
    val format = fields.field("format").asStringValue().asString()
    val version = fields.field("version").asIntegerValue().asInt()
    require(format == FORMAT && version == VERSION) { "Unsupported backup: $format v$version" }
    return fields.field("access_log").asArrayValue().map {
        val log = it.fields()
        AccessLog(
            stationId = log.field("station_id").asIntegerValue().asInt(),
            firstAccess = log.field("first_access").asIntegerValue().asLong(),
            lastAccess = log.field("last_access").asIntegerValue().asLong(),
            accessCount = log.field("access_count").asIntegerValue().asInt(),
            accessed = log.field("accessed").asBooleanValue().boolean,
        )
    }
}

private fun Value.fields(): Map<Value, Value> = asMapValue().map()

private fun Map<Value, Value>.field(key: String): Value = getValue(ValueFactory.newString(key))
