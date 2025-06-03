// ignore_for_file: non_constant_identifier_names

import 'package:sqflite/sqflite.dart';
import 'log.dart';
import 'const.dart';

final _logger = Logger('DatabaseHandler');

enum DatabaseType {
  station,
  line,
  treeNodes,
  accessLog,
}

class DatabaseHandler {
  static const int _version = 9;
  static final Map<String, List<dynamic>> _migration = {
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
    '2': [
      // add 'name_formal' column to 'line' table
      'ALTER TABLE line ADD COLUMN name_formal TEXT;',
    ],
    '3': [
      // add 'accessed' column to 'access_log' table
      'ALTER TABLE access_log ADD COLUMN accessed INTEGER DEFAULT 1;',
    ],
    '4': [
      // add 'delaunay' column to 'station' table
      'ALTER TABLE station ADD COLUMN delaunay TEXT DEFAULT "[]";',
    ],
    '5': [
      // create 'passing_log' table
      'CREATE TABLE IF NOT EXISTS passing_log (uuid TEXT PRIMARY KEY, id TEXT, timestamp TEXT, latitude REAL, longitude REAL, speed REAL, accuracy REAL, distance INTEGER, isReNotify INTEGER);',
    ],
    '6': [
      // remove 'code', 'name', 'lat', 'lng' columns from 'tree_node' table
      'CREATE TABLE IF NOT EXISTS tree_node_new (id TEXT PRIMARY KEY, left TEXT, right TEXT);',
      'INSERT INTO tree_node_new SELECT id, left, right FROM tree_node;',
      'DROP TABLE tree_node;',
      'ALTER TABLE tree_node_new RENAME TO tree_node;',
    ],
    '7': [
      // create 'interval_timer' table
      'CREATE TABLE IF NOT EXISTS interval_timer (id TEXT PRIMARY KEY, duration INTEGER, enable_notification INTEGER, notification_sound TEXT, vibration_pattern TEXT);',
    ],
    '8': [
      // add 'name', 'enable_tts' column to 'interval_timer' table
      'ALTER TABLE interval_timer ADD COLUMN name TEXT;',
      'ALTER TABLE interval_timer ADD COLUMN enable_tts INTEGER;',
    ],
    '9': [
      // change type of 'id' column in 'station' table from TEXT to INTEGER
      'DROP INDEX IF EXISTS idx_station;',
      'DROP TABLE IF EXISTS station;',
      'CREATE TABLE IF NOT EXISTS station (id INTEGER PRIMARY KEY, code INTEGER, name TEXT, original_name TEXT, name_kana TEXT, closed INTEGER, lat REAL, lng REAL, prefecture INTEGER, lines TEXT, attr TEXT, next TEXT, voronoi TEXT, delaunay TEXT DEFAULT "[]");',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_station ON station (id, code);',

      // change type of 'id' column in 'line' table from TEXT to INTEGER
      'DROP INDEX IF EXISTS idx_line;',
      'DROP TABLE IF EXISTS line;',
      'CREATE TABLE IF NOT EXISTS line (id INTEGER PRIMARY KEY, code INTEGER, name TEXT, name_kana TEXT, name_formal TEXT, station_size INTEGER, company_code INTEGER, closed INTEGER, color TEXT, station_list TEXT, polyline_list TEXT);',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_line ON line (id, code);',

      // change type of 'id', 'left', 'right' column in 'tree_node' table from TEXT to INTEGER
      'DROP INDEX IF EXISTS idx_tree_node;',
      'DROP TABLE IF EXISTS tree_node;',
      'CREATE TABLE IF NOT EXISTS tree_node (id INTEGER PRIMARY KEY, left INTEGER, right INTEGER);',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_tree_node ON tree_node (id);',

      // change type of 'id' column in 'access_log', 'passing_log' table from TEXT to INTEGER
      'CREATE TABLE IF NOT EXISTS _temp_id_mapping_table (old_id TEXT PRIMARY KEY, new_id INTEGER);',
      Migration.m_1747502120_StationIdBreakingChange, // insert mapping kvp to _temp_id_mapping_table

      // - migrate 'passing_log' table
      'ALTER TABLE passing_log ADD COLUMN new_id INTEGER;',
      'UPDATE passing_log SET new_id = (SELECT new_id FROM _temp_id_mapping_table WHERE _temp_id_mapping_table.old_id = passing_log.id);',
      'ALTER TABLE passing_log DROP COLUMN id;',
      'ALTER TABLE passing_log RENAME COLUMN new_id TO id;',

      // - migrate 'access_log' table
      'CREATE TABLE IF NOT EXISTS access_log_new (id INTEGER PRIMARY KEY, first_access TEXT, last_access TEXT, access_count INTEGER, accessed INTEGER DEFAULT 1);',
      'INSERT INTO access_log_new (id, first_access, last_access, access_count, accessed) SELECT mapping_table.new_id, first_access, last_access, access_count, accessed FROM access_log INNER JOIN _temp_id_mapping_table mapping_table ON access_log.id = mapping_table.old_id;',
      'DROP INDEX IF EXISTS idx_access_log;',
      'DROP TABLE IF EXISTS access_log;',
      'ALTER TABLE access_log_new RENAME TO access_log;',
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_access_log ON access_log (id);',

      // - drop _temp_id_mapping_table
      'DROP TABLE IF EXISTS _temp_id_mapping_table;',
    ],
    '10': [
      // create 'move_log', 'move_log_session' table
      'CREATE TABLE IF NOT EXISTS move_log (id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT, timestamp INTEGER, latitude REAL, longitude REAL, speed REAL, accuracy REAL);',
      'CREATE TABLE IF NOT EXISTS move_log_session (id TEXT PRIMARY KEY, start_time INTEGER);',

      // create index for 'move_log', 'move_log_session'
      'CREATE INDEX idx_move_log ON move_log (session_id, timestamp);',
      'CREATE INDEX idx_move_log_session ON move_log_session (start_time);',
    ]
  };

  static Future<void> _migrate(Database db, int previousVersion, int oldVersion) async {
    for (int i = previousVersion + 1; i <= oldVersion; i++) {
      final queries = _migration[i.toString()];
      if (queries == null) continue;

      _logger.info('Running migration for version "$i"');

      for (final query in queries) {
        if (query is String) {
          _logger.info('Executing query: $query');
          await db.execute(query);
        } else if (query is Future<void> Function(Database)) {
          _logger.info('Executing migration function');
          await query(db);
        }
      }
    }
  }

  static Database? _db;

  static Future<void> init() async {
    _db = await openDatabase(
      'database.db',
      version: _version,
      onCreate: (db, version) async {
        await _migrate(db, 0, version);
        _logger.info('Database created');
      },
      onUpgrade: (db, oldVersion, newVersion) async {
        _logger.info('Migrating database...');
        await _migrate(db, oldVersion, newVersion);
        _logger.info('Database migrated: $oldVersion -> $newVersion');
      },
    );

    _logger.info('Database initialized');
  }

  static Future<Database> get db async {
    if (_db == null) await init();
    return _db!;
  }

  static Future<void> reset() async {
    await deleteDatabase('database.db');
    _db = null;
  }
}

class Migration {
  static Future<void> m_1747502120_StationIdBreakingChange(Database db) async {
    final batch = db.batch();
    stationIdMappingTable.forEach((oldId, newId) {
      batch.insert(
        '_temp_id_mapping_table',
        {
          'old_id': oldId,
          'new_id': newId,
        },
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    });
    await batch.commit(noResult: true);
  }
}
