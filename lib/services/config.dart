import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:ekimemo_map/models/meta.dart';
import 'package:ekimemo_map/repository/meta.dart';

class ConfigProvider extends ChangeNotifier {
  static final _instance = ConfigProvider._internal();
  factory ConfigProvider() => _instance;
  ConfigProvider._internal();

  SharedPreferences? _config;

  Future<void> init() async {
    _config = await SharedPreferences.getInstance();
  }

  bool get serviceAvailable => _config != null;
  SharedPreferences? get config => _config;

  int get cooldownTime => _config?.getInt('cooldown_time') ?? 300;
  bool get enableReminder => _config?.getBool('enable_reminder') ?? false;
  bool get enableNotification => _config?.getBool('enable_notification') ?? true;
  int get maxResults => _config?.getInt('max_results') ?? 12;
  double get updateFrequency => _config?.getDouble('update_frequency') ?? 3;
  int get maxAcceptableAccuracy => _config?.getInt('max_acceptable_accuracy') ?? 0;

  void notify() {
    notifyListeners();
  }

  void setCooldownTime(int value) {
    _config?.setInt('cooldown_time', value);
    notifyListeners();
  }

  void setEnableReminder(bool value) {
    _config?.setBool('enable_reminder', value);
    notifyListeners();
  }

  void setEnableNotification(bool value) {
    _config?.setBool('enable_notification', value);
    notifyListeners();
  }

  void setMaxResults(int value) {
    _config?.setInt('max_results', value);
    notifyListeners();
  }

  void setUpdateFrequency(double value) {
    _config?.setDouble('update_frequency', value);
    notifyListeners();
  }

  void setMaxAcceptableAccuracy(int value) {
    _config?.setInt('max_acceptable_accuracy', value);
    notifyListeners();
  }
}

class Config {
  static ConfigProvider? _configProvider;

  static void init(ConfigProvider instance) {
    _configProvider = instance;
  }

  static int get cooldownTime => _configProvider?.cooldownTime ?? 300;
  static bool get enableReminder => _configProvider?.enableReminder ?? false;
  static bool get enableNotification => _configProvider?.enableNotification ?? true;
  static int get maxResults => _configProvider?.maxResults ?? 12;
  static int get maxAcceptableAccuracy => _configProvider?.maxAcceptableAccuracy ?? 0;
  static double get updateFrequency => _configProvider?.updateFrequency ?? 3;

  static String getString(String key, {String defaultValue = ''}) {
    return _configProvider?.config?.getString(key) ?? defaultValue;
  }

  static void setString(String key, String value) {
    _configProvider?.config?.setString(key, value);
    _configProvider?.notify();
  }
}

class SystemStateProvider extends ChangeNotifier {
  static final _instance = SystemStateProvider._internal();
  factory SystemStateProvider() => _instance;
  SystemStateProvider._internal();

  final state = <String, String>{};
  final MetaRepository _repo = MetaRepository();

  String get stationDataVersion => state['station_data_version'] ?? '';
  String get treeNodeRoot => state['tree_node_root'] ?? '';

  Future<void> init() async {
    final records = await _repo.getAll();
    for (final record in records) {
      state[record.key] = record.value;
    }
  }

  void set(String key, String value) {
    state[key] = value;
    _repo.setValue(key, value);
    notifyListeners();
  }

  void setStationDataVersion(String value) {
    set('station_data_version', value);
  }

  void setTreeNodeRoot(String value) {
    set('tree_node_root', value);
  }
}

class SystemState {
  static SystemStateProvider? _systemStateProvider;

  static void init(SystemStateProvider instance) {
    _systemStateProvider = instance;
  }

  static bool get serviceAvailable => _systemStateProvider != null;

  static String get stationDataVersion => _systemStateProvider?.stationDataVersion ?? '';
  static String get treeNodeRoot => _systemStateProvider?.treeNodeRoot ?? '0';

  static String getString(String key, {String defaultValue = ''}) {
    return _systemStateProvider?.state[key] ?? defaultValue;
  }

  static void setString(String key, String value) {
    _systemStateProvider?.set(key, value);
  }
}