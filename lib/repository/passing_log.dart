import 'package:ekimemo_map/models/passing_log.dart';
import '_abstract.dart';

class PassingLogRepository extends AbstractRepository<PassingLog> {
  static PassingLogRepository? _instance;
  PassingLogRepository._internal() : super(PassingLog(), 'passing_log', 'uuid');

  factory PassingLogRepository() {
    _instance ??= PassingLogRepository._internal();
    return _instance!;
  }
}