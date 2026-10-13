package dev.yukineko.ekimemo_map.data.legacy

import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class LegacyConversionTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun parsesDateTimeAsLocalTime() {
        assertEquals(1_714_534_496_000, parseLegacyDateTime("2024-05-01T12:34:56", tokyo))
        assertEquals(1_714_534_496_789, parseLegacyDateTime("2024-05-01T12:34:56.789", tokyo))
        assertEquals(1_714_534_496_789, parseLegacyDateTime("2024-05-01T12:34:56.789012", tokyo))
    }

    @Test
    fun mapsDartEnumNames() {
        assertEquals(NotificationSound.PB_1, legacyEnum<NotificationSound>("sePb1"))
        assertEquals(NotificationSound.SL_4, legacyEnum<NotificationSound>("seSl4"))
        assertEquals(VibrationPattern.PATTERN_5, legacyEnum<VibrationPattern>("pattern5"))
        assertEquals(ThemeMode.DARK, legacyEnum<ThemeMode>("dark"))
        assertNull(legacyEnum<NotificationSound>("unknown"))
        assertNull(legacyEnum<NotificationSound>(null))
    }
}
