import 'dart:collection';
import '_abstract.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/services/config.dart';

class StationRepository extends AbstractRepository<Station> {
  static StationRepository? _instance;
  StationRepository._internal() : super(Station(), 'station', 'id');

  factory StationRepository() {
    _instance ??= StationRepository._internal();
    return _instance!;
  }

  static final _useCache = !Config.disableDbCache;
  static final _cache = SplayTreeMap<String, Station>();

  Future<void> buildCache() async {
    if (!_useCache) return;
    final stations = await super.getAll();
    _cache.clear();
    for (final station in stations) {
      _cache[station.id] = station;
    }
  }

  @override
  Future<Station?> getOne(dynamic key, {String? column}) async {
    if (_useCache) {
      if (column == null) {
        return _cache[key.toString()];
      } else {
        logger.info('column is specified, fallback to database');
      }
    }
    return super.getOne(key, column: column);
  }

  @override
  Future<List<Station>> getAll() async {
    return _useCache ? _cache.values.toList() : await super.getAll();
  }

  @override
  Future<List<Station>> search(String query, [String? column]) async {
    if (_useCache) {
      if (column == null) {
        return _cache.values.where((station) => station.originalName.contains(query)).toList();
      } else {
        logger.info('column is specified, fallback to database');
      }
    }
    return await super.search(query, column ?? 'original_name');
  }
}
