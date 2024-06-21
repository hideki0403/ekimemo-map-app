import 'package:sqflite/sqflite.dart';
import 'package:ekimemo_map/models/_abstract.dart';
import 'package:ekimemo_map/services/database.dart';

abstract class AbstractRepository<T extends AbstractModel> {
  static Database? _database;
  late final String _tableName;
  late final String _primaryKey;
  late final T _model;

  AbstractRepository(T model, String tableName, String primaryKey) {
    _tableName = tableName;
    _primaryKey = primaryKey;
    _model = model;
  }

  static _initialize() async {
    _database = await DatabaseHandler.db;
  }

  Future<T?> get(dynamic key, {String? column}) async {
    if (_database == null) await _initialize();
    final stringKey = key.toString();
    final targetColumn = column ?? _primaryKey;

    final List<Map<String, dynamic>> maps = await _database!.query(
      _tableName,
      where: '$targetColumn = ?',
      whereArgs: [stringKey],
    );
    if (maps.isEmpty) return null;
    return _model.fromMap(maps[0]);
  }

  Future<List<T>> getAll() async {
    if (_database == null) await _initialize();
    final List<Map<String, dynamic>> maps = await _database!.query(_tableName);
    return List.generate(maps.length, (i) {
      return _model.fromMap(maps[i]);
    });
  }

  Future<Map<K, Map<String, dynamic>>> getAllMap<K>() async {
    if (_database == null) await _initialize();
    final List<Map<String, dynamic>> maps = await _database!.query(_tableName);
    final result = <K, Map<String, dynamic>>{};
    for (Map<String, dynamic> map in maps) {
      result[map[_primaryKey]] = map;
    }
    return result;
  }

  Future<void> insert(Map<String, dynamic> data) async {
    if (_database == null) await _initialize();
    await _database!.insert(
      _tableName,
      data,
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> bulkInsert(List<Map<String, dynamic>> data) async {
    if (_database == null) await _initialize();
    final batch = _database!.batch();
    for (Map<String, dynamic> record in data) {
      batch.insert(
        _tableName,
        record,
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    }
    await batch.commit(noResult: true);
  }

  Future<void> insertModel(T model) async {
    if (_database == null) await _initialize();
    await _database!.insert(
      _tableName,
      model.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> bulkInsertModel(List<T> models) async {
    if (_database == null) await _initialize();
    final batch = _database!.batch();
    for (T model in models) {
      batch.insert(
        _tableName,
        model.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    }
    await batch.commit(noResult: true);
  }

  Future<void> update(T model) async {
    if (_database == null) await _initialize();
    final mappedModel = model.toMap();

    await _database!.update(
      _tableName,
      mappedModel,
      where: '$_primaryKey = ?',
      whereArgs: [mappedModel[_primaryKey]],
    );
  }

  Future<void> delete(dynamic key) async {
    if (_database == null) await _initialize();
    await _database!.delete(
      _tableName,
      where: '$_primaryKey = ?',
      whereArgs: [key],
    );
  }

  Future<void> clear() async {
    if (_database == null) await _initialize();
    await _database!.delete(_tableName);
  }

  Future<int> count() async {
    if (_database == null) await _initialize();
    final List<Map<String, dynamic>> maps = await _database!.query(_tableName, columns: [_primaryKey]);
    return maps.length;
  }

  Future<List<T>> search(String column, String query) async {
    if (_database == null) await _initialize();
    final List<Map<String, dynamic>> maps = await _database!.query(
      _tableName,
      where: '$column LIKE ?',
      whereArgs: ['%$query%'],
    );
    return List.generate(maps.length, (i) {
      return _model.fromMap(maps[i]);
    });
  }
}