import 'package:flutter/services.dart';

class NativeMethods {
  static final _instance = NativeMethods._internal();
  factory NativeMethods() => _instance;
  NativeMethods._internal();

  static const _channel = MethodChannel('dev.yukineko.ekimemo_map/native');

  Future<String> getCommitHash() async {
    return await _channel.invokeMethod('getCommitHash');
  }

  Future<bool> hasPermission() async {
    return await _channel.invokeMethod('hasPermission');
  }

  Future<void> setDebugPackageName(String packageName) async {
    return await _channel.invokeMethod('setDebugPackageName', {'packageName': packageName});
  }

  Future<void> performTap(double x, double y) async {
    return await _channel.invokeMethod('performTap', {'x': x, 'y': y});
  }
}