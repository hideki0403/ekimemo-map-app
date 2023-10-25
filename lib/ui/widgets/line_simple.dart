import 'package:flutter/material.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/models/line.dart';

class LineSimple extends StatelessWidget {
  final Line line;
  const LineSimple({required this.line, Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          // TODO: 路線情報ページに飛べるようにする
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Expanded(
                child: Text(line.name, textScaleFactor: 1.2),
              ),
              _AccessProgress(stationList: line.uniqueStationList),
            ],
          ),
        ),
      ),
    );
  }
}

class _AccessProgress extends StatefulWidget {
  final List<String> stationList;
  const _AccessProgress({required this.stationList, Key? key}) : super(key: key);

  @override
  _AccessProgressState createState() => _AccessProgressState();
}

class _AccessProgressState extends State<_AccessProgress> {
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
    final stations = <String>[];
    await Future.wait(widget.stationList.map((stationId) async {
      final x = await AccessLogRepository().get(stationId);
      if (x == null) return;
      stations.add(x.id);
    }));
    setState(() {
      accessedStation = stations;
      isComplete = stations.length == widget.stationList.length;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Text('${accessedStation.length}/${widget.stationList.length} (${(accessedStation.length / widget.stationList.length * 100).toStringAsFixed(1)}%)', style: TextStyle(color: isComplete ? Theme.of(context).colorScheme.primary : null));
  }
}