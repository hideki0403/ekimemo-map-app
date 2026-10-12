package dev.yukineko.ekimemo_map.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import dev.yukineko.ekimemo_map.data.settings.Settings
import dev.yukineko.ekimemo_map.log.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import kotlin.coroutines.resume

class NotificationPlayer(private val context: Context) {
    private val logger = Logger("NotificationPlayer")
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .build()
    } else {
        null
    }
    private var player: MediaPlayer? = null
    private val ttsMutex = Mutex()
    private var tts: TextToSpeech? = null

    suspend fun playAll(settings: Settings, ttsText: String) {
        if (settings.enableVibration) vibrate(settings.vibrationPattern)
        if (settings.enableNotificationSound) {
            delay(playSound(settings.notificationSound, settings.notificationSoundVolume) + 500L)
        }
        if (settings.enableTts) speak(ttsText, settings.notificationSoundVolume)
    }

    fun playSound(sound: NotificationSound, volume: Int): Int {
        stopSound()
        val player = MediaPlayer.create(context, sound.resId, audioAttributes, audioManager.generateAudioSessionId())
            ?: return 0
        this.player = player
        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player.setVolume(volume / 100f, volume / 100f)
        player.setOnCompletionListener { stopSound() }
        focusRequest?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager.requestAudioFocus(it) }
        player.start()
        return player.duration
    }

    @Suppress("DEPRECATION")
    fun vibrate(pattern: VibrationPattern) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, -1))
        } else {
            vibrator.vibrate(pattern.timings, -1)
        }
    }

    suspend fun speak(text: String, volume: Int) {
        val tts = tts() ?: return
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume / 100f) }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, null)
    }

    suspend fun isTtsAvailable(): Boolean =
        (tts()?.isLanguageAvailable(Locale.JAPAN) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE

    private fun stopSound() {
        player?.release()
        player = null
        focusRequest?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager.abandonAudioFocusRequest(it) }
    }

    private suspend fun tts(): TextToSpeech? = ttsMutex.withLock {
        tts ?: suspendCancellableCoroutine { cont ->
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engine.language = Locale.JAPAN
                    cont.resume(engine)
                } else {
                    logger.error("Failed to initialize TTS: $status")
                    engine.shutdown()
                    cont.resume(null)
                }
            }
        }.also { tts = it }
    }
}
