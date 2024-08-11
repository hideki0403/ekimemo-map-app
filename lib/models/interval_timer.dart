import '_abstract.dart';
import 'package:ekimemo_map/services/notification.dart';

class IntervalTimer extends AbstractModel {
  late String id;
  late String name;
  late Duration duration;
  late bool enableNotification;
  late bool enableTts;
  late NotificationSound? notificationSound;
  late VibrationPattern? vibrationPattern;

  @override
  IntervalTimer fromMap(Map<String, dynamic> map) {
    final intervalTimer = IntervalTimer();
    intervalTimer.id = map['id'];
    intervalTimer.name = map['name'];
    intervalTimer.duration = Duration(seconds: map['duration']);
    intervalTimer.enableNotification = map['enable_notification'] == 1;
    intervalTimer.enableTts = map['enable_tts'] == 1;
    intervalTimer.notificationSound = NotificationSound.fromName(map['notification_sound']);
    intervalTimer.vibrationPattern = VibrationPattern.byName(map['vibration_pattern']);
    return intervalTimer;
  }

  @override
  IntervalTimer fromJson(Map<String, dynamic> json) {
    final intervalTimer = IntervalTimer();
    intervalTimer.id = json['id'];
    intervalTimer.name = json['name'];
    intervalTimer.duration = Duration(seconds: json['duration']);
    intervalTimer.enableNotification = json['enable_notification'];
    intervalTimer.enableTts = json['enable_tts'];
    intervalTimer.notificationSound = NotificationSound.fromName(json['notification_sound']);
    intervalTimer.vibrationPattern = VibrationPattern.byName(json['vibration_pattern']);
    return intervalTimer;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'duration': duration.inSeconds,
      'enable_notification': enableNotification ? 1 : 0,
      'enable_tts': enableTts ? 1 : 0,
      'notification_sound': notificationSound?.name,
      'vibration_pattern': vibrationPattern?.name,
    };
  }
}