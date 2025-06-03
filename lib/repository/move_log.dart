import 'package:ekimemo_map/models/move_log.dart';
import '_abstract.dart';

class MoveLogRepository extends AbstractRepository<MoveLog> {
  static MoveLogRepository? _instance;
  MoveLogRepository._internal() : super(MoveLog(), 'move_log', 'id');

  factory MoveLogRepository() {
    _instance ??= MoveLogRepository._internal();
    return _instance!;
  }

  Future<List<MoveLog>> getBySessionId(String sessionId) async {
    return await super.get([sessionId], column: 'session_id');
  }

  Future<void> deleteBySessionId(String sessionId) async {
    return await super.delete(sessionId, column: 'session_id');
  }
}