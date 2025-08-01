import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/repository/meta.dart';
import 'package:ekimemo_map/ui/pages/map.dart';

import 'notification.dart';

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

  int get cooldownTime => _config?.getInt('cooldown_time') ?? 320;
  bool get enableReminder => _config?.getBool('enable_reminder') ?? false;
  bool get enableNotification => _config?.getBool('enable_notification') ?? true;
  bool get enableNotificationDuringCooldown => _config?.getBool('enable_notification_during_cooldown') ?? false;
  int get maxResults => _config?.getInt('max_results') ?? 12;
  double get updateFrequency => _config?.getDouble('update_frequency') ?? 3;
  int get maxAcceptableAccuracy => _config?.getInt('max_acceptable_accuracy') ?? 0;
  bool get enableNotificationSound => _config?.getBool('enable_notification_sound') ?? true;
  NotificationSound get notificationSound => NotificationSound.fromName(_config?.getString('notification_sound')) ?? NotificationSound.sePb1;
  VibrationPattern get vibrationPattern => VibrationPattern.byName(_config?.getString('vibration_pattern')) ?? VibrationPattern.pattern1;
  int get notificationSoundVolume => _config?.getInt('notification_sound_volume') ?? 100;
  bool get enableVibration => _config?.getBool('enable_vibration') ?? true;
  bool get enableTts => _config?.getBool('enable_tts') ?? false;
  MapStyle get mapStyle => MapStyle.byName(_config?.getString('map_style')) ?? MapStyle.defaultStyle;
  int get mapRenderingLimit => _config?.getInt('map_rendering_limit') ?? 750;
  ThemeMode get themeMode => ThemeMode.values.firstWhere((e) => e.name == _config?.getString('theme_mode'), orElse: () => ThemeMode.system);
  String? get fontFamily {
    final font = _config?.getString('font_family');
    if (font == null || font == '') return 'Noto Sans JP';
    return font;
  }
  bool get disableDbCache => _config?.getBool('disable_db_cache') ?? false;
  bool get useMaterialYou => _config?.getBool('use_material_you') ?? false;
  Color get themeSeedColor => hexToColor(_config?.getString('theme_seed_color') ?? Colors.blue.toHexStringRGB());
  bool get enableMovementLog => _config?.getBool('enable_movement_log') ?? false;
  int get minimumMovementDistance => _config?.getInt('minimum_movement_distance') ?? 100;
  bool get enableDebugMode => kDebugMode || (_config?.getBool('enable_debug_mode') ?? false);

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

  void setEnableNotificationDuringCooldown(bool value) {
    _config?.setBool('enable_notification_during_cooldown', value);
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

  void setEnableNotificationSound(bool value) {
    _config?.setBool('enable_notification_sound', value);
    notifyListeners();
  }

  void setNotificationSound(NotificationSound value) {
    _config?.setString('notification_sound', value.name);
    notifyListeners();
  }

  void setVibrationPattern(VibrationPattern value) {
    _config?.setString('vibration_pattern', value.name);
    notifyListeners();
  }

  void setNotificationSoundVolume(int value) {
    _config?.setInt('notification_sound_volume', value);
    notifyListeners();
  }

  void setEnableVibration(bool value) {
    _config?.setBool('enable_vibration', value);
    notifyListeners();
  }

  void setEnableTts(bool value) {
    _config?.setBool('enable_tts', value);
    notifyListeners();
  }

  void setMapStyle(MapStyle value) {
    _config?.setString('map_style', value.name);
    notifyListeners();
  }

  void setThemeMode(ThemeMode value) {
    _config?.setString('theme_mode', value.name);
    notifyListeners();
  }

  void setMapRenderingLimit(int value) {
    _config?.setInt('map_rendering_limit', value);
    notifyListeners();
  }

  void setFontFamily(String? value) {
    _config?.setString('font_family', value ?? '');
    notifyListeners();
  }

  void setDisableDbCache(bool value) {
    _config?.setBool('disable_db_cache', value);
    notifyListeners();
  }

  void setUseMaterialYou(bool value) {
    _config?.setBool('use_material_you', value);
    notifyListeners();
  }

  void setThemeSeedColor(Color value) {
    _config?.setString('theme_seed_color', value.toHexStringRGB());
    notifyListeners();
  }

  void setEnableMovementLog(bool value) {
    _config?.setBool('enable_movement_log', value);
    notifyListeners();
  }

  void setMinimumMovementDistance(int value) {
    _config?.setInt('minimum_movement_distance', value);
    notifyListeners();
  }

  void setEnableDebugMode(bool value) {
    _config?.setBool('enable_debug_mode', value);
    notifyListeners();
  }
}

class Config {
  static ConfigProvider? _configProvider;

  static void init(ConfigProvider instance) {
    _configProvider = instance;
  }

  static int get cooldownTime => _configProvider?.cooldownTime ?? 320;
  static bool get enableReminder => _configProvider?.enableReminder ?? false;
  static bool get enableNotification => _configProvider?.enableNotification ?? true;
  static bool get enableNotificationDuringCooldown => _configProvider?.enableNotificationDuringCooldown ?? false;
  static int get maxResults => _configProvider?.maxResults ?? 12;
  static int get maxAcceptableAccuracy => _configProvider?.maxAcceptableAccuracy ?? 0;
  static double get updateFrequency => _configProvider?.updateFrequency ?? 3;
  static bool get enableNotificationSound => _configProvider?.enableNotificationSound ?? true;
  static NotificationSound get notificationSound => _configProvider?.notificationSound ?? NotificationSound.sePb1;
  static VibrationPattern get vibrationPattern => _configProvider?.vibrationPattern ?? VibrationPattern.pattern1;
  static int get notificationSoundVolume => _configProvider?.notificationSoundVolume ?? 100;
  static bool get enableVibration => _configProvider?.enableVibration ?? true;
  static bool get enableTts => _configProvider?.enableTts ?? false;
  static MapStyle get mapStyle => _configProvider?.mapStyle ?? MapStyle.defaultStyle;
  static ThemeMode get themeMode => _configProvider?.themeMode ?? ThemeMode.system;
  static int get mapRenderingLimit => _configProvider?.mapRenderingLimit ?? 750;
  static String? get fontFamily => _configProvider?.fontFamily ?? 'Noto Sans JP';
  static bool get disableDbCache => _configProvider?.disableDbCache ?? false;
  static bool get useMaterialYou => _configProvider?.useMaterialYou ?? false;
  static Color get themeSeedColor => _configProvider?.themeSeedColor ?? Colors.blue;
  static bool get enableMovementLog => _configProvider?.enableMovementLog ?? false;
  static int get minimumMovementDistance => _configProvider?.minimumMovementDistance ?? 100;
  static bool get enableDebugMode => _configProvider?.enableDebugMode ?? false;

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
  String get debugPackageName => state['debug_package_name'] ?? 'dev.yukineko.ekimemo_map';
  String get assistantFlow => state['assistant_flow'] ?? '[]';
  bool get enabledAssistantFlow => bool.tryParse(state['enabled_assistant_flow'] ?? '') ?? false;

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

  void setDebugPackageName(String value) {
    set('debug_package_name', value);
  }

  void setEnabledAssistantFlow(bool value) {
    set('enabled_assistant_flow', value.toString());
  }
}

class SystemState {
  static SystemStateProvider? _systemStateProvider;

  static void init(SystemStateProvider instance) {
    _systemStateProvider = instance;
  }

  static bool get serviceAvailable => _systemStateProvider != null;

  static String get stationDataVersion => _systemStateProvider?.stationDataVersion ?? '';
  static int? get treeNodeRoot => int.tryParse(_systemStateProvider?.treeNodeRoot ?? '');
  static String get assistantFlow => _systemStateProvider?.assistantFlow ?? '[]';
  static String get debugPackageName => _systemStateProvider?.debugPackageName ?? 'dev.yukineko.ekimemo_map';
  static bool get enabledAssistantFlow => _systemStateProvider?.enabledAssistantFlow ?? false;

  static String getString(String key, {String defaultValue = ''}) {
    return _systemStateProvider?.state[key] ?? defaultValue;
  }

  static void setString(String key, String value) {
    _systemStateProvider?.set(key, value);
  }
}