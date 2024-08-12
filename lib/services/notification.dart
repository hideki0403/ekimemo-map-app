import 'package:collection/collection.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:vibration/vibration.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'config.dart';
import 'utils.dart';

enum NotificationSound {
  sePb1('notification_pb_1', '通知音1'),
  sePb2('notification_pb_2', '通知音2'),
  sePb3('notification_pb_3', '通知音3'),
  sePs1('notification_ps_1', '通知音4'),
  sePs2('notification_ps_2', '通知音5'),
  sePs3('notification_ps_3', '通知音6'),
  sePs4('notification_ps_4', '通知音7'),
  seSl1('notification_sl_1', '通知音8'),
  seSl2('notification_sl_2', '通知音9'),
  seSl3('notification_sl_3', '通知音10'),
  seSl4('notification_sl_4', '通知音11');

  const NotificationSound(this.id, this.displayName);
  final String id;
  final String displayName;

  @override
  String toString() => id;

  static NotificationSound? fromName(String? value) {
    if (value == null) return null;
    return NotificationSound.values.firstWhereOrNull((e) => e.name == value);
  }
}

enum VibrationPattern {
  pattern1('パターン1', [0, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100]),
  pattern2('パターン2', [0, 300, 200, 300, 200]),
  pattern3('パターン3', [0, 500, 500, 500, 500]),
  pattern4('パターン4', [0, 100, 100, 500, 300, 100, 100, 500]),
  pattern5('パターン5', [0, 100, 200, 100, 100, 300, 200, 100, 200, 100, 100, 300]),;

  const VibrationPattern(this.displayName, this.pattern);
  final List<int> pattern;
  final String displayName;

  static VibrationPattern? byName(String? value) {
    if (value == null) return null;
    return VibrationPattern.values.firstWhereOrNull((e) => e.name == value);
  }
}

/// returns (isCanceled, selectedValue)
Future<(bool, NotificationSound?)> notificationSoundSelector(String? defaultValue, { bool withNone = false }) async {
  final entries = Map.fromEntries(NotificationSound.values.map((e) => MapEntry(e.name, e.displayName)));
  if (withNone) entries[''] = 'なし';

  final result = await showSelectDialog(
    title: '通知音',
    data: entries,
    defaultValue: defaultValue,
    showOkButton: true,
    onChanged: (String? value) {
      if (value != null && value.isNotEmpty) {
        NotificationManager.playSound(NotificationSound.values.byName(value));
      }
    },
  );

  if (result == null) return (true, null);
  if (result.isEmpty) return (false, null);
  return (false, NotificationSound.values.byName(result));
}

/// returns (isCanceled, selectedValue)
Future<(bool, VibrationPattern?)> vibrationPatternSelector(String? defaultValue, { bool withNone = false }) async {
  final entries = Map.fromEntries(VibrationPattern.values.map((e) => MapEntry(e.name, e.displayName)));
  if (withNone) entries[''] = 'なし';

  final result = await showSelectDialog(
    title: 'バイブレーションパターン',
    data: entries,
    defaultValue: defaultValue,
    showOkButton: true,
    onChanged: (String? value) {
      if (value != null && value.isNotEmpty) {
        NotificationManager.playVibration(VibrationPattern.values.byName(value));
      }
    },
  );

  if (result == null) return (true, null);
  if (result.isEmpty) return (false, null);
  return (false, VibrationPattern.values.byName(result));
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

  static Future<void> showIntervalNotification(String title, String body) async {
    const platform = NotificationDetails(android: AndroidNotificationDetails('interval_timer', 'インターバルタイマー',
      importance: Importance.high,
      priority: Priority.high,
      playSound: false,
      enableVibration: false,
      icon: 'ic_timer',
    ));

    await _notification.show(1, title, body, platform);
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