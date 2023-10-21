import '_abstract.dart';

class AccessLog extends AbstractModel {
  late String id;
  late DateTime firstAccess;
  late DateTime lastAccess;
  late int accessCount;

  @override
  AccessLog fromMap(Map<String, dynamic> map) {
    final accessLog = AccessLog();
    accessLog.id = map['id'];
    accessLog.firstAccess = DateTime.parse(map['first_access']);
    accessLog.lastAccess = DateTime.parse(map['last_access']);
    accessLog.accessCount = map['access_count'];
    return accessLog;
  }

  @override
  AccessLog fromJson(Map<String, dynamic> json) {
    final accessLog = AccessLog();
    accessLog.id = json['id'];
    accessLog.firstAccess = DateTime.parse(json['first_access']);
    accessLog.lastAccess = DateTime.parse(json['last_access']);
    accessLog.accessCount = json['access_count'];
    return accessLog;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'first_access': firstAccess.toIso8601String(),
      'last_access': lastAccess.toIso8601String(),
      'access_count': accessCount,
    };
  }
}