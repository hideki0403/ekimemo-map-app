import 'dart:async';
import 'package:flutter/material.dart';
import 'package:location/location.dart';

import 'permission.dart';
import 'station.dart';
import 'config.dart';

class GpsManager extends ChangeNotifier {
  bool _isEnabled = false;
  bool _isScheduledUpdate = false;
  StreamSubscription? _locationListener;
  LocationData? _lastLocation;
  DateTime? _lastUpdate;
  final _stationManager = StationManager();
  final _location = Location();

  bool get isEnabled => _isEnabled;
  LocationData? get lastLocation => _lastLocation;

  Future<void> setGpsEnabled(bool value) async {
    if (value) {
      await NotificationPermissionsHandler().checkAndRequest();

      if (!await LocationPermissionsHandler().checkAndRequest()) {
        _locationListener?.cancel();
        return;
      }
      _location.enableBackgroundMode(enable: true);
      _locationListener = _location.onLocationChanged.listen((event) => _updateHandler(event));
    } else {
      _location.enableBackgroundMode(enable: false);
      _locationListener?.cancel();
    }

    _isEnabled = value;
    notifyListeners();
  }

  void _updateHandler(LocationData location) {
    _lastLocation = location;
    if (!_stationManager.serviceAvailable) return;
    if (Config.maxAcceptableAccuracy != 0 && Config.maxAcceptableAccuracy < location.accuracy!) return;

    final updateFrequency = (Config.updateFrequency * 1000).toInt();
    final lastUpdateElapsed = _lastUpdate == null ? updateFrequency : DateTime.now().difference(_lastUpdate!).inMilliseconds;
    if (updateFrequency <= lastUpdateElapsed) {
      _updateLocation();
    } else if (!_isScheduledUpdate) {
      _isScheduledUpdate = true;
      Future.delayed(Duration(milliseconds: updateFrequency - lastUpdateElapsed), () => _updateLocation());
    }
  }

  void _updateLocation() {
    _isScheduledUpdate = false;
    _lastUpdate = DateTime.now();
    _stationManager.updateLocation(_lastLocation!.latitude!, _lastLocation!.longitude!);
  }
}