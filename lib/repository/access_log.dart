import 'package:ekimemo_map/models/access_log.dart';
import '_abstract.dart';

class AccessLogRepository extends AbstractRepository<AccessLog> {
  AccessLogRepository() : super(AccessLog(), 'access_log', 'id');
}