import 'package:flutter/material.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/access_log.dart';

// TODO
class StationDetailView extends StatefulWidget {
  final String? stationId;
  const StationDetailView({this.stationId, Key? key}) : super(key: key);

  @override
  _StationDetailViewState createState() => _StationDetailViewState();
}

class _StationDetailViewState extends State<StationDetailView> {
  Station? station;
  List<Line> lines = [];
  AccessLog? accessLog;

  @override
  void initState() {
    super.initState();
    if (widget.stationId == null) return;

    StationRepository().get(widget.stationId!).then((x) {
      if (x == null) return;
      setState(() {
        station = x;
      });

      station?.lines.forEach((lineCode) async {
        await LineRepository().get(lineCode).then((x) {
          if (x == null) return;
          setState(() {
            lines.add(x);
          });
        });
      });

      AccessLogRepository().get(widget.stationId!).then((x) {
        setState(() {
          accessLog = x;
        });
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('駅情報')),
      body: CustomScrollView(
        slivers: [
          SliverList(
            delegate: SliverChildListDelegate(station == null ? [
              const Center(child: CircularProgressIndicator()),
            ] : [
              Text(station!.name),
              Column(
                children: lines.map((line) => Text(line.name)).toList(),
              ),
            ]),
          ),
        ],
      ),
    );
  }
}