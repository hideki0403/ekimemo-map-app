import 'package:permission_handler/permission_handler.dart';

enum NotificationPermissionStatus { granted, denied, unknown }

class NotificationPermissionsHandler {
  static Future<bool> get isGranted async {
    final status = await Permission.notification.status;
    switch (status) {
      case PermissionStatus.granted:
        return true;
      case PermissionStatus.denied:
      case PermissionStatus.permanentlyDenied:
      case PermissionStatus.restricted:
        return false;
      default:
        return false;
    }
  }

  static Future<NotificationPermissionStatus> request() async {
    final status = await Permission.notification.request();
    switch (status) {
      case PermissionStatus.granted:
        return NotificationPermissionStatus.granted;
      case PermissionStatus.denied:
        return NotificationPermissionStatus.denied;
      case PermissionStatus.permanentlyDenied:
        return NotificationPermissionStatus.denied;
      case PermissionStatus.restricted:
        return NotificationPermissionStatus.denied;
      default:
        return NotificationPermissionStatus.denied;
    }
  }

  static Future<bool> checkAndRequest() async {
    final isGranted = await NotificationPermissionsHandler.isGranted;
    if (isGranted) return true;
    final status = await request();
    return status == NotificationPermissionStatus.granted;
  }
}