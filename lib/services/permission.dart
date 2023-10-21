import 'package:permission_handler/permission_handler.dart';

enum LocationPermissionStatus { granted, denied, permanentlyDenied, restricted }

class LocationPermissionsHandler {
  Future<bool> get isGranted async {
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

  Future<bool> get isAlwaysGranted {
    return Permission.locationAlways.isGranted;
  }

  Future<LocationPermissionStatus> request() async {
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

  Future<bool> checkAndRequest() async {
    final isGranted = await this.isGranted;
    if (isGranted) return true;
    final status = await request();
    return status == LocationPermissionStatus.granted;
  }
}

enum NotificationPermissionStatus { granted, denied, unknown }

class NotificationPermissionsHandler {
  Future<bool> get isGranted async {
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

  Future<NotificationPermissionStatus> request() async {
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

  Future<bool> checkAndRequest() async {
    final isGranted = await this.isGranted;
    if (isGranted) return true;
    final status = await request();
    return status == NotificationPermissionStatus.granted;
  }
}