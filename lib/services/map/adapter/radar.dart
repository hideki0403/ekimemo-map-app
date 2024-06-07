import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:maplibre_gl/maplibre_gl.dart';

import 'package:ekimemo_map/services/radar.dart';
import 'package:ekimemo_map/services/cache.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';
import '../map_adapter.dart';
import '../utils.dart';

class RadarMapAdapter extends MapAdapter {
  RadarMapAdapter(super.parent);

  final _radarService = SearchRadarRange();
  final _maxRange = Config.maxResults;

  @override
  List<Widget> get floatingWidgets => [
    ElevatedButton(
      onPressed: () {
        _radarService.terminate();
        parent.context.pop();
      },
      child: const Text('戻る'),
    ),
  ];

  @override
  void initialize() async {
    await controller.addLineLayer('voronoi', 'line', masterLineLayerProperties.copyWith(const LineLayerProperties(
      lineColor: ['get', 'color'],
    )));

    await controller.addFillLayer('voronoi', 'fill', masterFillLayerProperties.copyWith(const FillLayerProperties(
      fillColor: ['get', 'color'],
      fillOpacity: 0.3,
    )));

    await controller.setLayerVisibility('fill', false);

    _renderRadar();
  }

  Future<void> _renderRadar() async {
    final callback = HighVoronoiCallback(
      onStarted: () {
        parent.showLoading();
      },
      onProgress: (List<RadarPolygon> polygons) async {
        final features = polygons.map((e) => e.toGeoJson(_maxRange)).toList();
        controller.setGeoJsonSource('voronoi', {
          'type': 'FeatureCollection',
          'features': features,
        });

        controller.moveCamera(CameraUpdate.newLatLngBounds(getBounds(polygons.last.polygon, margin: true)));
      },
      onComplete: (List<RadarPolygon> polygons) async {
        final features = polygons.map((e) => e.toGeoJson(_maxRange)).toList();
        // final features = [polygons.last.toGeoJson()];
        controller.setGeoJsonSource('voronoi', {
          'type': 'FeatureCollection',
          'features': features,
        });
        controller.moveCamera(CameraUpdate.newLatLngBounds(getBounds(polygons.last.polygon, margin: true)));
        parent.removeOverlay();
      },
      onError: () {
        parent.setOverlay(const Text('エラーが発生しました'));
      },
    );

    final station = await StationCache.get(int.parse(parent.widget.radarId!));
    if (station == null) return;
    _radarService.run(station, _maxRange, callback);
  }
}
