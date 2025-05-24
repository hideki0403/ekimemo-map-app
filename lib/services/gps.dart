import 'dart:async';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import 'permission.dart';
import 'config.dart';
import 'utils.dart';
import 'log.dart';

final logger = Logger('GpsManager');

class GpsStateNotifier extends ChangeNotifier {
  static final GpsStateNotifier _instance = GpsStateNotifier._internal();

  factory GpsStateNotifier() => _instance;
  GpsStateNotifier._internal();

  void notify() {
    notifyListeners();
  }

  bool get isEnabled => GpsManager.isEnabled;
  Position? get lastLocation => GpsManager.lastLocation;
}

class GpsManager {
  static final GpsStateNotifier _stateNotifier = GpsStateNotifier();
  static final List<Function(double latitude, double longitude, double accuracy)> _listeners = [];
  static StreamSubscription? _locationListener;
  static Position? _lastLocation;

  static bool get isEnabled => _locationListener != null;
  static Position? get lastLocation => _lastLocation;

  static Future<void> setGpsEnabled(bool value) async {
    if (value) {
      if (!(await NotificationPermissionsHandler.checkAndRequest())) {
        showMessageDialog(
          title: '権限が必要です',
          message: '通知の許可がないため、最寄り駅の通知を行うことが出来ません。',
          icon: Icons.notifications_off_rounded,
        );
      }
      if (!(await _checkPermission())) return;

      final locationSettings = AndroidSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 10,
        intervalDuration: Duration(milliseconds: (Config.updateFrequency * 1000).toInt()),
        foregroundNotificationConfig: const ForegroundNotificationConfig(
          notificationTitle: '駅メモマップ',
          notificationText: '最寄り駅を探索中',
          notificationChannelName: '最寄り駅探索サービス',
          setOngoing: true,
          notificationIcon: AndroidResource(name: 'ic_notification_place', defType: 'drawable'),
        ),
      );

      _locationListener = Geolocator.getPositionStream(locationSettings: locationSettings).listen(_updateHandler);
    } else {
      _locationListener?.cancel();
      _locationListener = null;
    }

    logger.info('GPS ${value ? 'enabled' : 'disabled'}');

    _stateNotifier.notify();
  }

  static void addLocationListener(Function(double latitude, double longitude, double accuracy) listener) {
    _listeners.add(listener);
  }

  static void removeLocationListener(Function(double latitude, double longitude, double accuracy) listener) {
    _listeners.remove(listener);
  }

  static Future<bool> _checkPermission() async {
    LocationPermission permission;

    if (!await Geolocator.isLocationServiceEnabled()) {
      showMessageDialog(
        title: '位置情報が無効です',
        message: '位置情報が無効になっているため、駅の探索を行うことが出来ません。',
        icon: Icons.location_disabled_rounded,
      );

      return false;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) permission = await Geolocator.requestPermission();

    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      showMessageDialog(
        title: '権限が必要です',
        message: '位置情報を取得する権限がないため、駅の探索を行うことが出来ません。',
        icon: Icons.location_off_rounded,
      );

      return false;
    }

    return true;
  }

  static void _updateHandler(Position location) {
    _lastLocation = location;
    for (var listener in _listeners) {
      listener(location.latitude, location.longitude, location.accuracy);
    }
  }
}