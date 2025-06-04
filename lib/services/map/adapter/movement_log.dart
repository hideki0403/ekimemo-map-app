import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:ekimemo_map/services/movement_log.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/radar.dart';
import '../map_adapter.dart';
import '../utils.dart';

class MovementLogMapAdapter extends MapAdapter {
  MovementLogMapAdapter(super.parent);

  final DateFormat _dateFormat = DateFormat('H時mm分');

  MoveLogSessionMap _allSessions = {};
  List<String> _ignoredSessionIds = [];
  LatLngBounds? _cameraBounds;

  @override
  String? get title => '移動ログ';

  @override
  List<Widget> get floatingWidgets => [
    ElevatedButton(
      onPressed: _cameraBounds == null ? null : () => resetCameraBounds(),
      child: Text('表示範囲をリセット'),
    ),
    ElevatedButton(
      onPressed: () async {
        final displayValuePair = _allSessions.entries.map((session) => MapEntry('${_dateFormat.format(session.value.first.timestamp)}~${_dateFormat.format(session.value.last.timestamp)}\n(${session.value.length}地点)', session.key.id)).toList();
        final mappingTable = Map.fromEntries(displayValuePair);
        final currentState = Map.fromEntries(displayValuePair.map((x) => MapEntry(x.key, !_ignoredSessionIds.contains(x.value))));
        final result = await showCheckboxDialog(data: currentState, title: '表示するセッション', icon: Icons.route_rounded);
        if (result == null) return;

        _ignoredSessionIds = result.entries.where((x) => !x.value).map((x) => mappingTable[x.key]).whereType<String>().toList();
        updateSessions();
      },
      child: Text('表示するセッションを選択'),
    ),
  ];

  @override
  List<Widget> get appBarActions => [
    IconButton(
      icon: const Icon(Icons.delete_forever_rounded),
      onPressed: () async {
        MovementLogService.deleteSessionsDialog(_allSessions, (session) {
            _allSessions.remove(session);
            updateSessions(withRefreshSessions: true);
        });
      },
    ),
    IconButton(
      icon: const Icon(Icons.info_rounded),
      onPressed: () {
        showMessageDialog(
          title: '統計情報',
          message: '${DateFormat('yyyy年M月d日').format(_allSessions.entries.first.key.startTime)}の移動ログ\nセッション数: ${_allSessions.length}\n記録された地点数: ${_allSessions.values.fold<int>(0, (sum, logs) => sum + logs.length)}',
          icon: Icons.info_rounded,
        );
      },
    ),
  ];

  @override
  void initialize() async {
    await controller.addLineLayer(
      'voronoi',
      'line',
      masterLineLayerProperties.copyWith(const LineLayerProperties(
        lineWidth: 2.0,
      )),
    );

    updateSessions(withRefreshSessions: true);
  }

  Future<void> updateSessions({bool withRefreshSessions = false}) async {
    if (withRefreshSessions) {
      _allSessions = await MovementLogService.getSessionsWithLogs(parent.widget.sessionIds!);
    }

    if (_allSessions.isEmpty) {
      if (context.mounted) {
        context.pop();
      }
      showMessageDialog(
        title: '移動ログがありません',
        message: '表示できる移動ログがありませんでした',
        icon: Icons.info_rounded,
      );
      return;
    }

    final movementLogs = _allSessions.entries
        .where((entry) => !_ignoredSessionIds.contains(entry.key.id))
        .map((entry) => entry.value)
        .toList();

    final lineStrings = movementLogs.map((logs) => buildLine(logs)).toList();
    await controller.setGeoJsonSource('voronoi', {
      'type': 'FeatureCollection',
      'features': lineStrings,
    });

    _cameraBounds = movementLogs.isEmpty ? null : getBounds(movementLogs.expand((x) => x).map((x) => LatLngPoint(x.latitude, x.longitude)).toList(), margin: true);
    parent.rebuildWidget();
    resetCameraBounds();
  }

  void resetCameraBounds() {
    if (_cameraBounds == null) return;
    controller.moveCamera(CameraUpdate.newLatLngBounds(_cameraBounds!));
  }
}
