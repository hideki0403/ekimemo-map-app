import 'dart:collection';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/tree_node.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/log.dart';

final logger = Logger('CacheManager');

class CacheManager {
  static final disableCache = Config.disableDbCache;

  static Future<void> initialize() async {
    logger.info('Disable cache: $disableCache');
    if (disableCache) return;

    logger.info('Initializing database cache...');
    final stopWatch = Stopwatch();
    stopWatch.start();

    await Future.wait([
      StationCache.initialize(),
      LineCache.initialize(),
      TreeNodeCache.initialize(),
    ]);

    logger.info('Database cache initialized in ${stopWatch.elapsedMilliseconds}ms');
  }
}

class StationCache {
  static final _repository = StationRepository();
  static final _cache = SplayTreeMap<int, Station>();
  static final _convert = <String, int>{};

  static Future<void> initialize() async {
    final stations = await _repository.getAll();
    _cache.clear();
    _convert.clear();
    for (final station in stations) {
      _cache[station.code] = station;
      _convert[station.id] = station.code;
    }
  }

  static Future<Station?> get(int id) async {
    return !CacheManager.disableCache ? _cache[id] : await _repository.get(id);
  }

  static Future<List<Station>>? getAll () async {
    return !CacheManager.disableCache ? _cache.values.toList() : await _repository.getAll();
  }

  static Future<int> convert(String id) async {
    return !CacheManager.disableCache ? _convert[id]! : (await _repository.get(id, column: 'id'))!.code;
  }

  static Future<List<Station>> search(String query) async {
    return !CacheManager.disableCache ? _cache.values.where((station) => station.originalName.contains(query)).toList() : await _repository.search('original_name', query);
  }
}

class LineCache {
  static final _repository = LineRepository();
  static final _cache = SplayTreeMap<int, Line>();

  static Future<void> initialize() async {
    final lines = await _repository.getAll();
    _cache.clear();
    for (final line in lines) {
      _cache[line.code] = line;
    }
  }

  static Future<Line?> get(int id) async {
    return !CacheManager.disableCache ? _cache[id] : await _repository.get(id);
  }

  static Future<List<Line>> search(String query) async {
    return !CacheManager.disableCache ? _cache.values.where((line) => line.name.contains(query)).toList() : await _repository.search('name', query);
  }
}

class TreeNodeCache {
  static final _repository = TreeNodeRepository();
  static final _cache = SplayTreeMap<int, TreeNode>();

  static Future<void> initialize() async {
    final nodes = await _repository.getAll();
    _cache.clear();
    for (final node in nodes) {
      _cache[node.code] = node;
    }
  }

  static Future<TreeNode?> get(int code) async {
    return !CacheManager.disableCache ? _cache[code] : await _repository.get(code);
  }

  static Future<int> count() async {
    return !CacheManager.disableCache ? _cache.length : await _repository.count();
  }
}
