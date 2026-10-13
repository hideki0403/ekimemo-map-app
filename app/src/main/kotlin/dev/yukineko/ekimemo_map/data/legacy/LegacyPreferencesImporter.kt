package dev.yukineko.ekimemo_map.data.legacy

import android.content.Context
import androidx.datastore.core.DataStore
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.ui.theme.AppFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LegacyPreferencesImporter(
    private val context: Context,
    private val settings: DataStore<Settings>,
) {
    suspend fun import(): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).all
        if (prefs.isEmpty()) return@withContext false

        settings.updateData { legacySettings(prefs) }
        context.deleteSharedPreferences(NAME)
        true
    }

    companion object {
        private const val NAME = "FlutterSharedPreferences"
    }
}

private const val KEY_PREFIX = "flutter."
private const val DOUBLE_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu"

internal fun legacySettings(prefs: Map<String, *>): Settings {
    fun value(key: String) = prefs[KEY_PREFIX + key]
    fun bool(key: String) = value(key) as? Boolean
    fun int(key: String) = (value(key) as? Long)?.toInt()
    fun string(key: String) = (value(key) as? String)?.takeIf { it.isNotEmpty() }
    fun double(key: String) = string(key)?.takeIf { it.startsWith(DOUBLE_PREFIX) }?.removePrefix(DOUBLE_PREFIX)?.toDoubleOrNull()

    val d = Settings()
    return Settings(
        cooldownTime = int("cooldown_time") ?: d.cooldownTime,
        enableReminder = bool("enable_reminder") ?: d.enableReminder,
        enableNotification = bool("enable_notification") ?: d.enableNotification,
        enableNotificationDuringCooldown = bool("enable_notification_during_cooldown") ?: d.enableNotificationDuringCooldown,
        maxResults = int("max_results") ?: d.maxResults,
        updateFrequency = double("update_frequency") ?: d.updateFrequency,
        maxAcceptableAccuracy = int("max_acceptable_accuracy") ?: d.maxAcceptableAccuracy,
        enableNotificationSound = bool("enable_notification_sound") ?: d.enableNotificationSound,
        notificationSound = legacyEnum(string("notification_sound")) ?: d.notificationSound,
        notificationSoundVolume = int("notification_sound_volume") ?: d.notificationSoundVolume,
        enableVibration = bool("enable_vibration") ?: d.enableVibration,
        vibrationPattern = legacyEnum(string("vibration_pattern")) ?: d.vibrationPattern,
        enableTts = bool("enable_tts") ?: d.enableTts,
        mapRenderingLimit = int("map_rendering_limit") ?: d.mapRenderingLimit,
        themeMode = legacyEnum(string("theme_mode")) ?: d.themeMode,
        font = AppFont.entries.firstOrNull { it.googleFontName == string("font_family") } ?: d.font,
        useDynamicColor = bool("use_material_you") ?: d.useDynamicColor,
        themeSeedColor = string("theme_seed_color")?.let(::parseLegacyColor) ?: d.themeSeedColor,
        enableMovementLog = bool("enable_movement_log") ?: d.enableMovementLog,
        minimumMovementDistance = int("minimum_movement_distance") ?: d.minimumMovementDistance,
        enableDebugMode = bool("enable_debug_mode") ?: d.enableDebugMode,
    )
}

private fun parseLegacyColor(hex: String): Int? {
    val digits = hex.removePrefix("#")
    return (if (digits.length == 6) "FF$digits" else digits).toLongOrNull(16)?.toInt()
}
