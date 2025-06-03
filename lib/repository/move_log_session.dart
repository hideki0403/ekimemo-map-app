import 'package:ekimemo_map/models/move_log_session.dart';
import '_abstract.dart';

class MoveLogSessionRepository extends AbstractRepository<MoveLogSession> {
  static MoveLogSessionRepository? _instance;
  MoveLogSessionRepository._internal() : super(MoveLogSession(), 'move_log_session', 'id');

  factory MoveLogSessionRepository() {
    _instance ??= MoveLogSessionRepository._internal();
    return _instance!;
  }
}