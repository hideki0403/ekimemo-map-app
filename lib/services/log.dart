import 'dart:developer' as developer;
import 'package:flutter/material.dart';

enum LogType {
  debug(Colors.grey),
  info(Colors.blue),
  warning(Colors.yellow),
  error(Colors.red);

  const LogType(this.color);
  final Color color;
}

class LogObject {
  final LogType type;
  final String tag;
  final dynamic object;
  final DateTime time = DateTime.now();

  LogObject(this.type, this.tag, this.object);
}

class LogStateNotifier extends ChangeNotifier {
  static final _instance = LogStateNotifier._internal();
  factory LogStateNotifier() => _instance;
  LogStateNotifier._internal();

  List<LogObject> get logs => LogManager.logs;

  void notify() {
    notifyListeners();
  }

  void clear() {
    LogManager.logs.clear();
    notify();
  }
}

class LogManager {
  static const maxLogs = 3000;
  static final _logs = <LogObject>[];
  static final _stateNotifier = LogStateNotifier();

  static void push(LogObject log) {
    if (_logs.length >= maxLogs) _logs.removeAt(0);
    _logs.add(log);

    developer.log(log.object, name: '${log.tag}:${log.type.name.toUpperCase()}', time: log.time);
    _stateNotifier.notify();
  }

  static get logs => _logs;
}

class Logger {
  final String tag;

  Logger(this.tag);

  void debug(dynamic object) {
    LogManager.push(LogObject(LogType.debug, tag, object));
  }

  void info(dynamic object) {
    LogManager.push(LogObject(LogType.info, tag, object));
  }

  void warning(dynamic object) {
    LogManager.push(LogObject(LogType.warning, tag, object));
  }

  void error(dynamic object) {
    LogManager.push(LogObject(LogType.error, tag, object));
  }
}