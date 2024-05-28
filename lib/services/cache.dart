import 'dart:collection';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/tree_node.dart';

class CacheManager {
  static Future<void> initialize() async {
    await Future.wait([
      StationCache.initialize(),
      LineCache.initialize(),
      TreeNodeCache.initialize(),
    ]);
  }
}

class StationCache {
  static final _cache = SplayTreeMap<int, Station>();
  static final _convert = <String, int>{};

  static Future<void> initialize() async {
    final stations = await StationRepository().getAll();
    _cache.clear();
    _convert.clear();
    for (final station in stations) {
      _cache[station.code] = station;
      _convert[station.id] = station.code;
    }
  }

  static Future<Station?> get(int id) async {
    return _cache[id];
  }

  static int convert(String id) {
    return _convert[id]!;
  }
}

class LineCache {
  static final _cache = SplayTreeMap<int, Line>();

  static Future<void> initialize() async {
    final lines = await LineRepository().getAll();
    _cache.clear();
    for (final line in lines) {
      _cache[line.code] = line;
    }
  }

  static Future<Line?> get(int id) async {
    return _cache[id];
  }
}

class TreeNodeCache {
  static final _cache = SplayTreeMap<int, TreeNode>();

  static Future<void> initialize() async {
    final nodes = await TreeNodeRepository().getAll();
    _cache.clear();
    for (final node in nodes) {
      _cache[node.code] = node;
    }
  }

  static Future<TreeNode?> get(int code) async {
    return _cache[code];
  }

  static int count() {
    return _cache.length;
  }
}
