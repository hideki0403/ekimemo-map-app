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
  bool _selectedRangeOnly = true;
  List<RadarPolygon> _polygonCache = [];

  @override
  String get title => 'レーダー範囲';

  @override
  List<Widget> get appBarActions => [
    IconButton(
      icon: const Icon(Icons.help),
      onPressed: () {
        showMessageDialog(
          title: 'レーダー範囲について',
          message: '''
          レーダーで対象の駅にアクセスできる範囲を可視化したマップです。
          ポリゴン (線で囲まれている範囲) 内であれば理論上レーダーでアクセスすることができます。
          初期状態ではレーダーの検知数が$_maxRange駅の場合の範囲が表示されますが、これはマップ下部のスライダーで変更可能です。
          '''.replaceAll(RegExp(r' {2,}'), ''),
        );
      },
    ),
  ];

  @override
  List<Widget> get floatingWidgets => [
    ElevatedButton(
      onPressed: _polygonCache.isEmpty ? null : () {
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
        controller.moveCamera(CameraUpdate.newLatLngBounds(getBounds(polygons.last.polygon, margin: true)));
        parent.removeOverlay();

        _polygonCache = polygons;
        _reRender();

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
