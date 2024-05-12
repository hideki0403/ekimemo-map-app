import 'dart:async';
import 'dart:math';
import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:collection/collection.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/tree_node.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'config.dart';
import 'notification.dart';
import 'utils.dart';

final _stationRepository = StationRepository();
final _treeNodeRepository = TreeNodeRepository();

class Bounds {
  final double north;
  final double east;
  final double south;
  final double west;

  Bounds({
    required this.north,
    required this.east,
    required this.south,
    required this.west,
  });

  bool isInsideRect(double lat, double lng) {
    return lat >= south && lat <= north && lng >= west && lng <= east;
  }
}

class StationData {
  Station station;
  double rawDistance;
  String? distance;
  String? lineName;
  int? index;

  StationData(this.station, this.rawDistance, {this.distance, this.index});

  factory StationData.create(Station station, double rawDistance) {
    return StationData(station, rawDistance);
  }
}

class AccessCacheManager {
  static final accessCache = <String, DateTime>{};
  static final _repository = AccessLogRepository();

  static Future<void> initialize() async {
    if (accessCache.isNotEmpty) return;

    final accessLogs = await _repository.getAll();
    for (final accessLog in accessLogs) {
      accessCache[accessLog.id] = accessLog.lastAccess;
    }
  }

  static Future<void> update(String id, DateTime lastAccess) async {
    accessCache[id] = lastAccess;
    final accessLog = await _repository.get(id);
    if (accessLog == null) {
      final record = AccessLog();
      record.id = id;
      record.firstAccess = lastAccess;
      record.lastAccess = lastAccess;
      record.accessCount = 1;
      _repository.insertModel(record);
    } else {
      accessLog.lastAccess = lastAccess;
      accessLog.accessCount++;
      _repository.update(accessLog);
    }
  }

  static DateTime? get(String id) {
    return accessCache[id];
  }
}

class TreeNodeManager {
  static final _cache = SplayTreeMap<int, TreeNode?>();

  static Future<TreeNode?> get(int id) async {
    if (_cache.containsKey(id)) return _cache[id];
    final node = await _treeNodeRepository.get(id);
    _cache[id] = node;
    return node;
  }

  static void clear() {
    _cache.clear();
  }
}

class StationNode {
  final int depth;
  final int code;
  final Bounds region;

  Station? station;
  StationNode? left;
  StationNode? right;

  StationNode({
    required this.depth,
    required this.code,
    required this.region,
  });

  Future<StationNode> build(TreeNode data) async {
    await buildTree(data);
    return this;
  }

  Future<void> buildTree(TreeNode data) async {
    station = await _stationRepository.get(data.code);
    if (station == null) throw Exception('Station not found: ${data.code}');
    if (!region.isInsideRect(station!.lat, station!.lng)) throw Exception('Station ${data.code} is out of region');

    final isEven = depth % 2 == 0;

    if (data.left != null) {
      final leftNode = await TreeNodeManager.get(data.left!);
      if (leftNode == null) throw Exception('Node ${data.left} not found');

      final leftNodeRegion = Bounds(
        north: isEven ? region.north : station!.lat,
        south: region.south,
        east: isEven ? station!.lng : region.east,
        west: region.west,
      );

      left = await StationNode(depth: depth + 1, code: leftNode.code, region: leftNodeRegion).build(leftNode);
    }

    if (data.right != null) {
      final rightNode = await TreeNodeManager.get(data.right!);
      if (rightNode == null) throw Exception('Node ${data.right} not found');

      final rightNodeRegion = Bounds(
        north: region.north,
        south: isEven ? region.south : station!.lat,
        east: region.east,
        west: isEven ? station!.lng : region.west,
      );

      right = await StationNode(depth: depth + 1, code: rightNode.code, region: rightNodeRegion).build(rightNode);
    }
  }

  void clear() {
    station = null;
    left?.clear();
    right?.clear();
    left = null;
    right = null;
  }

  Future<Station> get() async {
    if (station != null) return station!;

    final rootNode = await TreeNodeManager.get(code);
    if (rootNode == null) throw Exception('Node $code not found');

    await buildTree(rootNode);

    if (station == null) throw Exception('Station not found');
    return station!;
  }
}

class StationManager extends ChangeNotifier {
  static final _instance = StationManager._internal();

  StationNode? _root;
  bool _locked = false;
  
  double _lastPositionLat = 0;
  double _lastPositionLng = 0;
  bool _serviceAvailable = false;
  final List<StationData> _searchList = [];
  DateTime? _lastUpdatedTime;
  StationData? _currentStation;
  int? _currentMasterLineId;
  List<int>? _currentLineIdRanking;
  Timer? _notificationTimer;

  factory StationManager() {
    return _instance;
  }

  StationManager._internal();

  Future<void> initialize() async {
    TreeNodeManager.clear();
    AccessCacheManager.initialize();

    if (await _treeNodeRepository.count() == 0) return;

    final rootNodeId = int.parse(SystemState.treeNodeRoot);
    final rootNode = await TreeNodeManager.get(rootNodeId);
    if (rootNode == null) {
      print('Root node not found: $rootNodeId, service: ${SystemState.serviceAvailable}');
      return;
    }

    // TODO: 起動時に全てのノードを構築しようとするのを修正したい
    _root = await StationNode(depth: 0, code: rootNode.code, region: Bounds(north: 90, east: 180, south: -90, west: -180)).build(rootNode);
    _serviceAvailable = true;
    print('StationManager initialized');
  }

  void clear() {
    _root?.clear();
    _root = null;
  }

  void cleanup() {
    _notificationTimer?.cancel();
  }

  Future<void> _search(StationNode node, double latitude, double longitude, int maxResults, {int maxDistance = 0}) async {
    var value = 0.0;
    var threshold = 0.0;

    final s = await node.get();
    final d = sqrt(pow(s.lat - latitude, 2) + pow(s.lng - longitude, 2));

    var index = -1;
    var size = _searchList.length;

    if (size > 0 && d < _searchList[size - 1].rawDistance) {
      index = size - 1;
      while (index > 0) {
        if (d >= _searchList[index - 1].rawDistance) break;
        index--;
      }
    } else if(size == 0) {
      index = 0;
    }

    if (index >= 0) {
      _searchList.insert(index, StationData.create(s, d));
      if (size >= maxResults && _searchList[size].rawDistance > maxDistance) _searchList.removeLast();
    }

    final isEven = node.depth % 2 == 0;
    value = isEven ? longitude : latitude;
    threshold = isEven ? s.lng : s.lat;

    final next = value < threshold ? node.left : node.right;
    if (next != null) await _search(next, latitude, longitude, maxResults, maxDistance: maxDistance);

    final opposite = value < threshold ? node.right : node.left;

    if (opposite != null && (value - threshold).abs() < max(_searchList.last.rawDistance, maxDistance)) {
      await _search(opposite, latitude, longitude, maxResults, maxDistance: maxDistance);
    }
  }

  Future<void> _searchRect(StationNode node, Bounds bounds, List<Station> dist, int? maxResults) async {
    final station = await node.get();
    if (maxResults != null && dist.length >= maxResults) return;

    if (bounds.isInsideRect(station.lat, station.lng)) {
      dist.add(station);
    }

    final tasks = <Future<void>>[];

    if (node.left != null && ((node.depth % 2 == 0 && bounds.west < station.lng) || (node.depth % 2 == 1 && bounds.south < station.lat))) {
      tasks.add(_searchRect(node.left!, bounds, dist, maxResults));
    }

    if (node.right != null && ((node.depth % 2 == 0 && bounds.east > station.lng) || (node.depth % 2 == 1 && bounds.north > station.lat))) {
      tasks.add(_searchRect(node.right!, bounds, dist, maxResults));
    }

    await Future.wait(tasks);
  }

  Future<void> updateLocation(double latitude, double longitude, {int maxDistance = 0}) async {
    final maxResults = Config.maxResults;
    if (maxResults <= 0) return;
    if (_root == null) throw Exception('Root node is not initialized');
    if (_locked) return;
    if (_searchList.isNotEmpty && _fixedLatLng(_lastPositionLat) == _fixedLatLng(latitude) && _fixedLatLng(_lastPositionLng) == _fixedLatLng(longitude)) return;

    final stopWatch = Stopwatch();
    stopWatch.start();

    _locked = true;
    _searchList.clear();
    await _search(_root!, latitude, longitude, maxResults, maxDistance: maxDistance);

    final currentStation = _searchList.first;
    final isChanged = _currentStation?.station.code != currentStation.station.code;
    _lastPositionLat = latitude;
    _lastPositionLng = longitude;

    // 最寄り駅が変更されたらアクセスログ等を更新
    final now = DateTime.now();
    if (isChanged) {
      _lastUpdatedTime = now;
      await AccessCacheManager.update(currentStation.station.id, now);

      // 優先表示される路線名を計算
      final lineIdCount = <int, int>{};
      for (final stationData in _searchList) {
        final lines = stationData.station.lines;
        for (final line in lines) {
          lineIdCount[line] = (lineIdCount[line] ?? 0) + 1;
        }
      }

      final lineIdCountList = lineIdCount.entries.toList();
      lineIdCountList.sort((a, b) => b.value.compareTo(a.value));
      final lineIdRanking = lineIdCountList.map((x) => x.key).toList();

      // 付近駅の最頻出路線を元に、最寄り駅が属する路線を決定
      _currentMasterLineId = lineIdRanking.firstWhereOrNull((x) => _searchList.first.station.lines.contains(x));
      _currentLineIdRanking = lineIdRanking;
    }

    // _searchListの中身を更新
    await Future.wait(_searchList.map((x) async {
      final hasMasterLine = x.station.lines.contains(_currentMasterLineId);
      final lineId = (hasMasterLine ? _currentMasterLineId : _currentLineIdRanking?.firstWhereOrNull((line) => x.station.lines.contains(line))) ?? x.station.lines.first;

      x.lineName = (await LineRepository().get(lineId))?.name ?? '不明';
      x.distance = beautifyDistance(measure(latitude, longitude, x.station.lat, x.station.lng));
    }));

    _currentStation = _searchList.first;
    _locked = false;
    notifyListeners();

    final lastAccess = AccessCacheManager.get(_currentStation!.station.id);
    final isCoolDown = getCoolDownTime(_currentStation!.station.id) > 0 && lastAccess!.difference(now).inSeconds != 0;
    if (isChanged) {
      if (!isCoolDown) NotificationManager().showStationNotification(_currentStation!);
      _scheduleNotification();
    }

    print('[${DateFormat('HH:mm:ss').format(now)}] updateLocation: ${stopWatch.elapsedMilliseconds}ms');
  }

  Future<List<Station>> updateRectRegion(double north, double east, double south, double west, {int? maxResults}) async {
    if (_root == null) throw Exception('Root node is not initialized');
    final dist = <Station>[];
    final bounds = Bounds(north: north, east: east, south: south, west: west);
    final stopWatch = Stopwatch();

    stopWatch.start();
    await _searchRect(_root!, bounds, dist, maxResults);

    print('updateRectRegion: ${stopWatch.elapsedMilliseconds}ms');
    return dist;
  }

  void _scheduleNotification() {
    if (_currentStation == null) return;
    _notificationTimer?.cancel();

    final coolDownTime = getCoolDownTime(_currentStation!.station.id);
    if (coolDownTime <= 0) return;

    _notificationTimer = Timer(Duration(seconds: coolDownTime), () async {
      NotificationManager().showStationNotification(_currentStation!, reNotify: true);

      final accessLog = await AccessLogRepository().get(_currentStation?.station.id);
      if (accessLog != null) {
        accessLog.lastAccess = DateTime.now();
        AccessLogRepository().update(accessLog);
      }

      _scheduleNotification();
    });
  }

  double _fixedLatLng(double value) {
    return double.parse(value.toStringAsFixed(5));
  }

  StationData? get currentStation => _currentStation;
  DateTime? get lastUpdatedTime => _lastUpdatedTime;
  bool get serviceAvailable => _serviceAvailable;
  List<StationData> get searchList => _searchList;
}