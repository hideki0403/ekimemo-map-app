import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'station.dart';
import 'config.dart';
import 'utils.dart';

class NotificationManager {
  static final _instance = NotificationManager._internal();
  factory NotificationManager() => _instance;
  NotificationManager._internal();

  final _notification = FlutterLocalNotificationsPlugin();

  Future<void> initialize() async {
    await _notification.initialize(const InitializationSettings(android: AndroidInitializationSettings('ic_launcher')));
  }

  Future<void> showNotification(String title, String body) async {
    if (!Config.enableNotification) return;
    const android = AndroidNotificationDetails('nearest_station', '最寄り駅通知', importance: Importance.high, priority: Priority.high);
    const platform = NotificationDetails(android: android);
    await _notification.show(0, title, body, platform);
  }

  Future<void> showStationNotification(StationData data, {bool reNotify = false}) async {
    final body = !reNotify ? '${data.distance}で最寄り駅になりました' : '最後に通知してから${beautifySeconds(Config.cooldownTime)}が経過しました';
    showNotification('${data.station.name} [${data.station.nameKana}]', body);
  }
}