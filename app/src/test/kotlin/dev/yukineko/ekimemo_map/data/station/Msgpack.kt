package dev.yukineko.ekimemo_map.data.station

import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import java.io.ByteArrayOutputStream

fun msgpack(value: Any?): ByteArray {
    val out = ByteArrayOutputStream()
    MessagePack.newDefaultPacker(out).use { it.pack(value) }
    return out.toByteArray()
}

private fun MessagePacker.pack(value: Any?) {
    when (value) {
        null -> packNil()
        is Boolean -> packBoolean(value)
        is Int -> packInt(value)
        is Double -> packDouble(value)
        is String -> packString(value)
        is List<*> -> {
            packArrayHeader(value.size)
            value.forEach { pack(it) }
        }
        is Map<*, *> -> {
            packMapHeader(value.size)
            value.forEach { (k, v) ->
                pack(k)
                pack(v)
            }
        }
        else -> error("Unsupported: $value")
    }
}
