import 'dart:collection';
import '_abstract.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/services/config.dart';

class LineRepository extends AbstractRepository<Line> {
  static LineRepository? _instance;
  LineRepository._internal() : super(Line(), 'line', 'code');

  factory LineRepository() {
    _instance ??= LineRepository._internal();
    return _instance!;
  }

  static final _useCache = !Config.disableDbCache;
  static final _cache = SplayTreeMap<int, Line>();

  Future<void> buildCache() async {
    if (!_useCache) return;
    final lines = await super.getAll();
    _cache.clear();
    for (final line in lines) {
      _cache[line.code] = line;
    }
  }

  @override
  Future<Line?> getOne(dynamic key, {String? column}) async {
    if (_useCache) {
      if (column == null) {
        return _cache[key];
      } else {
        logger.info('column is specified, fallback to database');
      }
    }
    return super.getOne(key, column: column);
  }

  @override
  Future<List<Line>> search(String query, [String? column]) async {
    if (_useCache) {
      if (column == null) {
        return _cache.values.where((line) => line.name.contains(query)).toList();
      } else {
        logger.info('column is specified, fallback to database');
      }
    }
    return await super.search(query, column ?? 'name');
  }
}
