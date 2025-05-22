import 'package:flutter/foundation.dart';
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

final _stationRepository = StationRepository();
final _lineRepository = LineRepository();

class StationDetailView extends StatefulWidget {
  final int? stationId;
  const StationDetailView({this.stationId, super.key});

  @override
  State<StatefulWidget> createState() => _StationDetailViewState();
}

class _StationDetailViewState extends State<StationDetailView> {
  Station? station;
  List<Line> lines = [];
  AccessLog? accessLog;

  @override
  void initState() {
    super.initState();
    _loadStation();
  }

  Future<void> _loadStation() async {
    if (widget.stationId == null) return;
    final x = await _stationRepository.getOne(widget.stationId!);
    if (x == null || !context.mounted) return;
    setState(() {
      station = x;
    });

    final tmp = await _lineRepository.get(x.lines);
    if (!context.mounted) return;
    setState(() {
      lines = tmp;
    });

    final z = await AccessLogRepository().getOne(x.id);
    if (!context.mounted) return;
    setState(() {
      accessLog = z;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('駅情報')),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, bottom: 12),
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
                        getAttrIcon(station!.attr, context: context),
                        Text(station!.originalName, textScaler: const TextScaler.linear(1.5)),
                        Text(station!.nameKana),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Wrap(
                  direction: Axis.horizontal,
                  alignment: WrapAlignment.center,
                  spacing: 8,
                  children: [
                    ElevatedButton.icon(
                      onPressed: () {
                        context.push(Uri(path: '/map', queryParameters: {'station-id': widget.stationId.toString()}).toString());
                      },
                      icon: const Icon(Icons.map_rounded),
                      label: const Text('マップ'),
                    ),
                    ElevatedButton.icon(
                      onPressed: () {
                        context.push(Uri(path: '/map', queryParameters: {'radar-id': widget.stationId.toString()}).toString());
                      },
                      icon: const Icon(Icons.radar_rounded),
                      label: const Text('レーダー範囲'),
                    ),
                  ]
                ),
                if (kDebugMode) ...[
                  const SectionTitle(title: 'Debug'),
                  Padding(
                    padding: const EdgeInsets.only(left: 16, right: 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('ID: ${station?.id}'),
                        Text('Code: ${station?.code}'),
                        Text('Latitude: ${station?.lat}'),
                        Text('Longitude: ${station?.lng}'),
                      ],
                    ),
                  ),
                ],
                const SectionTitle(title: 'アクセス情報'),
                Padding(
                  padding: const EdgeInsets.only(left: 16, right: 16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(accessLog?.accessed != true ? '未アクセス' : 'アクセス済み'),
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