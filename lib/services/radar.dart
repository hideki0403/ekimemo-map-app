import 'dart:async';
import 'dart:isolate';
import 'package:flutter/material.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'cache.dart';
import 'log.dart';
import 'diagram/types.dart';
import 'diagram/voronoi.dart';
import 'diagram/rect.dart' as rect;

final logger = Logger('Radar');
final workerLogger = Logger('Radar:worker');

class StationPoint extends Point {
  StationPoint(super.x, super.y, this.code);
  int code;
}

class LatLngPoint {
  LatLngPoint(this.lat, this.lng);
  final double lat;
  final double lng;
}

class RadarPolygon {
  RadarPolygon(this.index, this.polygon);
  final int index;
  final List<LatLngPoint> polygon;

  Map<String, dynamic> toGeoJson([int? radarK]) {
    return {
      'type': 'Feature',
      'properties': {
        'color': radarK != null ? HSVColor.fromAHSV(1.0, index / radarK * 360, 0.6, 1.0).toColor().toHexStringRGB() : 0xff000000,
      },
      'geometry': {
        'type': 'Polygon',
        'coordinates': [polygon.map((e) => [e.lng, e.lat]).toList()],
      },
    };
  }
}

class HighVoronoiCallback {
  HighVoronoiCallback({this.onProgress, this.onComplete, this.onStarted, this.onError, this.onCanceled});
  final void Function(List<RadarPolygon> polygon)? onProgress;
  final void Function(List<RadarPolygon> polygon)? onComplete;
  final void Function()? onStarted;
  final void Function()? onError;
  final void Function()? onCanceled;
}

typedef StationProvider = Future<List<StationPoint>> Function(StationPoint x);

class SearchRadarRange {
  final List<RadarPolygon> _polygon = [];
  Isolate? _worker;

  SearchRadarRange();

  bool get isRunning => _worker != null;

  Future<List<StationPoint>> _provider(int code) async {
    final station = await StationCache.get(code);
    if (station == null) {
      throw Exception('Station not found: $code');
    }
    return await Future.wait(station.delaunay.map((e) => StationCache.get(e))).then((value) {
      return value.where((e) => e != null).map((e) => StationPoint(e!.lng, e.lat, e.code)).toList();
    });
  }

  Future<void> run(Station station, int radarK, HighVoronoiCallback callback) async {
    if (isRunning) {
      logger.warning('RadarRangeSearchService is already running');
      callback.onError?.call();
      return;
    }

    SendPort? workerSendPort;

    final receivePort = ReceivePort();
    receivePort.listen((message) async {
      switch (message['type']) {
        case 'ready': {
          workerSendPort = message['receivePort'];
          workerSendPort?.send({'type': 'start'});
          break;
        }
        case 'point': {
          final code = message['code'] as int;
          final station = await _provider(code);
          workerSendPort?.send({'type': 'point', 'code': code, 'point': station});
        }
        case 'progress': {
          _polygon.add(message['polygon']);
          callback.onProgress?.call(_polygon);
          break;
        }
        case 'complete': {
          terminate();
          callback.onComplete?.call(_polygon);
          break;
        }
        case 'error': {
          terminate();
          callback.onError?.call();
          break;
        }
      }
    });

    final container = rect.getContainer(rect.init(127, 46, 146, 26));
    final center = StationPoint(station.lng, station.lat, station.code);

    _worker = await Isolate.spawn(_voronoiWorker, [receivePort.sendPort, center, container, radarK]);
    callback.onStarted?.call();
  }

  void terminate() {
    _worker?.kill(priority: Isolate.immediate);
    _worker = null;
  }
}

void _voronoiWorker(List<dynamic> args) {
  final sendPort = args[0] as SendPort;
  final center = args[1] as StationPoint;
  final container = args[2] as Triangle;
  final level = args[3] as int;

  final providerCompleter = <int, Completer>{};

  void progress(int index, List<Point> polygon) {
    sendPort.send({
      'type': 'progress',
      'polygon': RadarPolygon(index, polygon.map((e) => LatLngPoint(e.y, e.x)).toList()),
    });
  }

  Future<List<StationPoint>> provider(StationPoint x) async {
    final completer = Completer<List<StationPoint>>();
    providerCompleter[x.code] = completer;
    sendPort.send({'type': 'point', 'code': x.code});
    return completer.future;
  }

  final voronoi = Voronoi<StationPoint>(center, container, provider);
  final receivePort = ReceivePort();

  receivePort.listen((message) {
    switch (message['type']) {
      case 'start': {
        voronoi.execute(level, progress).then((_) => sendPort.send({'type': 'complete'})).catchError((e) {
          workerLogger.error('Error: $e');
          sendPort.send({'type': 'error'});
        });
        break;
      }
      case 'point': {
        final code = message['code'] as int;
        final completer = providerCompleter[code];
        if (completer != null) completer.complete(message['point']);
      }
    }
  });

  sendPort.send({'type': 'ready', 'receivePort': receivePort.sendPort});
}
