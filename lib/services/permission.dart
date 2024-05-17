import 'package:permission_handler/permission_handler.dart';

enum LocationPermissionStatus { granted, denied, permanentlyDenied, restricted }

class LocationPermissionsHandler {
  static Future<bool> get isGranted async {
    final status = await Permission.location.status;
    switch (status) {
      case PermissionStatus.granted:
      case PermissionStatus.limited:
        return true;
      case PermissionStatus.denied:
      case PermissionStatus.permanentlyDenied:
      case PermissionStatus.restricted:
        return false;
      default:
        return false;
    }
  }

  static Future<bool> get isAlwaysGranted {
    return Permission.locationAlways.isGranted;
  }

  static Future<LocationPermissionStatus> request() async {
    final status = await Permission.location.request();
    switch (status) {
      case PermissionStatus.granted:
        return LocationPermissionStatus.granted;
      case PermissionStatus.denied:
        return LocationPermissionStatus.denied;
      case PermissionStatus.limited:
      case PermissionStatus.permanentlyDenied:
        return LocationPermissionStatus.permanentlyDenied;
      case PermissionStatus.restricted:
        return LocationPermissionStatus.restricted;
      default:
        return LocationPermissionStatus.denied;
    }
  }

  static Future<bool> checkAndRequest() async {
    final isGranted = await LocationPermissionsHandler.isGranted;
    if (isGranted) return true;
    final status = await request();
    return status == LocationPermissionStatus.granted;
  }
}

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