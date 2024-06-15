import 'package:ekimemo_map/models/passing_log.dart';
import '_abstract.dart';

class PassingLogRepository extends AbstractRepository<PassingLog> {
  PassingLogRepository() : super(PassingLog(), 'passing_log', 'uuid');
}