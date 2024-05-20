import 'dart:async';
import 'dart:math';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:geolocator/geolocator.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/services/station.dart';

class MapView extends StatefulWidget {
  final String? stationId;
  final String? lineId;

  const MapView({this.stationId, this.lineId, super.key});

  @override
  State<StatefulWidget> createState() => _MapViewState();
}

class _MapViewState extends State<MapView> {
  final _mapReadyCompleter = Completer<MaplibreMapController>();
  final _hideCountThreshold = 2000;
  final _hideZoomThreshold = 10.0;

  Map<String, dynamic> _buildVoronoi(List<Station> stations) {
    final List<Map<String, dynamic>> voronoi = [];
    for (var station in stations) {
      voronoi.add(station.voronoi);
    }

    return {
      'type': 'FeatureCollection',
      'features': voronoi,
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
        },
      });
    }

    return {
      'type': 'FeatureCollection',
      'features': point,
    };
  }

  void _renderVoronoi() async {
    final controller = await _mapReadyCompleter.future;

    final zoom = controller.cameraPosition?.zoom;
    if (zoom == null || zoom > _hideZoomThreshold) return;

    final bounds = await controller.getVisibleRegion();
    final north = bounds.northeast.latitude;
    final east = bounds.northeast.longitude;
    final south = bounds.southwest.latitude;
    final west = bounds.southwest.longitude;

    final margin = min(max(north - south, east - west) * 0.5, 0.5);
    final stations = await StationManager.updateRectRegion(
      north + margin,
      east + margin,
      south - margin,
      west - margin,
      maxResults: _hideCountThreshold,
    );

    if (stations.length >= _hideCountThreshold) {
      controller.setLayerVisibility('voronoi', false);
      controller.setLayerVisibility('point', false);
      return;
    }

    controller.setGeoJsonSource('voronoi', _buildVoronoi(stations));
    controller.setGeoJsonSource('point', _buildPoint(stations));
    controller.setLayerVisibility('voronoi', true);
    controller.setLayerVisibility('point', true);
  }

  void _renderSingleStation() async {
    final controller = await _mapReadyCompleter.future;

    final station = await StationRepository().get(widget.stationId!);
    if (station == null) return;

    controller.setGeoJsonSource('voronoi', _buildVoronoi([station]));
    controller.setGeoJsonSource('point', _buildPoint([station]));
    controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
      target: LatLng(station.lat, station.lng),
      zoom: 14.0,
    )));
  }

  void _renderSingleLine() async {
    // TODO: 路線の描画
  }

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('マップ'),
      ),
      body: SafeArea(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(child: MaplibreMap(
              initialCameraPosition: const CameraPosition(target: LatLng(35.681236, 139.767125), zoom: 10.0),
              onMapCreated: (controller) {
                _mapReadyCompleter.complete(controller);
              },
              onStyleLoadedCallback: () async {
                final controller = await _mapReadyCompleter.future;
                final imageBuffer = await rootBundle.load('assets/icon/map-pin.png');

                await controller.addImage('pin', imageBuffer.buffer.asUint8List(), true);
                await controller.addGeoJsonSource('voronoi', _buildVoronoi([]));
                await controller.addGeoJsonSource('point', _buildPoint([]));

                await controller.addLineLayer('voronoi', 'voronoi', const LineLayerProperties(
                  lineColor: '#ff0000',
                  lineWidth: 1.0,
                ), minzoom: _hideZoomThreshold);

                await controller.addSymbolLayer('point', 'point', const SymbolLayerProperties(
                  textField: [Expressions.get, 'name'],
                  textHaloWidth: 1,
                  textSize: 14,
                  textHaloColor: '#ffffff',
                  textOffset: [
                    Expressions.literal,
                    [0, 2]
                  ],
                  iconImage: 'pin',
                  iconSize: 0.4,
                  iconColor: '#f7606d',
                ), minzoom: 12);

                if (widget.stationId != null) {
                  _renderSingleStation();
                  return;
                }

                if (widget.lineId != null) {
                  _renderSingleLine();
                  return;
                }

                final location = await Geolocator.getCurrentPosition();
                controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
                  target: LatLng(location.latitude, location.longitude),
                  zoom: 12.0,
                )));
              },
              onCameraIdle: () async {
                if (widget.stationId != null || widget.lineId != null) return;
                _renderVoronoi();
              },
              styleString: 'https://assets.yukineko.dev/map/style/google_maps_style.json',
              // myLocationEnabled: true,
            )),
            Padding(
              padding: const EdgeInsets.only(top: 8, bottom: 8),
              child: RichText(
                text: TextSpan(
                  style: const TextStyle(color: Colors.black),
                  children: [
                    const TextSpan(text: '©'),
                    TextSpan(text: 'OpenStreetMap', style: const TextStyle(color: Colors.blue), recognizer: TapGestureRecognizer()..onTap = () => launchUrlString('https://www.openstreetmap.org/copyright')),
                    const TextSpan(text: ' contributors'),
                  ],
                ),
              ),
            ),
          ],
        )
      )
    );
  }
}