import 'dart:async';
import 'package:uuid/uuid.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/notification.dart';
import 'package:ekimemo_map/models/interval_timer.dart';
import 'package:ekimemo_map/repository/interval_timer.dart';

final _repository = IntervalTimerRepository();

class IntervalTimerHandler {
  final IntervalTimer _model;
  Timer? _timer;
  Function? _onRemainingUpdated;
  Function? _onTimerStateChanged;
  int _cooldown = 0;

  IntervalTimer get timer => _model;
  bool get paused => _timer == null || !_timer!.isActive;
  bool get stopped => paused && _cooldown == _model.duration.inSeconds;
  int get remaining => _cooldown;

  IntervalTimerHandler(this._model) {
    _cooldown = _model.duration.inSeconds;
  }

  void start() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_cooldown > 0) {
        _cooldown--;
      } else {
        _notify();

        if (_model.duration.inSeconds <= 0) {
          _timer?.cancel();
          return;
        }

        _cooldown = _model.duration.inSeconds;
      }
      _onRemainingUpdated?.call();
    });

    _onTimerStateChanged?.call();
  }

  void pause() {
    _timer?.cancel();
    _onTimerStateChanged?.call();
  }

  void stop() {
    _timer?.cancel();
    _timer = null;
    _onTimerStateChanged?.call();
    reset();
  }

  void reset() {
    _cooldown = _model.duration.inSeconds;
    _onRemainingUpdated?.call();
    _onTimerStateChanged?.call();
  }

  Future<void> _notify() async {
    if (_model.enableNotification) {
      NotificationManager.showIntervalNotification('インターバルタイマー', '${_model.name} (${beautifySeconds(_model.duration.inSeconds)}) が終了しました');
    }

    if (_model.vibrationPattern != null) {
      NotificationManager.playVibration(_model.vibrationPattern!);
    }

    if (_model.notificationSound != null) {
      await NotificationManager.playSound(_model.notificationSound!);
    }

    if (_model.enableTts) {
      await NotificationManager.playTTS('タイマー「${_model.name}」が、終了しました');
    }
  }

  void setName(String name) {
    _model.name = name;
    _repository.update(_model);
  }

  void setEnableNotification(bool value) {
    _model.enableNotification = value;
    _repository.update(_model);
  }

  void setEnableTts(bool value) {
    _model.enableTts = value;
    _repository.update(_model);
  }

  void setDuration(Duration duration) {
    _model.duration = duration;
    reset();
    _repository.update(_model);
  }

  void setNotificationSound(NotificationSound? sound) {
    _model.notificationSound = sound;
    _repository.update(_model);
  }

  void setVibrationPattern(VibrationPattern? pattern) {
    _model.vibrationPattern = pattern;
    _repository.update(_model);
  }

  void onRemainingUpdated(Function() callback) {
    _onRemainingUpdated = callback;
  }

  void onTimerStateChanged(Function() callback) {
    _onTimerStateChanged = callback;
  }

  void refresh() {
    _onRemainingUpdated?.call();
    _onTimerStateChanged?.call();
  }

  void disposeCallback() {
    _onRemainingUpdated = null;
    _onTimerStateChanged = null;
  }

  void dispose() {
    _timer?.cancel();
  }
}

class IntervalTimerManager {
  static List<IntervalTimerHandler> _cache = [];

  static Future<List<IntervalTimerHandler>> getTimers() async {
    if (_cache.isNotEmpty) {
      return _cache;
    }

    final models = await _repository.getAll();
    final timers = <IntervalTimerHandler>[];

    for (final model in models) {
      timers.add(IntervalTimerHandler(model));
    }

    _cache = timers;

    return timers;
  }

  static Future<void> addTimer() async {
    final model = IntervalTimer();
    model.id = const Uuid().v4();
    model.name = '新しいタイマー';
    model.duration = Duration(seconds: Config.cooldownTime);
    model.enableNotification = Config.enableNotification;
    model.enableTts = Config.enableTts;
    model.notificationSound = Config.enableNotificationSound ? Config.notificationSound : null;
    model.vibrationPattern = Config.enableVibration ? Config.vibrationPattern : null;
    await _repository.insertModel(model);

    _cache.add(IntervalTimerHandler(model));
  }

  static Future<void> removeTimer(IntervalTimerHandler timer) async {
    timer.dispose();
    await _repository.delete(timer._model.id);
    _cache.remove(timer);
  }
}