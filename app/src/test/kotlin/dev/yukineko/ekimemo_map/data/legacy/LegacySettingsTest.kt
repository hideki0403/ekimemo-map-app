package dev.yukineko.ekimemo_map.data.legacy

import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.map.MapStyle
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.ui.theme.AppFont
import dev.yukineko.ekimemo_map.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacySettingsTest {
    @Test
    fun convertsFlutterPreferences() {
        val prefs = mapOf(
            "flutter.cooldown_time" to 100L,
            "flutter.enable_reminder" to true,
            "flutter.update_frequency" to "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu1.5",
            "flutter.notification_sound" to "sePs2",
            "flutter.vibration_pattern" to "pattern3",
            "flutter.map_style" to "fiord",
            "flutter.theme_mode" to "dark",
            "flutter.font_family" to "BIZ UDGothic",
            "flutter.use_material_you" to true,
            "flutter.theme_seed_color" to "FF5722",
            "flutter.disable_db_cache" to true,
        )

        val expected = Settings(
            cooldownTime = 100,
            enableReminder = true,
            updateFrequency = 1.5,
            notificationSound = NotificationSound.PS_2,
            vibrationPattern = VibrationPattern.PATTERN_3,
            mapStyle = MapStyle.FIORD,
            themeMode = ThemeMode.DARK,
            font = AppFont.BIZ_UD_GOTHIC,
            useDynamicColor = true,
            themeSeedColor = 0xFFFF5722.toInt(),
        )
        assertEquals(expected, legacySettings(prefs))
    }

    @Test
    fun fallsBackToDefaults() {
        val prefs = mapOf(
            "flutter.map_style" to "defaultStyle",
            "flutter.font_family" to "",
            "flutter.update_frequency" to "3",
            "flutter.cooldown_time" to "invalid",
        )
        assertEquals(Settings(), legacySettings(prefs))
    }
}
