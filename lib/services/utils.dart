import 'dart:math';
import 'package:flutter/material.dart';

import 'package:ekimemo_map/main.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/radar.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/access_log.dart';
import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';
import 'package:ekimemo_map/ui/widgets/select_dialog.dart';
import 'package:ekimemo_map/ui/widgets/checkbox_dialog.dart';
import 'package:maplibre_gl/maplibre_gl.dart' as maplibre;

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

Icon getAttrIcon(StationAttr attr, { BuildContext? context }) {
  const scale = 20.0;

  var isDark = false;
  if (context != null) {
    if (Config.themeMode == ThemeMode.system) {
      isDark = MediaQuery.of(context).platformBrightness == Brightness.dark;
    } else {
      isDark = Config.themeMode == ThemeMode.dark;
    }
  }

  switch (attr) {
    case StationAttr.eco:
      return Icon(Icons.energy_savings_leaf, color: !isDark ? Colors.green.shade400 : Colors.green.shade300, size: scale);
    case StationAttr.heat:
      return Icon(Icons.whatshot, color: !isDark ? Colors.red.shade400 : Colors.red.shade300, size: scale);
    case StationAttr.cool:
      return Icon(Icons.water_drop, color: !isDark ? Colors.blue.shade400 : Colors.blue.shade300, size: scale);
    default:
      return Icon(Icons.adjust, color: !isDark ? Colors.grey.shade600 : Colors.grey.shade400, size: scale);
  }
}

int getCoolDownTime(int stationId) {
  final data = AccessCacheManager.getTime(stationId);
  if (data == null || !Config.enableReminder) return 0;

  final coolDown = Config.cooldownTime;
  final timeDiff = DateTime.now().difference(data).inSeconds;
  if (timeDiff > coolDown) return 0;

  return coolDown - timeDiff;
}

int getCoolDownTimeFromAccessLog(AccessLog log) {
  final coolDown = Config.cooldownTime;
  final timeDiff = DateTime.now().difference(log.lastAccess).inSeconds;
  if (timeDiff > coolDown) return 0;

  return coolDown - timeDiff;
}

String rectToString(Rect rect) {
  return '${rect.left},${rect.top},${rect.right},${rect.bottom}';
}

Rect stringToRect(String str) {
  final rawRect = str.split(',').map((e) => double.parse(e)).toList();
  return Rect.fromLTRB(rawRect[0], rawRect[1], rawRect[2], rawRect[3]);
}

double randomInRange(double min, double max) {
  return min + Random().nextDouble() * (max - min);
}

const _bytesSuffixes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
String formatBytes(int bytes, {int decimals = 1}) {
  if (bytes <= 0) return '0B';
  final i = (log(bytes) / log(1024)).floor();
  return '${(bytes / pow(1024, i)).toStringAsFixed(decimals)}${_bytesSuffixes[i]}';
}

maplibre.LatLngBounds? getBoundsFromLine(Line line) {
  if (line.polylineList == null) return null;

  final properties = line.polylineList?['properties'];
  if (properties == null) return null;

  final north = properties['north'];
  final east = properties['east'];
  final south = properties['south'];
  final west = properties['west'];
  if (north == null || east == null || south == null || west == null) return null;

  return maplibre.LatLngBounds(
    southwest: maplibre.LatLng(south, west),
    northeast: maplibre.LatLng(north, east),
  );
}

maplibre.LatLngBounds getBounds(List<LatLngPoint> list, { bool margin = false }) {
  var north = -90.0;
  var south = 90.0;
  var east = -180.0;
  var west = 180.0;

  for (var p in list) {
    north = max(north, p.lat);
    south = min(south, p.lat);
    east = max(east, p.lng);
    west = min(west, p.lng);
  }

  if (margin) {
    final margin = max(north - south, east - west) * 0.05;
    north += margin;
    south -= margin;
    east += margin;
    west -= margin;
  }

  return maplibre.LatLngBounds(
    southwest: maplibre.LatLng(south, west),
    northeast: maplibre.LatLng(north, east),
  );
}

typedef ContextReceiver = void Function(BuildContext context);
Future<void> showMessageDialog({
  String? title,
  String? message,
  Widget? content,
  ContextReceiver? receiver,
  List<Widget>? actions,
  bool disableClose = false,
  bool disableActions = false,
}) async {
  return showDialog(
    context: navigatorKey.currentContext!,
    barrierDismissible: !disableClose,
    builder: (context) {
      receiver?.call(context);
      return PopScope(
        canPop: !disableClose,
        child: AlertDialog(
          title: title != null ? Text(title) : null,
          content: content ?? (message != null ? Text(message) : null),
          actions: disableActions ? null : actions ?? [
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: const Text('OK'),
            ),
          ],
        ),
      );
    },
  );
}

Future<bool?> showYesNoDialog({String? title, String? message, String? yesText, String? noText}) async {
  return await showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return AlertDialog(
        title: title != null ? Text(title) : null,
        content: message != null ? Text(message) : null,
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop(false);
            },
            child: Text(noText ?? 'いいえ'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop(true);
            },
            child: Text(yesText ?? 'はい'),
          ),
        ],
      );
    },
  );
}

Future<String?> showEditorDialog({String? data, String? title, String? caption, String? suffix, EditorDialogType? type}) async {
  return showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return EditorDialog(data: data, title: title, caption: caption, suffix: suffix, type: type);
    },
  );
}

Future<String?> showSelectDialog({
  required Map<String, String> data,
  String? title,
  String? defaultValue,
  String? caption,
  bool? noRadio,
  bool? showOkButton,
  Function(String?)? onChanged,
}) async {
  return showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return SelectDialog(data: data, defaultValue: defaultValue, title: title, caption: caption, noRadio: noRadio, showOkButton: showOkButton, onChanged: onChanged);
    },
  );
}

Future<Map<String, bool>?> showCheckboxDialog({required Map<String, bool> data, String? title, String? caption}) async {
  return showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return CheckboxDialog(data: data, title: title, caption: caption);
    },
  );
}

Future<bool?> showConfirmDialog({String? title, String? caption}) async {
  return showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return AlertDialog(
        title: title != null ? Text(title) : null,
        content: caption != null ? Text(caption) : null,
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop(false);
            },
            child: const Text('キャンセル'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop(true);
            },
            child: const Text('OK'),
          ),
        ],
      );
    },
  );
}

Future<Color?> showColorPickerDialog({List<Color>? defaultColor, String? title}) async {
  final colors = defaultColor ?? [Colors.red, Colors.pink, Colors.purple, Colors.deepPurple, Colors.indigo, Colors.blue, Colors.lightBlue, Colors.cyan, Colors.teal, Colors.green, Colors.lightGreen, Colors.lime, Colors.yellow, Colors.amber, Colors.orange, Colors.deepOrange, Colors.brown];
  return await showDialog(
    context: navigatorKey.currentContext!,
    builder: (context) {
      return AlertDialog(
        title: Text(title ?? 'カラーピッカー'),
        content: SizedBox(
          width: double.maxFinite,
          child: GridView.builder(
            shrinkWrap: true,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 5,
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
            ),
            itemCount: colors.length,
            itemBuilder: (context, index) {
              final c = colors[index];
              return GestureDetector(
                onTap: () {
                  Navigator.of(context).pop(c);
                },
                child: Container(
                  width: 24,
                  height: 24,
                  decoration: BoxDecoration(
                    color: c,
                    shape: BoxShape.circle,
                  ),
                ),
              );
            },
          ),
        )
      );
    },
  );
}

Color hexToColor(String? hex) {
  if (hex == null) return Colors.transparent;
  hex = hex.replaceAll('#', '');
  if (hex.length == 6) {
    hex = 'FF$hex';
  }
  return Color(int.parse(hex, radix: 16));
}
