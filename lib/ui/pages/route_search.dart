import 'dart:async';

import 'package:flutter/material.dart';
import 'package:gpx/gpx.dart';
import 'package:file_picker/file_picker.dart';

import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/ui/widgets/station_card.dart';

class RouteSearchView extends StatefulWidget {
  const RouteSearchView({super.key});

  @override
  State<RouteSearchView> createState() => RouteSearchViewState();
}

class RouteSearchViewState extends State<RouteSearchView> {
  bool isCalculating = false;
  String fileName = '';
  List<Wpt> trackPoints = [];
  Map<String, StationData> stations = {};
  int calcTime = 0;

  Future<Map<String, StationData>> calculateRoute() async {
    final s = <String, StationData>{};
    for (var i = 0; i < trackPoints.length; i++) {
      final pt = trackPoints[i];
      if (pt.lat != null && pt.lon != null) {
        final data = await StationSearchService.getNearestStation(pt.lat!, pt.lon!, withLineData: true);
        if (!s.containsKey(data.station.id)) {
          s[data.station.id] = data;
        }
      }
    }
    return s;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ルート探索'),
      ),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 64),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                const Text('ルート探索に使用するGPXファイルを選択してください。'),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: isCalculating ? null : () async {
                    final result = await FilePicker.platform.pickFiles(
                      type: FileType.any,
                      withData: true,
                    );
                    if (result != null) {
                      final file = result.files.single;
                      if (file.extension != 'gpx') return;
                      if (file.bytes == null) return;
                      final gpx = GpxReader().fromString(String.fromCharCodes(file.bytes!));

                      final pts = <Wpt>[];
                      for (var trk in gpx.trks) {
                        for (var seg in trk.trksegs) {
                          pts.addAll(seg.trkpts);
                        }
                      }

                      setState(() {
                        fileName = file.name;
                        trackPoints = pts;
                      });
                    }
                  },
                  child: const Text('GPXファイルを選択'),
                ),
                const Divider(),
                Text('ファイル名: $fileName'),
                Text('探索に使用する地点数: ${trackPoints.length}'),
                const SizedBox(height: 8),
                ElevatedButton(
                  onPressed: trackPoints.isEmpty || isCalculating ? null : () async {
                    setState(() {
                      isCalculating = true;
                    });

                    final stopwatch = Stopwatch()..start();
                    final result = await calculateRoute();
                    stopwatch.stop();

                    setState(() {
                      isCalculating = false;
                      calcTime = stopwatch.elapsedMilliseconds;
                      stations = result;
                    });
                  },
                  child: const Text('探索実行'),
                ),
                const Divider(),
                Text('探索結果: ${stations.length}駅 (${calcTime}ms)'),
                const SizedBox(height: 8),
                ListView.builder(
                  padding: EdgeInsets.zero,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: stations.length,
                  itemBuilder: (context, index) {
                    return StationCard(stationData: stations.values.elementAt(index), index: index, viewOnly: true);
                  }
                ),
              ]),
            ),
          ),
        ],
      ),
    );
  }
}
