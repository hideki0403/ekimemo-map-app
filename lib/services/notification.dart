import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';
import 'station.dart';
import 'config.dart';
import 'utils.dart';

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
  static final _instance = NotificationManager._internal();
  factory NotificationManager() => _instance;
  NotificationManager._internal();

  final _notification = FlutterLocalNotificationsPlugin();
  final _audioPlayer = AudioPlayer();

  Future<void> initialize() async {
    await _notification.initialize(const InitializationSettings(android: AndroidInitializationSettings('ic_launcher')));
  }

  Future<void> showNotification(String title, String body) async {
    if (!Config.enableNotification) return;
    const android = AndroidNotificationDetails('nearest_station', '最寄り駅通知',
      importance: Importance.high,
      priority: Priority.high,
      playSound: false,
      enableVibration: false,
    );
    const platform = NotificationDetails(android: android);
    await _notification.show(0, title, body, platform);

    _audioPlayer.setVolume(Config.notificationSoundVolume / 100);
    _audioPlayer.play(AssetSource('sound/${Config.notificationSound.toString()}.mp3'));
    if (Config.enableVibration) Vibration.vibrate(pattern: Config.vibrationPattern.pattern);
  }

  Future<void> showStationNotification(StationData data, {bool reNotify = false}) async {
    final body = !reNotify ? '${data.distance}で最寄り駅になりました' : '最後に通知してから${beautifySeconds(Config.cooldownTime)}が経過しました';
    showNotification('${data.station.name} [${data.station.nameKana}]', body);
  }
}