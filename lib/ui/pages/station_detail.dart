import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher_string.dart';
import 'package:bottom_sheet/bottom_sheet.dart';

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
                context.push(Uri(path: '/map', queryParameters: {'station-id': widget.stationId.toString()}).toString());
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text('レーダー範囲'),
              leading: const Icon(Icons.radar_rounded),
              onTap: () {
                context.push(Uri(path: '/map', queryParameters: {'radar-id': widget.stationId.toString()}).toString());
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text('駅のにぎわい (公式サイト)'),
              leading: const Icon(Icons.train_rounded),
              onTap: () async {
                Navigator.pop(context);
                final url = 'https://ekimemo.com/database/station/${station?.id}/activity';
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
      appBar: AppBar(title: const Text('駅情報')),
      floatingActionButton: FloatingActionButton(
        onPressed: showToolsSheet,
        child: const Icon(Icons.menu_rounded),
      ),
      body: station == null ? const Center(child: CircularProgressIndicator()) : CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: Container(
              width: double.infinity,
              color: Theme.of(context).colorScheme.surfaceContainer,
              padding: const EdgeInsets.all(16),
              child: Column(
                spacing: 4,
                children: [
                  Text(prefectureCode[station!.prefecture]!),
                  getAttrIcon(station!.attr, context: context),
                  Text(station!.originalName, textScaler: const TextScaler.linear(1.5)),
                  Text(station!.nameKana),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    decoration: BoxDecoration(
                      color: accessLog?.accessed != true ? Theme.of(context).colorScheme.surfaceBright : Theme.of(context).colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(256),
                    ),
                    child: Text(accessLog?.accessed != true ? '未アクセス' : 'アクセス済み', textAlign: TextAlign.center, style: TextStyle(
                      color: accessLog?.accessed != true ? Theme.of(context).colorScheme.onSurface : Theme.of(context).colorScheme.onPrimaryContainer,
                    )),

                  ),
                ],
              ),
            )
          ),
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 12),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                if (kDebugMode) ...[
                  const SectionTitle(title: 'Station Info (Debug)'),
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