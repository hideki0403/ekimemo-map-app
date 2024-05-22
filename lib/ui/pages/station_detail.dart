import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/services/const.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/ui/widgets/line_simple.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';

// TODO
class StationDetailView extends StatefulWidget {
  final String? stationId;
  const StationDetailView({this.stationId, super.key});

  @override
  State<StatefulWidget> createState() => _StationDetailViewState();
}

class _StationDetailViewState extends State<StationDetailView> {
  final LineRepository _lineRepository = LineRepository();
  Station? station;
  List<Line> lines = [];
  AccessLog? accessLog;

  @override
  void initState() {
    super.initState();
    if (widget.stationId == null) return;

    StationRepository().get(widget.stationId!, column: 'id').then((x) {
      if (x == null) return;
      setState(() {
        station = x;
      });

      List<Line> tmp = [];
      station?.lines.forEach((lineCode) async {
        await _lineRepository.get(lineCode).then((x) {
          if (x == null) return;
          tmp.add(x);
        }).then((_) => {
          setState(() {
            lines = tmp;
          })
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
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12),
            sliver: SliverList(
              delegate: SliverChildListDelegate(station == null ? [
                const Center(child: CircularProgressIndicator()),
              ] : [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        Text(prefectureCode[station!.prefecture]!),
                        const SizedBox(height: 4),
                        getAttrIcon(station!.attr),
                        Text(station!.originalName, textScaler: const TextScaler.linear(1.5)),
                        Text(station!.nameKana),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.center,
                  child: ElevatedButton(
                    onPressed: () {
                      context.push(Uri(path: '/map', queryParameters: {'station-id': widget.stationId}).toString());
                    },
                    child: const Text('マップで見る'),
                  ),
                ),
                const SectionTitle(title: 'アクセス情報'),
                Padding(
                  padding: const EdgeInsets.only(left: 16, right: 16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('アクセス回数: ${accessLog?.accessCount ?? 0}回'),
                      Text('最終アクセス: ${accessLog?.lastAccess != null ? DateFormat('yyyy/MM/dd HH:mm:ss').format(accessLog!.lastAccess) : '未アクセス'}'),
                      Text('初回アクセス: ${accessLog?.firstAccess != null ? DateFormat('yyyy/MM/dd HH:mm:ss').format(accessLog!.firstAccess) : '未アクセス'}'),
                    ],
                  ),
                ),
                const SectionTitle(title: '路線情報'),
                ListView.builder(
                    padding: EdgeInsets.zero,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: lines.length,
                    itemBuilder: (context, index) {
                      return LineSimple(line: lines[index]);
                    }
                )
              ]),
            ),
          ),
        ],
      ),
    );
  }
}