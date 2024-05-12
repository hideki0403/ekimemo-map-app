import '_abstract.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/repository/station.dart';


class LineRepository extends AbstractRepository<Line> {
  LineRepository() : super(Line(), 'line', 'code');
}