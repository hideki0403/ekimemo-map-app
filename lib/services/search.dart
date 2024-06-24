import 'dart:async';
import 'dart:math';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'cache.dart';
import 'config.dart';
import 'log.dart';

final logger = Logger('StationSearchService');

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

class StationNode {
  late final int depth;
  late final Bounds region;
  late final String id;
  late final String? leftId;
  late final String? rightId;

  Station? station;
  StationNode? left;
  StationNode? right;

  StationNode({required this.depth, required TreeNode node, required this.region}) {
    id = node.id;
    leftId = node.left;
    rightId = node.right;
  }

  Future<StationNode> build() async {
    station = await StationCache.get(id);
    if (station == null) throw Exception('Station not found: $id');
    if (!region.isInsideRect(station!.lat, station!.lng)) throw Exception('Station $id is out of region');
    return this;
  }

  void clear() {
    station = null;
    left?.clear();
    right?.clear();
    left = null;
    right = null;
  }

  Future<StationNode?> getLeft() async {
    final isEven = depth % 2 == 0;

    if (leftId != null && left == null) {
      final leftNode = await TreeNodeCache.get(leftId!);
      if (leftNode == null) throw Exception('Node $leftId not found');

      final leftNodeRegion = Bounds(
        north: isEven ? region.north : station!.lat,
        south: region.south,
        east: isEven ? station!.lng : region.east,
        west: region.west,
      );

      left = await StationNode(depth: depth + 1, node: leftNode, region: leftNodeRegion).build();
    }

    return left;
  }

  Future<StationNode?> getRight() async {
    final isEven = depth % 2 == 0;

    if (rightId != null && right == null) {
      final rightNode = await TreeNodeCache.get(rightId!);
      if (rightNode == null) throw Exception('Node $rightId not found');

      final rightNodeRegion = Bounds(
        north: region.north,
        south: isEven ? region.south : station!.lat,
        east: region.east,
        west: isEven ? station!.lng : region.west,
      );

      right = await StationNode(depth: depth + 1, node: rightNode, region: rightNodeRegion).build();
    }

    return right;
  }
}

class StationSearchService {
  static StationNode? _root;
  static List<StationData> _searchCache = [];

  static bool get serviceAvailable => _root != null;
  static List<StationData> get list => _searchCache;
  static String get latestProcessingTime => _latestProcessingTime;
  static DateTime? get lastUpdatedTime => _lastUpdatedTime;

  static StationData? _currentStation;
  static double _lastPositionLat = 0;
  static double _lastPositionLng = 0;
  static String _latestProcessingTime = '---';
  static DateTime? _lastUpdatedTime;

  static Future<void> initialize() async {
    if (await TreeNodeCache.count() == 0) return;

    final rootNodeId = SystemState.treeNodeRoot;
    final rootNode = await TreeNodeCache.get(rootNodeId);
    if (rootNode == null) {
      logger.error('Root node not found: $rootNodeId, service: ${SystemState.serviceAvailable}');
      return;
    }

    _root = await StationNode(depth: 0, node: rootNode, region: Bounds(north: 90, east: 180, south: -90, west: -180)).build();
    logger.info('StationSearchService initialized');
  }

  static void clear() {
    _root?.clear();
    _root = null;
  }

  static Future<void> _search(StationNode node, double latitude, double longitude, List<StationData> dist, {int maxResults = 0, int maxDistance = 0}) async {
    var value = 0.0;
    var threshold = 0.0;

    final s = node.station!;
    final d = sqrt(pow(s.lat - latitude, 2) + pow(s.lng - longitude, 2));

    var index = -1;
    var size = dist.length;

    if (size > 0 && d < dist[size - 1].rawDistance) {
      index = size - 1;
      while (index > 0) {
        if (d >= dist[index - 1].rawDistance) break;
        index--;
      }
    } else if(size == 0) {
      index = 0;
    }

    if (index >= 0) {
      dist.insert(index, StationData.create(s, d));
      if (size >= maxResults && dist[size].rawDistance > maxDistance) dist.removeLast();
    }

    final isEven = node.depth % 2 == 0;
    value = isEven ? longitude : latitude;
    threshold = isEven ? s.lng : s.lat;

    final next = value < threshold ? await node.getLeft() : await node.getRight();
    if (next != null) await _search(next, latitude, longitude, dist, maxResults: maxResults, maxDistance: maxDistance);

    final opposite = value < threshold ? await node.getRight() : await node.getLeft();

    if (opposite != null && (value - threshold).abs() < max(dist.last.rawDistance, maxDistance)) {
      await _search(opposite, latitude, longitude, dist, maxResults: maxResults, maxDistance: maxDistance);
    }
  }

  static Future<void> _searchRect(StationNode node, Bounds bounds, List<Station> dist, int? maxResults) async {
    final station = node.station!;
    if (maxResults != null && dist.length >= maxResults) return;

    if (bounds.isInsideRect(station.lat, station.lng)) {
      dist.add(station);
    }

    final tasks = <Future<void>>[];

    if (node.leftId != null && ((node.depth % 2 == 0 && bounds.west < station.lng) || (node.depth % 2 == 1 && bounds.south < station.lat))) {
      tasks.add(_searchRect((await node.getLeft())!, bounds, dist, maxResults));
    }

    if (node.rightId != null && ((node.depth % 2 == 0 && bounds.east > station.lng) || (node.depth % 2 == 1 && bounds.north > station.lat))) {
      tasks.add(_searchRect((await node.getRight())!, bounds, dist, maxResults));
    }

    await Future.wait(tasks);
  }

  static Future<(bool updated, StationData? station)> updateLocation(double latitude, double longitude, {int maxDistance = 0}) async {
    final maxResults = Config.maxResults;
    if (maxResults <= 0) return (false, null);
    if (!serviceAvailable) throw Exception('StationSearchService not initialized');

    if (_searchCache.isNotEmpty && _fixedLatLng(_lastPositionLat) == _fixedLatLng(latitude) && _fixedLatLng(_lastPositionLng) == _fixedLatLng(longitude)) {
      logger.debug('Skip updateLocation: same position');
      return (false, _currentStation);
    }

    final stopWatch = Stopwatch();
    stopWatch.start();

    final dist = <StationData>[];
    await _search(_root!, latitude, longitude, dist, maxResults: maxResults, maxDistance: maxDistance);
    _searchCache = dist;

    final isUpdated = _currentStation == null || _currentStation!.station.id != _searchCache.first.station.id;
    final elapsed = (stopWatch.elapsedMicroseconds / 1000).toStringAsFixed(1);

    _currentStation = _searchCache.first;
    _lastPositionLat = latitude;
    _lastPositionLng = longitude;
    _latestProcessingTime = elapsed;
    _lastUpdatedTime = DateTime.now();

    logger.debug('Update location: ${elapsed}ms');
    if (isUpdated) logger.info('Nearest station changed: ${_currentStation!.station.name} (${_currentStation!.station.id})');

    return (isUpdated, _currentStation);
  }

  static Future<List<Station>> updateRectRegion(double north, double east, double south, double west, {int? maxResults}) async {
    if (!serviceAvailable) throw Exception('StationSearchService not initialized');
    final dist = <Station>[];
    final bounds = Bounds(north: north, east: east, south: south, west: west);
    final stopWatch = Stopwatch();

    stopWatch.start();
    await _searchRect(_root!, bounds, dist, maxResults);

    logger.debug('Update rect region: ${stopWatch.elapsedMilliseconds}ms');
    return dist;
  }

  static Future<List<StationData>> getNearestStations(double latitude, double longitude, { bool withLineData = false, int maxResults = 1}) async {
    if (!serviceAvailable) throw Exception('StationSearchService not initialized');
    final dist = <StationData>[];
    await _search(_root!, latitude, longitude, dist, maxResults: maxResults);

    if (withLineData) {
      for (final data in dist) {
        final lineData = await LineCache.get(data.station.lines.first);
        data.lineName = lineData?.name;
      }
    }

    return dist;
  }

  static double _fixedLatLng(double value) {
    return double.parse(value.toStringAsFixed(5));
  }
}