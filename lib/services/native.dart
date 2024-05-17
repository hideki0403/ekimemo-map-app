import 'package:flutter/services.dart';

class NativeMethods {
  static const _channel = MethodChannel('dev.yukineko.ekimemo_map/native');

  static Future<String> getCommitHash() async {
    return await _channel.invokeMethod('getCommitHash');
  }

  static Future<bool> hasPermission() async {
    return await _channel.invokeMethod('hasPermission');
  }

  static Future<void> setDebugPackageName(String packageName) async {
    return await _channel.invokeMethod('setDebugPackageName', {'packageName': packageName});
  }

  static Future<void> performTap(double x, double y) async {
    return await _channel.invokeMethod('performTap', {'x': x, 'y': y});
  }
}