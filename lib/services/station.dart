import 'dart:async';
import 'dart:ffi';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/tree_segments.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'config.dart';
import 'notification.dart';
import 'utils.dart';

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
  DateTime? firstAccess;
  DateTime? lastAccess;
  int accessCount;
  bool isNew;
  int? index;

  StationData(this.station, this.rawDistance, {this.distance, this.firstAccess, this.lastAccess, this.accessCount = 0, this.isNew = true, this.index});

  factory StationData.create(Station station, double rawDistance) {
    return StationData(station, rawDistance);
  }
}

class StationNode {
  final int depth;
  final int code;
  final Bounds region;

  String? segmentName;
  Station? station;
  StationNode? left;
  StationNode? right;

  StationNode({
    required this.depth,
    required this.code,
    required this.region,
  });

  Future<StationNode> build(TreeNode data, Map<int, TreeNode> dataMap) async {
    await buildTree(data, dataMap);
    return this;
  }

  Future<void> buildTree(TreeNode data, Map<int, TreeNode> dataMap) async {
    // LeafNode
    if (data.segment != null) {
      segmentName = data.segment;
      return;
    }

    station = await StationRepository().get(data.code);
    if (station == null) throw Exception('Station not found: ${data.code}');
    if (!region.isInsideRect(station!.lat, station!.lng)) throw Exception('Station ${data.code} is out of region');

    final isEven = depth % 2 == 0;

    if (data.left != null) {
      final leftNode = dataMap[data.left];
      if (leftNode == null) throw Exception('Node ${data.left} not found');

      final leftNodeRegion = Bounds(
        north: isEven ? region.north : station!.lat,
        south: region.south,
        east: isEven ? station!.lng : region.east,
        west: region.west,
      );

      left = await StationNode(depth: depth + 1, code: leftNode.code, region: leftNodeRegion).build(leftNode, dataMap);
    }

    if (data.right != null) {
      final rightNode = dataMap[data.right];
      if (rightNode == null) throw Exception('Node ${data.right} not found');

      final rightNodeRegion = Bounds(
        north: region.north,
        south: isEven ? region.south : station!.lat,
        east: region.east,
        west: isEven ? station!.lng : region.west,
      );

      right = await StationNode(depth: depth + 1, code: rightNode.code, region: rightNodeRegion).build(rightNode, dataMap);
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
    if (segmentName == null) throw Exception('Segment name not found');

    final treeSegment = await TreeSegmentsRepository().get(segmentName!);
    if (treeSegment == null) throw Exception('Tree segment ${segmentName!} not found');

    if (treeSegment.root != code) throw Exception('Root node is not matched');

    final dataMap = <int, TreeNode>{};
    for (final rawNode in treeSegment.nodeList) {
      final node = TreeNode().fromMap(rawNode);
      dataMap[node.code] = node;
    }

    final rootNode = dataMap[treeSegment.root];
    if (rootNode == null) throw Exception('Root node ${treeSegment.root} not found');

    await buildTree(rootNode, dataMap);

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
  Timer? _notificationTimer;

  factory StationManager() {
    return _instance;
  }

  StationManager._internal();

  Future<void> initialize({String rootName = 'root'}) async {
    if (await TreeSegmentsRepository().count() == 0) return;

    final treeSegment = await TreeSegmentsRepository().get(rootName);
    if (treeSegment == null) throw Exception('Tree segment $rootName not found');

    final dataMap = <int, TreeNode>{};
    for (final rawNode in treeSegment.nodeList) {
      final node = TreeNode().fromMap(rawNode);
      dataMap[node.code] = node;
    }

    final rootNode = dataMap[treeSegment.root];
    if (rootNode == null) throw Exception('Root node ${treeSegment.root} not found');

    _root = await StationNode(depth: 0, code: rootNode.code, region: Bounds(north: 90, east: 180, south: -90, west: -180)).build(rootNode, dataMap);
    _serviceAvailable = true;
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

      final accessLog = await AccessLogRepository().get(currentStation.station.id);
      if (accessLog == null) {
        final record = AccessLog();
        record.id = currentStation.station.id;
        record.firstAccess = now;
        record.lastAccess = now;
        record.accessCount = 1;
        AccessLogRepository().insertModel(record);
      } else if(getCoolDownTimeFromAccessLog(accessLog) == 0) {
        accessLog.lastAccess = now;
        accessLog.accessCount++;
        AccessLogRepository().update(accessLog);
      }
    }

    // _searchListの中身を更新
    await Future.wait(_searchList.map((x) async {
      x.distance = beautifyDistance(measure(latitude, longitude, x.station.lat, x.station.lng));
      final accessLog = await AccessLogRepository().get(x.station.id);
      if (accessLog == null) return;
      x.isNew = false;
      x.accessCount = accessLog.accessCount;
      x.firstAccess = accessLog.firstAccess;
      x.lastAccess = accessLog.lastAccess;
    }));

    _currentStation = _searchList.first;
    _locked = false;
    notifyListeners();

    final isCoolDown = getCoolDownTime(_currentStation!) > 0 && _currentStation!.lastAccess!.difference(now).inSeconds != 0;
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

    final coolDownTime = getCoolDownTime(_currentStation!);
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