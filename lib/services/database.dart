import 'package:sqflite/sqflite.dart';

enum DatabaseType {
  station,
  line,
  treeNodes,
  accessLog,
}

class DatabaseHandler {
  final int _version = 1;
  final Map<String, List<String>> _migration = {
    '1': [
      // create table
      'CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT);',
      'CREATE TABLE IF NOT EXISTS station (id TEXT PRIMARY KEY, code INTEGER, name TEXT, original_name TEXT, name_kana TEXT, closed INTEGER, lat REAL, lng REAL, prefecture INTEGER, lines TEXT, attr TEXT, next TEXT, voronoi TEXT);',
      'CREATE TABLE IF NOT EXISTS line (id TEXT PRIMARY KEY, code INTEGER, name TEXT, name_kana TEXT, station_size INTEGER, company_code INTEGER, closed INTEGER, color TEXT, station_list TEXT, polyline_list TEXT);',
      'CREATE TABLE IF NOT EXISTS tree_node (id TEXT PRIMARY KEY, code INTEGER, name TEXT, lat REAL, lng REAL, left INTEGER, right INTEGER);',
      'CREATE TABLE IF NOT EXISTS access_log (id TEXT PRIMARY KEY, first_access TEXT, last_access TEXT, access_count INTEGER);',
      // create index
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_meta ON meta (key);',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_station ON station (id, code);',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_line ON line (id, code);',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_tree_node ON tree_node (id, code);',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_access_log ON access_log (id);',
    ],
  };

  void _migrate(Database db, int previousVersion, int oldVersion) async {
    for(int i = previousVersion + 1; i <= oldVersion; i++){
      List<String>? queries = _migration[i.toString()];
      if(queries == null) continue;

      for (String query in queries) {
        await db.execute(query);
      }
    }
  }

  Database? _db;
  Future<Database> get db async {
    _db ??= await openDatabase('database.db',
      version: _version,
      onCreate: (db, version) async {
        _migrate(db, 0, version);
      },
      onUpgrade: (db, oldVersion, newVersion) async {
        _migrate(db, oldVersion, newVersion);
      },
    );
    return _db!;
  }

  Future<void> reset() async {
    await deleteDatabase('database.db');
    _db = null;
  }
}