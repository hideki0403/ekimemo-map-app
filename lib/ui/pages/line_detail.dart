import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher_string.dart';
import 'package:bottom_sheet/bottom_sheet.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';
import 'package:ekimemo_map/ui/widgets/station_simple.dart';

final _stationRepository = StationRepository();
final _lineRepository = LineRepository();

class LineDetailView extends StatefulWidget {
  final int? lineId;
  const LineDetailView({this.lineId, super.key});

  @override
  State<StatefulWidget> createState() => _LineDetailViewState();
}

class _LineDetailViewState extends State<LineDetailView> {
  final AccessLogRepository _accessLogRepository = AccessLogRepository();
  Line? line;
  List<Station> stations = [];
  List<int> accessedStation = [];

  @override
  void initState() {
    super.initState();
    _loadLine();
  }

  Future<void> _loadLine() async {
    if (widget.lineId == null) return;

    final x = await _lineRepository.getOne(widget.lineId!);
    if (x == null || !context.mounted) return;
    setState(() {
      line = x;
    });

    final tmpStation = await _stationRepository.get(line!.stationList);
    final accessLog = await _accessLogRepository.get(tmpStation.map((x) => x.id).toList());

    if (!context.mounted) return;
    setState(() {
      stations = tmpStation;
      accessedStation = accessLog.map((x) => x.id).toList();
    });
  }

  void showToolsSheet() async {
    await showFlexibleBottomSheet(
      context: context,
      maxHeight: 0.35,
      initHeight: 0.35,
      anchors: [0, 0.35],
      isSafeArea: true,
      bottomSheetBorderRadius: const BorderRadius.vertical(top: Radius.circular(25.0)),
      builder: (context, scrollController, _) {
        return ListView(
          controller: scrollController,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Theme.of(context).dividerColor,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
            ),
            Text('ツール', textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleLarge),
            ListTile(
              title: const Text('マップで見る'),
              leading: const Icon(Icons.map_rounded),
              onTap: () {
                Navigator.pop(context);
                context.push(Uri(path: '/map', queryParameters: {'line-id': widget.lineId.toString()}).toString());
              },
            ),
            ListTile(
              title: const Text('路線情報 (公式サイト)'),
              leading: const Icon(Icons.train_rounded),
              onTap: () async {
                Navigator.pop(context);
                final url = 'https://ekimemo.com/database/line/${line?.id}';
                if (await canLaunchUrlString(url)) {
                  await launchUrlString(url);
                }
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('路線情報')),
      floatingActionButton: FloatingActionButton(
        onPressed: showToolsSheet,
        child: const Icon(Icons.menu_rounded),
      ),
      body: line == null ? const Center(child: CircularProgressIndicator()) : CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: Container(
              width: double.infinity,
              color: Theme.of(context).colorScheme.surfaceContainer,
              padding: const EdgeInsets.all(16),
              child: Column(
                spacing: 4,
                children: [
                  Text(line!.name, textScaler: const TextScaler.linear(1.5)),
                  Text(line!.nameKana),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    height: 12,
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    decoration: BoxDecoration(
                      color: line?.color != null ? hexToColor(line?.color) : Theme.of(context).colorScheme.surfaceBright,
                      borderRadius: BorderRadius.circular(256),
                    ),
                  ),
                  if (line!.nameFormal != null) ...[
                    const SizedBox(height: 8),
                    Text(line!.nameFormal!),
                  ],
                ],
              ),
            )
          ),
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 12),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                const SectionTitle(title: '路線情報'),
                Padding(
                  padding: const EdgeInsets.only(left: 16, right: 16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('駅数: ${line?.stationSize ?? 0}'),
                      Text('アクセス済み駅数: ${accessedStation.length} (${(accessedStation.length / (line?.stationSize ?? 1) * 100).toStringAsFixed(1)}%)'),
                      const SizedBox(height: 8),
                      LinearProgressIndicator(
                        value: accessedStation.length / (line?.stationSize ?? 1),
                      ),
                    ],
                  ),
                ),
                const SectionTitle(title: '駅情報'),
                ListView.builder(
                  padding: EdgeInsets.zero,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: stations.length,
                  itemBuilder: (context, index) {
                    final station = stations[index];
                    return StationSimple(
                      station: station,
                      isAccessed: accessedStation.contains(station.id),
                      showAttr: true,
                    );
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
