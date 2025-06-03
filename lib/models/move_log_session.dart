import '_abstract.dart';

class MoveLogSession extends AbstractModel {
  late String id;
  late DateTime startTime;

  @override
  MoveLogSession fromMap(Map<String, dynamic> map) {
    final session = MoveLogSession();
    session.id = map['id'];
    session.startTime = DateTime.fromMillisecondsSinceEpoch(map['start_time']);
    return session;
  }

  @override
  MoveLogSession fromJson(Map<String, dynamic> json) {
    final session = MoveLogSession();
    session.id = json['id'];
    session.startTime = DateTime.fromMillisecondsSinceEpoch(json['start_time']);
    return session;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'start_time': startTime.millisecondsSinceEpoch,
    };
  }
}
