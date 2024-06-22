import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/cache.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';
import 'package:ekimemo_map/ui/widgets/station_simple.dart';

class LineDetailView extends StatefulWidget {
  final String? lineId;
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

    final x = await LineCache.get(int.parse(widget.lineId!));
    if (x == null || !context.mounted) return;
    setState(() {
      line = x;
    });

    final List<Station> tmpStation = [];
    final List<int> tmpAccessed = [];

    await Future.wait(line!.stationList.map((x) async {
      final station = await StationCache.get(x);
      final accessLog = await _accessLogRepository.get(station?.id);
      if (accessLog != null) tmpAccessed.add(x);
      if (station != null) tmpStation.add(station);
    }));

    if (!context.mounted) return;
    setState(() {
      stations = tmpStation;
      accessedStation = tmpAccessed;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('路線情報')),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, bottom: 12),
            sliver: SliverList(
              delegate: SliverChildListDelegate(line == null ? [
                const Center(child: CircularProgressIndicator()),
              ] : [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        Text(line!.name, textScaler: const TextScaler.linear(1.5)),
                        Text(line!.nameKana),
                        const SizedBox(height: 16),
                        CustomPaint(
                          painter: _TrianglePainter(color: line?.color != null ? hexToColor(line?.color) : Theme.of(context).colorScheme.onSurface),
                          child: const SizedBox(
                            width: double.infinity,
                            height: 12,
                          ),
                        ),
                        if (line!.nameFormal != null) ...[
                          const SizedBox(height: 16),
                          Text(line!.nameFormal!),
                        ],
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.center,
                  child: ElevatedButton(
                    onPressed: line?.polylineList != null ? () {
                      context.push(Uri(path: '/map', queryParameters: {'line-id': widget.lineId}).toString());
                    } : null,
                    child: const Text('マップで見る'),
                  ),
                ),
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
                        isAccessed: accessedStation.contains(station.code),
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

class _TrianglePainter extends CustomPainter {
  Color color;
  _TrianglePainter({required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    final path = Path()
      ..moveTo(0, size.height / 2)
      ..lineTo(10, 0)
      ..lineTo(size.width, 0)
      ..lineTo(size.width, size.height)
      ..lineTo(10, size.height)
      ..close();

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return false;
  }
}