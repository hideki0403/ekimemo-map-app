import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';
import 'station.dart';
import 'config.dart';
import 'utils.dart';

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
    
    _audioPlayer.play(AssetSource('sound/notification_1.mp3')); // TODO: 通知音を変更できるように
    Vibration.vibrate(pattern: [0, 100, 200, 100, 100, 300, 150, 100, 200, 100, 100, 300]); // TODO: パターンを変更できるように
  }

  Future<void> showStationNotification(StationData data, {bool reNotify = false}) async {
    final body = !reNotify ? '${data.distance}で最寄り駅になりました' : '最後に通知してから${beautifySeconds(Config.cooldownTime)}が経過しました';
    showNotification('${data.station.name} [${data.station.nameKana}]', body);
  }
}