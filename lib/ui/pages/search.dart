import 'package:flutter/material.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/ui/widgets/station_simple.dart';
import 'package:ekimemo_map/ui/widgets/line_simple.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';

final _stationRepository = StationRepository();
final _lineRepository = LineRepository();

class SearchView extends StatefulWidget {
  const SearchView({super.key});

  @override
  State<SearchView> createState() => _SearchViewState();
}

class _SearchViewState extends State<SearchView> {
  final controller = TextEditingController();
  String resultText = '';
  List<Station> hitStations = [];
  List<Line> hitLines = [];

  void _search([String query = '']) async {
    final stopWatch = Stopwatch()..start();
    if (query.isEmpty) {
      setState(() {
        hitStations.clear();
        hitLines.clear();
        resultText = '';
      });
      return;
    }

    final stations = await _stationRepository.search(query);
    final lines = await _lineRepository.search(query);
    List<String> texts = [];
    if (stations.isNotEmpty) texts.add('${stations.length}駅');
    if (lines.isNotEmpty) texts.add('${lines.length}路線');

    if (!context.mounted) return;
    setState(() {
      hitStations = stations;
      hitLines = lines;
      resultText = '${texts.isEmpty ? '該当する駅または路線はありませんでした' : '${texts.join('、')}が見つかりました'} (${stopWatch.elapsedMilliseconds}ms)';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('検索'),
      ),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 64),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                TextField(
                  controller: controller,
                  onChanged: _search,
                  decoration: InputDecoration(
                    hintText: '駅名または路線名を入力してください',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: IconButton(
                      icon: const Icon(Icons.clear),
                      onPressed: () {
                        controller.clear();
                        _search();
                      },
                    ),
                    fillColor: Theme.of(context).colorScheme.surfaceContainer,
                    filled: true,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(32),
                      borderSide: BorderSide.none,
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Text(resultText),
                ),
                if (hitStations.isNotEmpty) ...[
                  const SectionTitle(title: '駅'),
                  ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: hitStations.length,
                    itemBuilder: (context, index) {
                      final station = hitStations[index];
                      return StationSimple(station: station, showAttr: true);
                    },
                  ),
                ],
                if (hitLines.isNotEmpty) ...[
                  const SectionTitle(title: '路線'),
                  ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: hitLines.length,
                    itemBuilder: (context, index) {
                      final line = hitLines[index];
                      return LineSimple(line: line, disableStats: true);
                    },
                  ),
                ],
              ]),
            ),
          ),
        ]
      ),
    );
  }
}
