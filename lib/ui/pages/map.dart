import 'dart:async';
import 'dart:math';
import 'package:provider/provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:geolocator/geolocator.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';

enum MapStyle {
  defaultStyle('デフォルト', 'default'),
  basic('ベーシック', 'basic'),
  fiord('Fiord', 'fiord'),;

  const MapStyle(this.displayName, this.filename);
  final String displayName;
  final String filename;

  @override
  String toString() => 'assets/map_style/$filename.json';

  static MapStyle? byName(String? value) {
    if (value == null) return null;
    return MapStyle.values.firstWhereOrNull((e) => e.name == value);
  }
}

class MapView extends StatefulWidget {
  final String? stationId;
  final String? lineId;

  const MapView({this.stationId, this.lineId, super.key});

  @override
  State<StatefulWidget> createState() => _MapViewState();
}

class _MapViewState extends State<MapView> {
  final _mapReadyCompleter = Completer<MaplibreMapController>();
  bool _isRendering = false;
  bool _isNormalMode = false;
  bool _hidePoints = false;
  DateTime _lastRectUpdate = DateTime.now();
  MyLocationTrackingMode _trackingMode = MyLocationTrackingMode.None;
  final StationRepository _stationRepository = StationRepository();

  void showLoading() {
    const snackBar = SnackBar(
      content: Row(
        children: [
          SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(),
          ),
          SizedBox(width: 32),
          Text('計算中...', style: TextStyle(fontSize: 18)),
        ],
      ),
      duration: Duration(seconds: 30),
      shape: StadiumBorder(),
      margin: EdgeInsets.only(left: 23, right: 23, bottom: 23),
      behavior: SnackBarBehavior.floating,
    );

    ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(snackBar);
  }

  void hideLoading() {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
  }

  Map<String, dynamic> _buildVoronoi(List<Station> stations) {
    final List<Map<String, dynamic>> features = [];
    for (var station in stations) {
      final voronoi = station.voronoi;
      voronoi['properties']['accessed'] = AccessCacheManager.get(station.id) != null;
      features.add(voronoi);
    }

    return {
      'type': 'FeatureCollection',
      'features': features,
    };
  }

  Map<String, dynamic> _buildPoint(List<Station> stations) {
    final List<Map<String, dynamic>> point = [];
    for (var station in stations) {
      point.add({
        'type': 'Feature',
        'geometry': {
          'type': 'Point',
          'coordinates': [station.lng, station.lat],
        },
        'properties': {
          'name': station.name,
          'accessed': AccessCacheManager.get(station.id) != null,
        },
      });
    }

    return {
      'type': 'FeatureCollection',
      'features': point,
    };
  }

  void _renderVoronoi() async {
    final isCooldown = _trackingMode != MyLocationTrackingMode.None && DateTime.now().difference(_lastRectUpdate).inMilliseconds < 1000;
    if (_isRendering || isCooldown) return;
    _isRendering = true;

    final controller = await _mapReadyCompleter.future;

    final zoom = controller.cameraPosition?.zoom;
    if (zoom != null && zoom > 10.0) {
      final bounds = await controller.getVisibleRegion();
      final north = bounds.northeast.latitude;
      final east = bounds.northeast.longitude;
      final south = bounds.southwest.latitude;
      final west = bounds.southwest.longitude;

      final margin = min(max(north - south, east - west) * 0.5, 0.5);

      showLoading();
      final stations = await StationManager.updateRect(
        north + margin,
        east + margin,
        south - margin,
        west - margin,
        maxResults: 2000,
      );

      controller.setGeoJsonSource('voronoi', _buildVoronoi(stations));
      controller.setGeoJsonSource('point', _buildPoint(stations));

      hideLoading();
    }

    _isRendering = false;
    _lastRectUpdate = DateTime.now();
  }

  void _renderSingleStation() async {
    final controller = await _mapReadyCompleter.future;

    final station = await _stationRepository.get(widget.stationId!, column: 'id');
    if (station == null) return;

    controller.setGeoJsonSource('voronoi', _buildVoronoi([station]));
    controller.setGeoJsonSource('point', _buildPoint([station]));
    controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
      target: LatLng(station.lat, station.lng),
      zoom: 14.0,
    )));
  }

  void _renderSingleLine() async {
    final controller = await _mapReadyCompleter.future;

    final line = await LineRepository().get(widget.lineId!, column: 'id');
    if (line == null || line.polylineList == null) return;

    final stations = await Future.wait(line.stationList.map((x) async {
      final station = await _stationRepository.get(x, column: 'id');
      if (station == null) throw Exception('Station not found');
      return station;
    }));

    controller.setGeoJsonSource('voronoi', line.polylineList as Map<String, dynamic>);
    controller.setGeoJsonSource('point', _buildPoint(stations));

    final bounds = getBoundsFromLine(line);
    if (bounds != null) {
      controller.moveCamera(CameraUpdate.newLatLngBounds(bounds));
    }
  }

  @override
  Widget build(BuildContext context) {
    final config = Provider.of<ConfigProvider>(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.stationId != null ? 'マップ (駅情報)' : widget.lineId != null ? 'マップ (路線情報)' : 'マップ'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          setState(() {
            _trackingMode = _trackingMode == MyLocationTrackingMode.None ? MyLocationTrackingMode.Tracking : MyLocationTrackingMode.None;
          });
        },
        foregroundColor: _trackingMode != MyLocationTrackingMode.None ? Theme.of(context).colorScheme.primary : null,
        child: Icon(_trackingMode != MyLocationTrackingMode.None ? Icons.gps_fixed : Icons.gps_not_fixed),
      ),
      body: SafeArea(
        child: Stack(children: [
          MaplibreMap(
            initialCameraPosition: const CameraPosition(target: LatLng(35.681236, 139.767125), zoom: 10.0),
            trackCameraPosition: true,
            onMapCreated: (controller) {
              _mapReadyCompleter.complete(controller);
            },
            onStyleLoadedCallback: () async {
              final controller = await _mapReadyCompleter.future;
              final imageBuffer = await rootBundle.load('assets/icon/location.png');

              await controller.addImage('pin', imageBuffer.buffer.asUint8List(), true);
              await controller.addGeoJsonSource('voronoi', _buildVoronoi([]));
              await controller.addGeoJsonSource('point', _buildPoint([]));

              await controller.addFillLayer('voronoi', 'fill', FillLayerProperties(
                fillColor: [
                  'case',
                  ['get', 'accessed'],
                  Colors.red.toHexStringRGB(),
                  '#fff',
                ],
                fillOpacity: [
                  'case',
                  ['get', 'accessed'],
                  0.3,
                  0.0,
                ],
              ));

              await controller.addLineLayer('voronoi', 'line', LineLayerProperties(
                lineColor: Colors.red.toHexStringRGB(),
                lineWidth: (widget.lineId != null || widget.stationId != null) ? 2.0 : 1.0,
              ));

              await controller.addSymbolLayer('point', 'point', SymbolLayerProperties(
                textField: [Expressions.get, 'name'],
                textHaloWidth: 2,
                textSize: 14,
                textColor: '#444',
                textHaloColor: '#ffffff',
                textFont: ['migu2m-regular'],
                textOffset: [
                  Expressions.literal,
                  [0, 2]
                ],
                iconImage: 'pin',
                iconSize: 0.2,
                iconColor: Colors.red.toHexStringRGB(),
              ), minzoom: 12);

              if (widget.stationId != null) {
                _renderSingleStation();
                return;
              }

              if (widget.lineId != null) {
                _renderSingleLine();
                return;
              }

              _isNormalMode = true;

              final location = await Geolocator.getCurrentPosition();
              controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
                target: LatLng(location.latitude, location.longitude),
                zoom: 12.0,
              )));
            },
            onCameraIdle: () async {
              if (!_isNormalMode) return;
              _renderVoronoi();
            },
            onCameraTrackingDismissed: () {
              setState(() {
                _trackingMode = MyLocationTrackingMode.None;
              });
            },
            styleString: config.mapStyle.toString(),
            myLocationEnabled: true,
            myLocationTrackingMode: _trackingMode,
            compassViewPosition: CompassViewPosition.TopLeft,
          ),
          Positioned(
            top: 0,
            right: 0,
            child: Container(
              padding: const EdgeInsets.all(8),
              child: IconButton.filledTonal(
                icon: Icon(_hidePoints ? Icons.layers : Icons.layers_clear),
                onPressed: () {
                  setState(() {
                    _hidePoints = !_hidePoints;
                    _mapReadyCompleter.future.then((value) {
                      value.setLayerVisibility('point', !_hidePoints);
                      value.setLayerVisibility('fill', !_hidePoints);
                    });
                  });
                },
              ),
            ),
          ),
        ]),
      )
    );
  }
}