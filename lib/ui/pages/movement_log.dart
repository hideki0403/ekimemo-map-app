import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:ekimemo_map/ui/widgets/scrollview_template.dart';
import 'package:ekimemo_map/services/movement_log.dart';
import 'package:ekimemo_map/models/move_log_session.dart';

class MovementLogView extends StatefulWidget {
  const MovementLogView({super.key});

  @override
  State<MovementLogView> createState() => MovementLogViewState();
}

class MovementLogViewState extends State<MovementLogView> {
  final DateFormat _internalDateFormat = DateFormat('yyyy-MM-dd');
  final DateFormat _displayDateFormat = DateFormat('yyyy年M月d日');

  final Map<String, List<MoveLogSession>> _sessions = {};

  @override
  void initState() {
    super.initState();
    _loadMovementLog();
  }

  Future<void> _loadMovementLog() async {
    final sessions = await MovementLogService.getSessions();
    if (!context.mounted) return;

    setState(() {
      _sessions.clear();
      for (final session in sessions) {
        final key = _internalDateFormat.format(session.startTime);
        if (!_sessions.containsKey(key)) {
          _sessions[key] = [];
        }
        _sessions[key]!.add(session);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('移動ログ'),
      ),
      body: ScrollViewTemplate(
          delegate: SliverChildBuilderDelegate((BuildContext context, int index) {
            final itemIndex = _sessions.length - 1 - index;
            final border = RoundedRectangleBorder(borderRadius: BorderRadius.circular(16));
            final date = DateTime.parse(_sessions.keys.elementAt(itemIndex));
            return Card(
              shape: border,
              child: InkWell(
                customBorder: border,
                onTap: () {
                  final sessions = _sessions.values.elementAt(itemIndex).map((e) => e.id).toList();
                  context.push(Uri(path: '/map', queryParameters: {'session-ids': sessions.join(',')}).toString());
                },
                onLongPress: () {
                  // TODO: 一括削除
                },
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Expanded(child: Text(_displayDateFormat.format(date), textScaler: TextScaler.linear(1.2))),
                      const SizedBox(height: 8),
                      Text('${_sessions.values.elementAt(itemIndex).length}件の移動ログ'),
                    ],
                  ),
                ),
              ),
            );
          }, childCount: _sessions.length),
          empty: const Center(
            child: Text('移動ログがありません'),
          ),
          isEmpty: _sessions.isEmpty,
        ),
    );
  }
}
