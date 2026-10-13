package dev.yukineko.ekimemo_map.data.backup

import dev.yukineko.ekimemo_map.data.user.AccessLog
import org.msgpack.core.MessagePack
import java.io.OutputStream

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
