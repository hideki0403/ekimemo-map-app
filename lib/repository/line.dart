import 'package:ekimemo_map/models/line.dart';
import '_abstract.dart';

class LineRepository extends AbstractRepository<Line> {
  LineRepository() : super(Line(), 'line', 'code');
}