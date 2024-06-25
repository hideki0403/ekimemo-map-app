import 'package:sqflite/sqflite.dart';
import 'package:ekimemo_map/models/_abstract.dart';
import 'package:ekimemo_map/services/database.dart';
import 'package:ekimemo_map/services/log.dart';

abstract class AbstractRepository<T extends AbstractModel> {
  static Database? _database;
  late final String _tableName;
  late final String _primaryKey;
  late final T _model;
  late final Logger logger;

  AbstractRepository(T model, String tableName, String primaryKey) {
    _tableName = tableName;
    _primaryKey = primaryKey;
    _model = model;

    logger = Logger('DB:$_tableName');
  }

  static _initialize() async {
    _database = await DatabaseHandler.db;
  }

  /// データベースからレコードを1件取得します。
  Future<T?> get(dynamic key, {String? column}) async {
    if (_database == null) await _initialize();

    final stringKey = key.toString();
    final targetColumn = column ?? _primaryKey;
    try {
      final List<Map<String, dynamic>> maps = await _database!.query(
        _tableName,
        where: '$targetColumn = ?',
        whereArgs: [stringKey],
      );
      if (maps.isEmpty) return null;
      return _model.fromMap(maps[0]);
    } catch (e) {
      logger.error('Failed to get $targetColumn: $stringKey');
      return null;
    }
  }

  /// データベースから全てのレコードを取得します。
  Future<List<T>> getAll() async {
    if (_database == null) await _initialize();
    try {
      final List<Map<String, dynamic>> maps = await _database!.query(_tableName);
      return List.generate(maps.length, (i) {
        return _model.fromMap(maps[i]);
      });
    } catch (e) {
      logger.error('Failed to get all records from $_tableName');
      return [];
    }
  }

  /// データベースから全てのレコードをMapで取得します。キャッシュは無視されます。
  Future<Map<K, Map<String, dynamic>>> getAllMap<K>() async {
    if (_database == null) await _initialize();
    try {
      final List<Map<String, dynamic>> maps = await _database!.query(_tableName);
      final result = <K, Map<String, dynamic>>{};
      for (Map<String, dynamic> map in maps) {
        result[map[_primaryKey]] = map;
      }
      return result;
    } catch (e) {
      logger.error('Failed to get all records (map) from $_tableName');
      return {};
    }
  }

  /// データベースにレコードを挿入します。
  Future<void> insert(Map<String, dynamic> data) async {
    if (_database == null) await _initialize();
    try {
      await _database!.insert(
        _tableName,
        data,
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    } catch (e) {
      logger.error('Failed to insert data to $_tableName');
    }
  }

  /// データベースに複数のレコードを挿入します。
  Future<void> bulkInsert(List<Map<String, dynamic>> data) async {
    if (_database == null) await _initialize();
    try {
      final batch = _database!.batch();
      for (Map<String, dynamic> record in data) {
        batch.insert(
          _tableName,
          record,
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      await batch.commit(noResult: true);
    } catch (e) {
      logger.error('Failed to bulk insert data to $_tableName');
    }
  }

  /// データベースにレコードを挿入します。
  Future<void> insertModel(T model) async {
    if (_database == null) await _initialize();
    try {
      await _database!.insert(
        _tableName,
        model.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    } catch (e) {
      logger.error('Failed to insert model to $_tableName');
    }
  }

  /// データベースに複数のレコードを挿入します。
  Future<void> bulkInsertModel(List<T> models) async {
    if (_database == null) await _initialize();
    try {
      final batch = _database!.batch();
      for (T model in models) {
        batch.insert(
          _tableName,
          model.toMap(),
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      await batch.commit(noResult: true);
    } catch (e) {
      logger.error('Failed to bulk insert model to $_tableName');
    }
  }

  /// データベースのレコードを更新します。
  Future<void> update(T model) async {
    if (_database == null) await _initialize();
    try {
      final mappedModel = model.toMap();
      await _database!.update(
        _tableName,
        mappedModel,
        where: '$_primaryKey = ?',
        whereArgs: [mappedModel[_primaryKey]],
      );
    } catch (e) {
      logger.error('Failed to update model in $_tableName');
    }
  }

  /// データベースのレコードを削除します。
  Future<void> delete(dynamic key) async {
    if (_database == null) await _initialize();
    try {
      await _database!.delete(
        _tableName,
        where: '$_primaryKey = ?',
        whereArgs: [key],
      );
    } catch (e) {
      logger.error('Failed to delete record from $_tableName');
    }
  }

  /// データベースの全てのレコードを削除します。
  Future<void> clear() async {
    if (_database == null) await _initialize();
    try {
      await _database!.delete(_tableName);
    } catch (e) {
      logger.error('Failed to clear $_tableName');
    }
  }

  /// データベースのレコード数を取得します。
  Future<int> count() async {
    if (_database == null) await _initialize();
    try {
      final List<Map<String, dynamic>> maps = await _database!.query(_tableName, columns: [_primaryKey]);
      return maps.length;
    } catch (e) {
      logger.error('Failed to count records in $_tableName');
      return 0;
    }
  }

  /// データベースのレコードを検索します。
  Future<List<T>> search(String query, [String? column]) async {
    if (_database == null) await _initialize();
    try {
      final List<Map<String, dynamic>> maps = await _database!.query(
        _tableName,
        where: '${column ?? _primaryKey } LIKE ?',
        whereArgs: ['%$query%'],
      );
      return List.generate(maps.length, (i) {
        return _model.fromMap(maps[i]);
      });
    } catch (e) {
      logger.error('Failed to search records in $_tableName');
      return [];
    }
  }
}