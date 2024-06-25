import '_abstract.dart';
import 'package:ekimemo_map/models/line.dart';

class LineRepository extends AbstractRepository<Line> {
  static LineRepository? _instance;
  LineRepository._internal() : super(Line(), 'line', 'code');

  factory LineRepository() {
    _instance ??= LineRepository._internal();
    return _instance!;
  }
}