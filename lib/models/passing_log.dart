import '_abstract.dart';

class PassingLog extends AbstractModel {
  late String uuid;
  late int id;
  late DateTime timestamp;
  late double latitude;
  late double longitude;
  late double speed;
  late double accuracy;
  late int distance;
  late bool isReNotify;

  @override
  PassingLog fromMap(Map<String, dynamic> map) {
    final accessLog = PassingLog();
    accessLog.uuid = map['uuid'];
    accessLog.id = map['id'];
    accessLog.timestamp = DateTime.parse(map['timestamp']);
    accessLog.latitude = map['latitude'];
    accessLog.longitude = map['longitude'];
    accessLog.speed = map['speed'];
    accessLog.accuracy = map['accuracy'];
    accessLog.distance = map['distance'];
    accessLog.isReNotify = map['isReNotify'] == 1; // 1: true, 0: false
    return accessLog;
  }

  @override
  PassingLog fromJson(Map<String, dynamic> json) {
    final accessLog = PassingLog();
    accessLog.uuid = json['uuid'];
    accessLog.id = json['id'];
    accessLog.timestamp = DateTime.parse(json['timestamp']);
    accessLog.latitude = json['latitude'];
    accessLog.longitude = json['longitude'];
    accessLog.speed = json['speed'];
    accessLog.accuracy = json['accuracy'];
    accessLog.distance = json['distance'];
    accessLog.isReNotify = json['isReNotify'];
    return accessLog;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'uuid': uuid,
      'id': id,
      'timestamp': timestamp.toIso8601String(),
      'latitude': latitude,
      'longitude': longitude,
      'speed': speed,
      'accuracy': accuracy,
      'distance': distance,
      'isReNotify': isReNotify ? 1 : 0, // 1: true, 0: false
    };
  }
}