import 'package:flutter/material.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:go_router/go_router.dart';

class LineSimple extends StatelessWidget {
  final Line line;
  final bool disableStats;
  const LineSimple({required this.line, this.disableStats = false, super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/line', queryParameters: {'id': line.code.toString()}).toString());
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Container(
                width: 8,
                height: 30,
                decoration: BoxDecoration(
                  color: hexToColor(line.color),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.2), width: 1),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(line.name, textScaler: const TextScaler.linear(1.2)),
              ),
              if (!disableStats) _AccessProgress(stationList: line.stationList),
            ],
          ),
        ),
      ),
    );
  }
}

class _AccessProgress extends StatefulWidget {
  final List<String> stationList;
  const _AccessProgress({required this.stationList});

  @override
  _AccessProgressState createState() => _AccessProgressState();
}

class _AccessProgressState extends State<_AccessProgress> {
  final AccessLogRepository _accessLogRepository = AccessLogRepository();
  List<String> accessedStation = [];
  bool isComplete = false;

  @override
  void initState() {
    super.initState();
    rebuild();
  }

  @override
  void didUpdateWidget(covariant _AccessProgress oldWidget) {
    super.didUpdateWidget(oldWidget);
    rebuild();
  }

  Future<void> rebuild() async {
    final stations = await _accessLogRepository.get(widget.stationList);
    if (!context.mounted) return;
    setState(() {
      accessedStation = stations.map((x) => x.id).toList();
      isComplete = stations.length == widget.stationList.length;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Text('${accessedStation.length}/${widget.stationList.length} (${(accessedStation.length / widget.stationList.length * 100).toStringAsFixed(1)}%)', style: TextStyle(color: isComplete ? Theme.of(context).colorScheme.primary : null));
  }
}