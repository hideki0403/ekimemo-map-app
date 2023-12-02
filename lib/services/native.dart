import 'package:flutter/services.dart';

class NativeMethods {
  static final _instance = NativeMethods._internal();
  factory NativeMethods() => _instance;
  NativeMethods._internal();

  static const _channel = MethodChannel('dev.yukineko.ekimemo_map/native');

  Future<String> getCommitHash() async {
    return await _channel.invokeMethod('getCommitHash');
  }
}