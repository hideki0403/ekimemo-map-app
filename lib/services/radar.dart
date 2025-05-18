import 'dart:async';
import 'dart:isolate';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'log.dart' as log;
import 'diagram/types.dart';
import 'diagram/voronoi.dart';
import 'diagram/rect.dart' as rect;

final logger = log.Logger('Radar');

class StationPoint extends Point {
  StationPoint(super.x, super.y, this.id);
  int id;
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
  final _repository = StationRepository();
  final List<RadarPolygon> _polygon = [];
  Isolate? _worker;

  SearchRadarRange();

  bool get isRunning => _worker != null;

  Future<List<StationPoint>> _provider(int id) async {
    final station = await _repository.getOne(id);
    if (station == null) {
      throw Exception('Station not found: $id');
    }

    return _repository.get(station.delaunay).then((value) {
      return value.map((e) => StationPoint(e.lng, e.lat, e.id)).toList();
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
          final id = message['id'] as int;
          final station = await _provider(id);
          workerSendPort?.send({'type': 'point', 'id': id, 'point': station});
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
          logger.error('Worker error: ${message['message']}');
          callback.onError?.call();
          break;
        }
        case 'log': {
          logger.debug(message['message']);
          break;
        }
      }
    });

    final container = rect.getContainer(rect.init(127, 46, 146, 26));
    final center = StationPoint(station.lng, station.lat, station.id);

    _worker = await Isolate.spawn(_voronoiWorker, [receivePort.sendPort, center, container, radarK]);
    callback.onStarted?.call();
  }

  void terminate() {
    _worker?.kill(priority: Isolate.immediate);
    _worker = null;
    logger.debug('VoronoiWorker terminated');
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

  void logger(String message) {
    sendPort.send({'type': 'log', 'message': message});
  }

  Future<List<StationPoint>> provider(StationPoint x) async {
    final completer = Completer<List<StationPoint>>();
    providerCompleter[x.id] = completer;
    sendPort.send({'type': 'point', 'id': x.id});
    return completer.future;
  }

  final voronoi = Voronoi<StationPoint>(center, container, provider);
  final receivePort = ReceivePort();

  receivePort.listen((message) {
    switch (message['type']) {
      case 'start': {
        voronoi.execute(level, progress, logger).then((_) => sendPort.send({'type': 'complete'})).catchError((e) {
          sendPort.send({'type': 'error', 'message': e.toString()});
        });
        break;
      }
      case 'point': {
        final id = message['id'] as int;
        final completer = providerCompleter[id];
        if (completer != null) completer.complete(message['point']);
      }
    }
  });

  sendPort.send({'type': 'ready', 'receivePort': receivePort.sendPort});
}
