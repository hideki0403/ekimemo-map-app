package dev.yukineko.ekimemo_map.data.settings

import dev.yukineko.ekimemo_map.notification.NotificationSound
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class JsonSerializerTest {
    private val serializer = JsonSerializer(Settings.serializer(), Settings())

    private fun read(text: String) = runBlocking { serializer.readFrom(text.byteInputStream()) }

    @Test
    fun roundTrip() {
        val settings = Settings(cooldownTime = 100, notificationSound = NotificationSound.SL_4)
        val output = ByteArrayOutputStream()
        runBlocking { serializer.writeTo(settings, output) }
        assertEquals(settings, read(output.toString()))
    }

    @Test
    fun fallsBackToDefaultsForMissingKeysAndUnknownValues() {
        val settings = read("""{"cooldownTime":100,"notificationSound":"REMOVED","removedKey":true}""")
        assertEquals(Settings(cooldownTime = 100), settings)
    }
}
