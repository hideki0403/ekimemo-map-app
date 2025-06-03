import 'package:uuid/uuid.dart';

import 'package:ekimemo_map/models/move_log.dart';
import 'package:ekimemo_map/models/move_log_session.dart';
import 'package:ekimemo_map/repository/move_log.dart';
import 'package:ekimemo_map/repository/move_log_session.dart';

import 'config.dart';
import 'gps.dart';

class MovementLogService {
  static final _moveLogRepository = MoveLogRepository();
  static final _moveLogSessionRepository = MoveLogSessionRepository();

  static String? _currentSessionId;

  static void initialize() {
    GpsManager.addLocationListener(_locationHandler);
    GpsManager.addGpsToggleListener(_gpsToggleHandler);
  }

  static void _locationHandler(double latitude, double longitude, double accuracy) {
    if (!Config.enableMovementLog) return;

    if (_currentSessionId == null) {
      _currentSessionId = const Uuid().v4();
      final session = MoveLogSession();
      session.id = _currentSessionId!;
      session.startTime = DateTime.now();
      _moveLogSessionRepository.insertModel(session);
    }

    final moveLog = MoveLog();
    moveLog.sessionId = _currentSessionId!;
    moveLog.timestamp = DateTime.now();
    moveLog.latitude = latitude;
    moveLog.longitude = longitude;
    moveLog.accuracy = accuracy;
    moveLog.speed = GpsManager.lastLocation!.speed;
    _moveLogRepository.insert(moveLog.toMapWithoutId());
  }

  static void _gpsToggleHandler(bool isEnabled) {
    if (isEnabled) return;
    _currentSessionId = null;
  }

  static Future<List<MoveLogSession>> getSessions() async {
    return await _moveLogSessionRepository.getAll();
  }

  static Future<MoveLogSession?> getSession(String id) async {
    return await _moveLogSessionRepository.getOne(id);
  }

  static Future<List<MoveLog>> getLogById(String id) async {
    return await _moveLogRepository.get([id], column: 'session_id');
  }

  static Future<List<MoveLog>> getLog(MoveLogSession session) async {
    return await getLogById(session.id);
  }

  static Future<void> deleteSession(MoveLogSession session) async {
    await _moveLogRepository.delete(session.id, column: 'session_id');
    await _moveLogSessionRepository.delete(session.id);
  }
}
