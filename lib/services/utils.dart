import 'dart:math';
import 'package:flutter/material.dart';

import 'package:ekimemo_map/main.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';

int measure(double plat1, double plng1, double plat2, double plng2) {
  final lng1 = pi * plng1 / 180;
  final lat1 = pi * plat1 / 180;
  final lng2 = pi * plng2 / 180;
  final lat2 = pi * plat2 / 180;
  final lng = (lng1 - lng2) / 2;
  final lat = (lat1 - lat2) / 2;
  return (6378137.0 * 2 * asin(sqrt(pow(sin(lat), 2) + cos(lat1) * cos(lat2) * pow(sin(lng), 2)))).toInt();
}

String beautifyDistance(int distance) {
  if (distance < 1000) {
    return '${distance}m';
  } else {
    return '${(distance / 1000).toStringAsFixed(1)}km';
  }
}

String beautifySeconds(int seconds, {bool jp = false}) {
  if (jp) {
    return '${(seconds / 60).floor()}分${(seconds % 60).toString().padLeft(2, '0')}秒';
  } else {
    return '${(seconds / 60).floor()}:${(seconds % 60).toString().padLeft(2, '0')}';
  }
}

// TODO: テーマに合わせてアイコン色を調整する
Icon getAttrIcon(StationAttr attr) {
  const scale = 20.0;
  switch (attr) {
    case StationAttr.eco:
      return Icon(Icons.energy_savings_leaf, color: Colors.green.shade300, size: scale);
    case StationAttr.heat:
      return Icon(Icons.whatshot, color: Colors.red.shade300, size: scale);
    case StationAttr.cool:
      return Icon(Icons.water_drop, color: Colors.blue.shade300, size: scale);
    default:
      return const Icon(Icons.adjust, color: Colors.grey, size: scale);
  }
}

int getCoolDownTime(StationData data) {
  if (data.isNew || data.lastAccess == null || !Config.enableReminder) return 0;

  final coolDown = Config.cooldownTime;
  final timeDiff = DateTime.now().difference(data.lastAccess!).inSeconds;
  if (timeDiff > coolDown) return 0;

  return coolDown - timeDiff;
}

int getCoolDownTimeFromAccessLog(AccessLog log) {
  final coolDown = Config.cooldownTime;
  final timeDiff = DateTime.now().difference(log.lastAccess).inSeconds;
  if (timeDiff > coolDown) return 0;

  return coolDown - timeDiff;
}

Future<String?> showEditorDialog({String? data, String? title, String? caption, String? suffix, EditorDialogType? type}) async {
  return showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return EditorDialog(data: data, title: title, caption: caption, suffix: suffix, type: type);
    },
  );
}