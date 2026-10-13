package dev.yukineko.ekimemo_map.data.settings

import androidx.compose.ui.graphics.toArgb
import dev.yukineko.ekimemo_map.BuildConfig
import dev.yukineko.ekimemo_map.notification.NotificationSound
import dev.yukineko.ekimemo_map.notification.VibrationPattern
import dev.yukineko.ekimemo_map.ui.theme.AppFont
import dev.yukineko.ekimemo_map.ui.theme.DefaultSeedColor
import dev.yukineko.ekimemo_map.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val cooldownTime: Int = 320,
    val enableReminder: Boolean = false,
    val enableNotification: Boolean = true,
    val enableNotificationDuringCooldown: Boolean = false,
    val maxResults: Int = 12,
    val updateFrequency: Double = 3.0,
    val maxAcceptableAccuracy: Int = 0,
    val enableNotificationSound: Boolean = true,
    val notificationSound: NotificationSound = NotificationSound.PB_1,
    val notificationSoundVolume: Int = 100,
    val enableVibration: Boolean = true,
    val vibrationPattern: VibrationPattern = VibrationPattern.PATTERN_1,
    val enableTts: Boolean = false,
    val mapRenderingLimit: Int = 750,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val font: AppFont = AppFont.NOTO_SANS_JP,
    val useDynamicColor: Boolean = true,
    val themeSeedColor: Int = DefaultSeedColor.toArgb(),
    val enableMovementLog: Boolean = false,
    val minimumMovementDistance: Int = 100,
    val enableDebugMode: Boolean = false,
) {
    val isDebugMode: Boolean get() = BuildConfig.DEBUG || enableDebugMode
}

@Serializable
data class AssistantSettings(
    val enabled: Boolean = false,
    val flow: String = "[]",
    val debugPackageName: String = "dev.yukineko.ekimemo_map",
)
