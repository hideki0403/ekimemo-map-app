import 'package:ekimemo_map/models/access_log.dart';
import '_abstract.dart';

class AccessLogRepository extends AbstractRepository<AccessLog> {
  static AccessLogRepository? _instance;
  AccessLogRepository._internal() : super(AccessLog(), 'access_log', 'id');

  factory AccessLogRepository() {
    _instance ??= AccessLogRepository._internal();
    return _instance!;
  }
}