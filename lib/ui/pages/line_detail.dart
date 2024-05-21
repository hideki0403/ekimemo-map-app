import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';

class LineDetailView extends StatefulWidget {
  final String? lineId;
  const LineDetailView({this.lineId, super.key});

  @override
  State<StatefulWidget> createState() => _LineDetailViewState();
}

class _LineDetailViewState extends State<LineDetailView> {
  final LineRepository _lineRepository = LineRepository();
  final StationRepository _stationRepository = StationRepository();
  Line? line;
  List<Station> stations = [];

  @override
  void initState() {
    super.initState();
    if (widget.lineId == null) return;

    _lineRepository.get(widget.lineId!, column: 'id').then((x) {
      if (x == null) return;
      setState(() {
        line = x;
      });

      Future.wait(line!.stationList.map((x) async {
        final station = await _stationRepository.get(x);
        return station;
      })).then((x) {
        setState(() {
          stations = x.where((x) => x != null).map((x) => x!).toList();
        });
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('路線情報')),
      body: CustomScrollView(
        slivers: [
          SliverList(
            delegate: SliverChildListDelegate(line == null ? [
              const Center(child: CircularProgressIndicator()),
            ] : [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      Text(line!.name, textScaler: const TextScaler.linear(1.5)),
                      Text(line!.nameKana),
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
              // TODO: 駅情報を表示したい
              // ListView.builder(
              //     padding: EdgeInsets.zero,
              //     shrinkWrap: true,
              //     physics: const NeverScrollableScrollPhysics(),
              //     itemCount: lines.length,
              //     itemBuilder: (context, index) {
              //       return LineSimple(line: lines[index]);
              //     }
              // )
            ]),
          ),
        ],
      ),
    );
  }
}