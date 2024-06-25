import 'dart:async';
import 'package:flutter/material.dart';
import 'package:collection/collection.dart';
import 'package:uuid/uuid.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/models/passing_log.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/repository/passing_log.dart';
import 'package:intl/intl.dart';
import 'config.dart';
import 'notification.dart';
import 'utils.dart';
import 'assistant.dart';
import 'search.dart';
import 'gps.dart';
import 'log.dart';

final logger = Logger('StationManager');
final _lineRepository = LineRepository();

enum ResultType {
  success,
  error
}

class AsyncResult<T> {
  final ResultType type;
  final T? value;
  final dynamic err;

  AsyncResult.success(this.value)
      : type = ResultType.success,
        err = null;
  AsyncResult.error(this.err)
      : type = ResultType.error,
        value = null;
}

class AccessCacheItem {
  DateTime time;
  bool accessed;

  AccessCacheItem(this.time, this.accessed);
}

class AccessCacheManager {
  static final accessCache = <String, AccessCacheItem>{};
  static final _repository = AccessLogRepository();

  static Future<void> initialize({ bool force = false }) async {
    if (accessCache.isNotEmpty) {
      if (!force) return;
      accessCache.clear();
    }

    final accessLogs = await _repository.getAll();
    for (final accessLog in accessLogs) {
      accessCache[accessLog.id] = AccessCacheItem(accessLog.lastAccess, accessLog.accessed);
    }
  }

  static Future<void> update(String id, DateTime lastAccess, { bool updateOnly = false, bool? accessed }) async {
    final accessLog = await _repository.get(id);
    var isAccessed = false;

    if (accessLog == null) {
      final record = AccessLog();
      record.id = id;
      record.firstAccess = lastAccess;
      record.lastAccess = lastAccess;
      record.accessCount = updateOnly ? 0 : 1;
      record.accessed = accessed ?? (updateOnly ? false : true);
      await _repository.insertModel(record);

      isAccessed = record.accessed;
    } else {
      accessLog.lastAccess = lastAccess;
      if (!updateOnly) accessLog.accessCount++;
      if (accessed != null) accessLog.accessed = accessed;
      await _repository.update(accessLog);

      isAccessed = accessLog.accessed;
    }

    accessCache[id] = AccessCacheItem(lastAccess, isAccessed);
  }

  static bool has(String id) => accessCache.containsKey(id);

  static DateTime? getTime(String id) {
    final item = accessCache[id];
    return item?.time;
  }

  static AccessCacheItem? get(String id) {
    return accessCache[id];
  }
}

class PassingLogManager {
  static final _repository = PassingLogRepository();
  static Future<void> insert(StationData data, bool reNotify) async {
    if (GpsManager.lastLocation == null) {
      logger.warning('PassingLogManager: Location is not available');
      return;
    }

    final latitude = GpsManager.lastLocation!.latitude;
    final longitude = GpsManager.lastLocation!.longitude;

    final passingLog = PassingLog();
    passingLog.uuid = const Uuid().v4();
    passingLog.id = data.station.id;
    passingLog.timestamp = DateTime.now();
    passingLog.latitude = latitude;
    passingLog.longitude = longitude;
    passingLog.speed = GpsManager.lastLocation!.speed;
    passingLog.accuracy = GpsManager.lastLocation!.accuracy;
    passingLog.distance = measure(latitude, longitude, data.station.lat, data.station.lng);
    passingLog.isReNotify = reNotify;
    await _repository.insertModel(passingLog);
  }
}

class StationStateNotifier extends ChangeNotifier {
  static final _instance = StationStateNotifier._internal();

  factory StationStateNotifier() {
    return _instance;
  }

  StationStateNotifier._internal();

  void notify() {
    notifyListeners();
  }

  void cleanup() {
    StationManager.cleanup();
  }

  List<StationData> get list => StationSearchService.list;
  String get lastUpdate => StationSearchService.lastUpdatedTime != null ? DateFormat('HH:mm:ss').format(StationSearchService.lastUpdatedTime!) : '--:--:--';
  String get latestProcessingTime => StationSearchService.latestProcessingTime;
}

class StationManager {
  static final StationStateNotifier _stateNotifier = StationStateNotifier();
  static final Map<String, Future<AsyncResult>> _tasks = {};
  static Timer? _notificationTimer;

  static bool get serviceAvailable => StationSearchService.serviceAvailable;

  static Future<void> initialize() async {
    await AccessCacheManager.initialize();
    await StationSearchService.initialize();

    GpsManager.addLocationListener((latitude, longitude, accuracy) {
      if (!serviceAvailable) return;
      if (Config.maxAcceptableAccuracy != 0 && Config.maxAcceptableAccuracy < accuracy) return;
      updateLocation(latitude, longitude);
    });
  }

  static void cleanup() {
    _notificationTimer?.cancel();
  }

  static Future<T> runSync<T>(String tag, Future<T> Function() task) async {
    final running = _tasks[tag] ?? Future.value(AsyncResult.success(null));
    final next = running.then((_) async {
      try {
        final result = await task();
        return AsyncResult.success(result);
      } catch (err) {
        return AsyncResult.error(err);
      }
    });

    _tasks[tag] = next;
    final result = await next;
    if (_tasks[tag] == next) {
      _tasks.remove(tag);
    }

    if (result.type == ResultType.success) {
      return result.value as T;
    } else {
      return Future.error(result.err);
    }
  }

  static Future<void> updateLocation(double latitude, double longitude, {int maxDistance = 0}) async {
    return runSync<void>('updateLocation', () async {
      final (updated, station) = await StationSearchService.updateLocation(latitude, longitude, maxDistance: maxDistance);
      if (station == null) return;

      final stationList = StationSearchService.list;

      int? currentMasterLineId;
      List<int>? currentLineIdRanking;

      // 最寄り駅が変更されたらアクセスログ等を更新
      final isCoolDown = getCoolDownTime(station.station.id) > 0;

      if (updated) {
        if (!isCoolDown) await AccessCacheManager.update(station.station.id, DateTime.now());

        // 優先表示される路線名を計算
        final lineIdCount = <int, int>{};
        for (final stationData in stationList) {
          final lines = stationData.station.lines;
          for (final line in lines) {
            lineIdCount[line] = (lineIdCount[line] ?? 0) + 1;
          }
        }

        final lineIdCountList = lineIdCount.entries.toList();
        lineIdCountList.sort((a, b) => b.value.compareTo(a.value));
        final lineIdRanking = lineIdCountList.map((x) => x.key).toList();

        // 付近駅の最頻出路線を元に、最寄り駅が属する路線を決定
        currentMasterLineId = lineIdRanking.firstWhereOrNull((x) => stationList.first.station.lines.contains(x));
        currentLineIdRanking = lineIdRanking;
      }

      // _searchListの中身を更新
      await Future.wait(stationList.map((x) async {
        final hasMasterLine = x.station.lines.contains(currentMasterLineId);
        final lineId = (hasMasterLine ? currentMasterLineId : currentLineIdRanking?.firstWhereOrNull((line) => x.station.lines.contains(line))) ?? x.station.lines.first;

        x.lineName = (await _lineRepository.get(lineId))?.name ?? '不明';
        x.distance = beautifyDistance(measure(latitude, longitude, x.station.lat, x.station.lng));
      }));

      _stateNotifier.notify();

      if (updated) {
        _handleStationUpdate(station, silent: isCoolDown && !Config.enableNotificationDuringCooldown);
        _scheduleNotification();
      }
    });
  }

  static Future<List<Station>> updateRect(double north, double east, double south, double west, {int maxResults = 0}) async {
    return runSync<List<Station>>('updateRect', () async {
      return await StationSearchService.updateRectRegion(north, east, south, west, maxResults: maxResults);
    });
  }

  static void _scheduleNotification() {
    final currentStation = StationSearchService.list.firstOrNull;
    if (currentStation == null) return;
    _notificationTimer?.cancel();

    final coolDownTime = getCoolDownTime(currentStation.station.id);
    if (coolDownTime <= 0) return;

    _notificationTimer = Timer(Duration(seconds: coolDownTime), () async {
      _handleStationUpdate(currentStation, reNotify: true);

      final stationId = currentStation.station.id;
      final accessLog = AccessCacheManager.has(stationId);
      if (accessLog) {
        AccessCacheManager.update(stationId, DateTime.now(), updateOnly: true);
      }

      _scheduleNotification();
    });
  }

  static void _handleStationUpdate(StationData data, { bool reNotify = false, bool silent = false }) {
    PassingLogManager.insert(data, reNotify);

    final body = !reNotify ? '${data.distance}で最寄り駅になりました' : '最後に通知してから${beautifySeconds(Config.cooldownTime)}が経過しました';
    final ttsText = !reNotify ? '「${data.station.nameKana}」が、最寄り駅になりました' : '「${data.station.nameKana}」に、アクセスしてから、${beautifySeconds(Config.cooldownTime, jp: true)}が経過しました';
    NotificationManager.showNotification(data.station.name, body, silent: silent, icon: reNotify ? 'ic_notification_remind' : 'ic_notification_new', ttsText: ttsText);
    AssistantFlow.run();
  }
}