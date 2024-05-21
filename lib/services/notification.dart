import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';
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

  static Future<void> initialize() async {
    await _notification.initialize(const InitializationSettings(android: AndroidInitializationSettings('ic_launcher')));
  }

  static Future<void> showNotification(String title, String body, { bool silent = false }) async {
    if (!Config.enableNotification) return;

    final platform = NotificationDetails(android: AndroidNotificationDetails('nearest_station', '最寄り駅通知',
      importance: Importance.high,
      priority: Priority.high,
      playSound: false,
      enableVibration: false,
      silent: silent,
    ));

    await _notification.show(0, title, body, platform);

    if (!silent) {
      playSound(Config.notificationSound);
      if (Config.enableVibration) playVibration(Config.vibrationPattern);
    }
  }

  static void playSound(NotificationSound sound) {
    _audioPlayer.setVolume(Config.notificationSoundVolume / 100);
    _audioPlayer.play(AssetSource('sound/${sound.toString()}.mp3'));
  }

  static void playVibration(VibrationPattern pattern) {
    Vibration.vibrate(pattern: pattern.pattern);
  }
}