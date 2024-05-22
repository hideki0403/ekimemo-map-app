import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'config.dart';

enum NotificationSound {
  se1('notification_1', '通知音1'),
  se2('notification_2', '通知音2'),;

  const NotificationSound(this.id, this.displayName);
  final String id;
  final String displayName;

  @override
  String toString() => id;
}

enum VibrationPattern {
  pattern1('パターン1', [0, 100, 200, 100, 100, 300, 150, 100, 200, 100, 100, 300]);

  const VibrationPattern(this.displayName, this.pattern);
  final List<int> pattern;
  final String displayName;
}

class NotificationManager {
  static final _notification = FlutterLocalNotificationsPlugin();
  static final _audioPlayer = AudioPlayer();
  static final _tts = FlutterTts();

  static Future<void> initialize() async {
    await _notification.initialize(const InitializationSettings(android: AndroidInitializationSettings('@mipmap/ic_launcher')));
    await _tts.setLanguage('ja-JP');
  }

  static Future<void> showNotification(String title, String body, { bool silent = false, String? icon, String? ttsText }) async {
    if (!Config.enableNotification) return;

    final platform = NotificationDetails(android: AndroidNotificationDetails('nearest_station', '最寄り駅通知',
      importance: Importance.high,
      priority: Priority.high,
      playSound: false,
      enableVibration: false,
      silent: silent,
      icon: icon,
    ));

    await _notification.show(0, title, body, platform);

    if (!silent) {
      if (Config.enableVibration) playVibration(Config.vibrationPattern);
      if (Config.enableNotificationSound) await playSound(Config.notificationSound);
      if (Config.enableTts && ttsText != null) await playTTS(ttsText);
    }
  }

  static Future<void> playSound(NotificationSound sound) async {
    await _audioPlayer.setVolume(Config.notificationSoundVolume / 100);
    await _audioPlayer.setSource(AssetSource('sound/${sound.toString()}.mp3'));
    final waitTime = await _audioPlayer.getDuration() ?? Duration.zero;
    await _audioPlayer.resume();
    await Future.delayed(Duration(milliseconds: waitTime.inMilliseconds + 500));
    await _audioPlayer.stop();
  }

  static Future<void> playVibration(VibrationPattern pattern) async {
    await Vibration.vibrate(pattern: pattern.pattern);
  }

  static Future<void> playTTS(String text) async {
    await _tts.setVolume(Config.notificationSoundVolume / 100);
    await _tts.speak(text);
  }
}