import 'dart:math';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:uuid/uuid.dart';

import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/models/move_log.dart';
import 'package:ekimemo_map/models/move_log_session.dart';
import 'package:ekimemo_map/repository/move_log.dart';
import 'package:ekimemo_map/repository/move_log_session.dart';

import 'config.dart';
import 'gps.dart';

typedef MoveLogSessionMap = Map<MoveLogSession, List<MoveLog>>;
typedef _Point = (double latitude, double longitude);

class MovementLogService {
  static final _moveLogRepository = MoveLogRepository();
  static final _moveLogSessionRepository = MoveLogSessionRepository();
  static final DateFormat _dateFormat = DateFormat('H時mm分');

  static String? _currentSessionId;
  static _Point? _lastLocation;

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

    if (_lastLocation != null && Config.minimumMovementDistance != 0) {
      final distance = _DistanceCalcurator.calculate(
        _lastLocation!,
        (latitude, longitude),
      );

      if (distance < Config.minimumMovementDistance) return;
    }

    _lastLocation = (latitude, longitude);

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

  static Future<MoveLogSessionMap> getLogsFromSessions(List<MoveLogSession> sessions) async {
    final sessionEntries = await Future.wait(
      sessions.map((session) {
        return getLog(session).then((logs) => MapEntry(session, logs));
      }),
    );

    return Map.fromEntries(sessionEntries.where((entry) => entry.value.isNotEmpty));
  }

  static Future<MoveLogSessionMap> getSessionsWithLogs(List<String> sessionIds) async {
    final sessionRecords = await Future.wait(
      sessionIds.map((id) => getSession(id)),
    ).then((sessions) => sessions.whereType<MoveLogSession>().toList());
    return getLogsFromSessions(sessionRecords);
  }

  static Future<void> deleteSession(MoveLogSession session) async {
    await _moveLogRepository.delete(session.id, column: 'session_id');
    await _moveLogSessionRepository.delete(session.id);
  }

  static Future<void> deleteSessionsDialog(MoveLogSessionMap allSessions, Function(MoveLogSession session) onDeleted) async {
    final sessions = allSessions.entries
        .map((entry) => MapEntry(entry.key.id, '${_dateFormat.format(entry.value.first.timestamp)}~${_dateFormat.format(entry.value.last.timestamp)}\n${entry.value.length}地点を含むデータ'));
    showDeleteDialog(
      title: '移動ログの削除',
      data: Map.fromEntries(sessions),
      icon: Icons.route_rounded,
      onDelete: (id) async {
        final session = allSessions.entries.firstWhereOrNull((entry) => entry.key.id == id);
        if (session == null) return false;

        final result = await showConfirmDialog(
          title: '移動ログの削除',
          caption: '${session.value.length}件の移動ログを削除しますか？\nこの操作は取り消せません。',
        );
        if (result != true) return false;

        await deleteSession(session.key);
        onDeleted(session.key);

        return true;
      },
    );
  }
}

class _DistanceCalcurator {
  static const earthRadius = 6378.137;

  static double deg2rad(double deg) {
    return deg * (pi / 180);
  }

  static double calculate(_Point p1, _Point p2) {
    final (double degLat1, double degLon1) = p1;
    final (double degLat2, double degLon2) = p2;

    final lat1 = deg2rad(degLat1);
    final lon1 = deg2rad(degLon1);
    final lat2 = deg2rad(degLat2);
    final lon2 = deg2rad(degLon2);

    return 1000 * earthRadius * acos(
      sin(lat1) * sin(lat2) +
      cos(lat1) * cos(lat2) * cos(lon2 - lon1),
    );
  }
}
