import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';

import 'package:ekimemo_map/services/radar.dart';
import 'package:ekimemo_map/services/cache.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/ui/widgets/cb_slider.dart';
import '../map_adapter.dart';
import '../utils.dart';

class RadarMapAdapter extends MapAdapter {
  RadarMapAdapter(super.parent);

  final _radarService = SearchRadarRange();
  final _maxRange = Config.maxResults;
  int _showRange = Config.maxResults;
  bool _selectedRangeOnly = false;
  List<RadarPolygon> _polygonCache = [];

  @override
  List<Widget> get floatingWidgets => [
    ElevatedButton(
      onPressed: () {
        _selectedRangeOnly = !_selectedRangeOnly;
        parent.rebuildWidget();
        _reRender();
      },
      child: Text(_selectedRangeOnly ? '選択した範囲のみ表示' : '選択した範囲以下を表示'),
    ),
  ];

  @override
  List<Widget> get bottomWidgets => [
    CbSlider(
      defaultValue: _showRange,
      min: 1,
      max: _maxRange,
      disabled: _polygonCache.isEmpty,
      onChanged: (value) {
        _showRange = value;
        _reRender();
      },
    )
  ];

  @override
  void initialize() async {
    await controller.addLineLayer('voronoi', 'line', masterLineLayerProperties.copyWith(const LineLayerProperties(
      lineColor: ['get', 'color'],
      lineWidth: 1.5,
    )));

    await controller.addFillLayer('voronoi', 'fill', masterFillLayerProperties.copyWith(const FillLayerProperties(
      fillColor: ['get', 'color'],
      fillOpacity: 0.3,
    )));

    await controller.setLayerVisibility('fill', false);

    _renderRadar();
  }

  @override
  void onDispose() {
    _radarService.terminate();
  }

  void _reRender() async {
    if (_polygonCache.isEmpty) return;
    final polygons = _polygonCache.where((e) {
      return _selectedRangeOnly ? e.index == _showRange : e.index <= _showRange;
    }).toList();
    _renderPolygon(polygons);
    await controller.setLayerVisibility('fill', _selectedRangeOnly);
  }

  void _renderPolygon(List<RadarPolygon> polygons) {
    final features = polygons.map((e) => e.toGeoJson(_showRange)).toList();
    controller.setGeoJsonSource('voronoi', {
      'type': 'FeatureCollection',
      'features': features,
    });
  }

  Future<void> _renderRadar() async {
    final callback = HighVoronoiCallback(
      onStarted: () {
        parent.showLoading();
      },
      onProgress: (List<RadarPolygon> polygons) async {
        _renderPolygon(polygons);
        controller.moveCamera(CameraUpdate.newLatLngBounds(getBounds(polygons.last.polygon, margin: true)));
      },
      onComplete: (List<RadarPolygon> polygons) async {
        _renderPolygon(polygons);
        controller.moveCamera(CameraUpdate.newLatLngBounds(getBounds(polygons.last.polygon, margin: true)));
        parent.removeOverlay();

        _polygonCache = polygons;
        parent.rebuildWidget();
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
