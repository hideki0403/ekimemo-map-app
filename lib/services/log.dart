import 'dart:developer' as developer;

enum LogType {
  debug,
  info,
  warning,
  error,
}

class LogObject {
  final LogType type;
  final String tag;
  final dynamic object;
  final DateTime time = DateTime.now();

  LogObject(this.type, this.tag, this.object);
}

class LogManager {
  static const maxLogs = 3000;
  static final _logs = <LogObject>[];

  static void push(LogObject log) {
    if (_logs.length >= maxLogs) _logs.removeAt(0);
    _logs.add(log);

    developer.log(log.object, name: '${log.tag}:${log.type.name.toUpperCase()}', time: log.time);
  }

  static get logs => _logs;
}

class Logger {
  final String tag;

  Logger(this.tag);

  void debug(String message) {
    LogManager.push(LogObject(LogType.debug, tag, message));
  }

  void info(String message) {
    LogManager.push(LogObject(LogType.info, tag, message));
  }

  void warning(String message) {
    LogManager.push(LogObject(LogType.warning, tag, message));
  }

  void error(String message) {
    LogManager.push(LogObject(LogType.error, tag, message));
  }
}