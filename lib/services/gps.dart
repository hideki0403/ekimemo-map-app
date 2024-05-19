import 'dart:async';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

import 'permission.dart';
import 'station.dart';
import 'config.dart';
import 'utils.dart';

class GpsManager extends ChangeNotifier {
  final _stationManager = StationManager();
  StreamSubscription? _locationListener;
  Position? _lastLocation;

  bool get isEnabled => _locationListener != null;
  Position? get lastLocation => _lastLocation;

  Future<void> setGpsEnabled(bool value) async {
    if (value) {
      if (!(await NotificationPermissionsHandler.checkAndRequest())) {
        showMessageDialog(
          title: '権限が必要です',
          message: '通知の許可がないため、最寄り駅の通知を行うことが出来ません。',
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
        ),
      );

      _locationListener = Geolocator.getPositionStream(locationSettings: locationSettings).listen(_updateHandler);
    } else {
      _locationListener?.cancel();
      _locationListener = null;
    }

    notifyListeners();
  }

  Future<bool> _checkPermission() async {
    LocationPermission permission;

    if (!await Geolocator.isLocationServiceEnabled()) {
      showMessageDialog(
        title: '位置情報が無効です',
        message: '位置情報が無効になっているため、駅の探索を行うことが出来ません。',
      );

      return false;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) permission = await Geolocator.requestPermission();

    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      showMessageDialog(
        title: '権限が必要です',
        message: '位置情報を取得する権限がないため、駅の探索を行うことが出来ません。',
      );

      return false;
    }

    return true;
  }

  void _updateHandler(Position location) {
    _lastLocation = location;
    if (!_stationManager.serviceAvailable) return;
    if (Config.maxAcceptableAccuracy != 0 && Config.maxAcceptableAccuracy < location.accuracy) return;
    _stationManager.updateLocation(location.latitude, location.longitude);
  }
}