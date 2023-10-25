import '_abstract.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/repository/station.dart';


class LineRepository extends AbstractRepository<Line> {
  LineRepository() : super(Line(), 'line', 'code');

  Future<void> rebuildUniqueStationList() async {
    final lines = await getAll();
    final stations = await StationRepository().getAllMap<int>();
    await Future.wait(lines.map((line) async {
      final uniqueStationList = <String>[];
      for (var station in line.stationList) {
        final stationData = stations[station['code']];
        if (stationData == null) continue;
        uniqueStationList.add(stationData['id']);
      }
      line.uniqueStationList = uniqueStationList;
      await update(line);
    }));
  }
}