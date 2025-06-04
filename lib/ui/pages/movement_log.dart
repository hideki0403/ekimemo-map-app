import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:expandable/expandable.dart';

import 'package:ekimemo_map/ui/widgets/scrollview_template.dart';
import 'package:ekimemo_map/services/movement_log.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/models/move_log_session.dart';

class MovementLogView extends StatefulWidget {
  const MovementLogView({super.key});

  @override
  State<MovementLogView> createState() => MovementLogViewState();
}

class MovementLogViewState extends State<MovementLogView> {
  final Map<DateTime, List<MoveLogSession>> _sessions = {};
  bool _useCalender = true;

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
        final key = normalizeDate(session.startTime);
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
        actions: [
          IconButton(
            icon: Icon(_useCalender ? Icons.list_rounded : Icons.calendar_month_rounded),
            onPressed: () {
              setState(() {
                _useCalender = !_useCalender;
              });
            },
          ),
        ],
      ),
      body: SafeArea(child: _useCalender ? _Calender(sessions: _sessions) : _ListViewer(sessions: _sessions)),
    );
  }
}

class _ListViewer extends StatelessWidget {
  final Map<DateTime, List<MoveLogSession>> sessions;

  const _ListViewer({required this.sessions});

  @override
  Widget build(BuildContext context) {
    return ScrollViewTemplate(
      delegate: SliverChildBuilderDelegate((BuildContext context, int index) {
        final itemIndex = sessions.length - 1 - index;
        final date = sessions.keys.elementAt(itemIndex);
        return _ListViewerItem(
          date: date,
          sessions: sessions,
        );
      }, childCount: sessions.length),
      empty: const Center(
        child: Text('移動ログがありません'),
      ),
      isEmpty: sessions.isEmpty,
    );
  }

  void deleteSessionDay(DateTime key) {
    sessions.remove(key);
  }
}

class _ListViewerItem extends StatefulWidget {
  final DateTime date;
  final Map<DateTime, List<MoveLogSession>> sessions;

  const _ListViewerItem({required this.date, required this.sessions});

  @override
  State<_ListViewerItem> createState() => _ListViewerItemState();
}

class _ListViewerItemState extends State<_ListViewerItem> {
  final DateFormat _displayDateFormat = DateFormat('yyyy年M月d日 (E)');
  final expandableController = ExpandableController();

  bool isExpanded = false;

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Column(
        children: [
          GestureDetector(
            onTap: () {
              expandableController.toggle();
              setState(() {
                isExpanded = !isExpanded;
              });
            },
            onLongPress: () => _Utils.moveToSessionMap(context, widget.sessions[widget.date]!),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(child: Text(_displayDateFormat.format(widget.date), textScaler: TextScaler.linear(1.2))),
                  Icon(isExpanded ? Icons.expand_less_rounded : Icons.expand_more_rounded),
                ],
              ),
            ),
          ),
          ExpandableNotifier(
            controller: expandableController,
            child: Expandable(
              collapsed: const SizedBox(
                width: double.infinity,
              ),
              expanded: Column(
                children: [
                  const Divider(),
                  _SessionSummary(sessions: widget.sessions[widget.date]!, parentKey: widget.date, deleteSessionDay: (DateTime key) {
                    setState(() {
                      widget.sessions.remove(key);
                    });
                  }),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                    child: FilledButton.icon(
                      label: Text('全ての移動ログを地図で表示'),
                      onPressed: () => _Utils.moveToSessionMap(context, widget.sessions[widget.date]!),
                      icon: const Icon(Icons.map_rounded),
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(double.infinity, 48),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Calender extends StatefulWidget {
  final Map<DateTime, List<MoveLogSession>> sessions;

  const _Calender({required this.sessions});

  @override
  State<_Calender> createState() => _CalenderState();
}

class _CalenderState extends State<_Calender> {
  DateTime _selectedDay = normalizeDate(DateTime.now());
  DateTime _firstDay = DateTime.now();
  DateTime _lastDay = DateTime.now();

  @override
  void initState() {
    super.initState();

    final firstDay = widget.sessions.isEmpty ? DateTime.now() : widget.sessions.keys.first;
    final lastDay = widget.sessions.isEmpty ? DateTime.now() : widget.sessions.keys.last;

    setState(() {
      _firstDay = DateTime(firstDay.year, firstDay.month, 1);
      _lastDay = DateTime(lastDay.year, lastDay.month + 1, 0);
    });

    if (widget.sessions.isNotEmpty) {
      setState(() {
        _selectedDay = widget.sessions.keys.last;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TableCalendar(
          locale: 'ja_JP',
          firstDay: _firstDay,
          lastDay: _lastDay,
          focusedDay: _selectedDay,
          selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
          enabledDayPredicate: (day) => widget.sessions.containsKey(normalizeDate(day)),
          onDaySelected: (selectedDay, focusedDay) {
            setState(() {
              _selectedDay = selectedDay;
            });
          },
          headerStyle: HeaderStyle(
            formatButtonVisible: false,
            titleCentered: true,
          ),
          calendarStyle: CalendarStyle(
            outsideDaysVisible: false,
            todayTextStyle: TextStyle(
              color: Theme.of(context).colorScheme.primary,
            ),
            todayDecoration: BoxDecoration(
              border: null,
              shape: BoxShape.circle,
            ),
            selectedTextStyle: TextStyle(
              color: Theme.of(context).colorScheme.onPrimary,
            ),
            selectedDecoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary,
              shape: BoxShape.circle,
            ),
            disabledTextStyle: TextStyle(
              color: Theme.of(context).textTheme.bodyMedium?.color?.withValues(alpha: 0.35) ?? Colors.grey,
            ),
            defaultTextStyle: TextStyle(
              color: Theme.of(context).textTheme.bodyMedium?.color ?? Colors.black,
            ),
            weekendTextStyle: TextStyle(
              color: Theme.of(context).textTheme.bodyMedium?.color ?? Colors.black,
            ),
          ),
          daysOfWeekHeight: 32,
          calendarBuilders: CalendarBuilders(
            dowBuilder: (context, day) {
              final text = DateFormat.E().format(day);
              Color textColor;

              if (day.weekday == DateTime.sunday) {
                textColor = isDarkMode(context) ? Colors.red.shade300 : Colors.red.shade400;
              } else if (day.weekday == DateTime.saturday) {
                textColor = isDarkMode(context) ? Colors.blue.shade300 : Colors.blue.shade400;
              } else {
                textColor = Theme.of(context).textTheme.bodyMedium?.color ?? Colors.black;
              }

              return Center(
                child: Text(
                  text,
                  style: TextStyle(color: textColor),
                ),
              );
            },
          ),
        ),
        const Divider(),
        if (widget.sessions.containsKey(_selectedDay)) ...[
          const SizedBox(height: 8),
          Expanded(
            child: _SessionSummary(sessions: widget.sessions[_selectedDay]!, parentKey: _selectedDay, deleteSessionDay: deleteSessionDay),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: FilledButton.icon(
              label: Text('全ての移動ログを地図で表示'),
              onPressed: () => _Utils.moveToSessionMap(context, widget.sessions[_selectedDay]!),
              icon: const Icon(Icons.map_rounded),
              style: FilledButton.styleFrom(
                minimumSize: const Size(double.infinity, 48),
              ),
            ),
          ),
          const SizedBox(height: 16),
        ] else
          Expanded(
            child: Center(
              child: Text(
                '${DateFormat('yyyy/M/d').format(_selectedDay)} の移動ログはありません',
              ),
            ),
          ),
      ],
    );
  }

  void deleteSessionDay(DateTime key) {
    setState(() {
      widget.sessions.remove(key);
    });
  }
}

class _SessionSummary extends StatefulWidget {
  final Function(DateTime key) deleteSessionDay;
  final DateTime parentKey;
  final List<MoveLogSession> sessions;

  const _SessionSummary({required this.sessions, required this.parentKey, required this.deleteSessionDay});

  @override
  State<_SessionSummary> createState() => _SessionSummaryState();
}

class _SessionSummaryState extends State<_SessionSummary> {
  final DateFormat _dateFormat = DateFormat('HH:mm:ss');

  MoveLogSessionMap? _sessionLogs;

  @override
  void initState() {
    super.initState();
    _loadSessionLogs();
  }

  @override
  void didUpdateWidget(covariant _SessionSummary oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.sessions != widget.sessions) {
      _loadSessionLogs();
    }
  }

  Future<void> _loadSessionLogs() async {
    final logs = await MovementLogService.getLogsFromSessions(widget.sessions);
    if (!context.mounted) return;
    setState(() {
      _sessionLogs = logs;
    });
  }

  @override
  Widget build(BuildContext context) {
    return _sessionLogs == null
      ? const Center(child: CircularProgressIndicator())
      : Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  Expanded(child: Text('ログ一覧', style: Theme.of(context).textTheme.titleLarge)),
                  IconButton(
                    icon: const Icon(Icons.delete_forever_rounded),
                    color: Theme.of(context).colorScheme.error,
                    onPressed: () {
                      MovementLogService.deleteSessionsDialog(_sessionLogs!, (MoveLogSession session) {
                        setState(() {
                          _sessionLogs!.remove(session);
                          if (_sessionLogs!.isEmpty) {
                            widget.deleteSessionDay(widget.parentKey);
                          }
                        });
                      });
                    },
                  ),
                ],
              ),
            ),
            SingleChildScrollView(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Column(
                spacing: 16,
                children: [
                  for (final session in _sessionLogs!.entries) ...[
                    ListTile(
                      title: Text('${_dateFormat.format(session.value.firstOrNull?.timestamp ?? DateTime.now())} 〜 ${_dateFormat.format(session.value.lastOrNull?.timestamp ?? DateTime.now())}'),
                      subtitle: Text('${session.value.length}件の移動ログ'),
                      trailing: Icon(Icons.route_rounded),
                      onTap: () => _Utils.moveToSessionMap(context, [session.key]),
                    )
                  ]
                ],
              ),
            ),
          ],
        );
  }
}

class _Utils {
  static void moveToSessionMap(BuildContext context, List<MoveLogSession> sessionIds) {
    final sessionList = sessionIds.map((e) => e.id).toList();
    context.push(Uri(path: '/map', queryParameters: {'session-ids': sessionList.join(',')}).toString());
  }
}
