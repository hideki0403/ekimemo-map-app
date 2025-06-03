import '_abstract.dart';

class MoveLog extends AbstractModel {
  late int id;
  late String sessionId;
  late DateTime timestamp;
  late double latitude;
  late double longitude;
  late double speed;
  late double accuracy;

  @override
  MoveLog fromMap(Map<String, dynamic> map) {
    final moveLog = MoveLog();
    moveLog.id = map['id'];
    moveLog.sessionId = map['session_id'];
    moveLog.timestamp = DateTime.fromMillisecondsSinceEpoch(map['timestamp']);
    moveLog.latitude = map['latitude'];
    moveLog.longitude = map['longitude'];
    moveLog.speed = map['speed'];
    moveLog.accuracy = map['accuracy'];
    return moveLog;
  }

  @override
  MoveLog fromJson(Map<String, dynamic> json) {
    final moveLog = MoveLog();
    moveLog.id = json['id'];
    moveLog.sessionId = json['session_id'];
    moveLog.timestamp = DateTime.fromMillisecondsSinceEpoch(json['timestamp']);
    moveLog.latitude = json['latitude'];
    moveLog.longitude = json['longitude'];
    moveLog.speed = json['speed'];
    moveLog.accuracy = json['accuracy'];
    return moveLog;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      ...toMapWithoutId(),
    };
  }

  Map<String, dynamic> toMapWithoutId() {
    return {
      'session_id': sessionId,
      'timestamp': timestamp.millisecondsSinceEpoch,
      'latitude': latitude,
      'longitude': longitude,
      'speed': speed,
      'accuracy': accuracy,
    };
  }
}