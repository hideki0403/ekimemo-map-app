import 'dart:math';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';

import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/ui/pages/map.dart';
import '../map_adapter.dart';
import '../utils.dart';

class CoreMapAdapter extends MapAdapter {
  bool _hidePointLayer = false;
  bool _hideAccessState = false;
  bool _isRendering = false;
  bool _isSearchingStation = false;
  DateTime _lastRectUpdate = DateTime.now();

  bool get attrMode => false;

  CoreMapAdapter(super.parent);

  @override
  List<Widget> get floatingWidgets => [
    ElevatedButton(
      onPressed: () {
        _hidePointLayer = !_hidePointLayer;
        controller.setLayerVisibility('point', !_hidePointLayer);
        parent.rebuildWidget();
      },
      child: Text('${ attrMode ? '属性アイコン': 'マップピン' }表示: ${_hidePointLayer ? 'OFF' : 'ON'}'),
    ),
    ElevatedButton(
      onPressed: () {
        _hideAccessState = !_hideAccessState;
        controller.setLayerVisibility('fill', !_hideAccessState);
        parent.rebuildWidget();
      },
      child: Text('${ attrMode ? '塗りつぶし' : 'アクセス状態' }表示: ${_hideAccessState ? 'OFF' : 'ON'}'),
    ),
  ];

  @override
  List<Widget> get appBarActions => [
    IconButton(
      icon: const Icon(Icons.layers),
      onPressed: () => parent.useAdapter(attrMode ? MapAdapterType.core : MapAdapterType.attribute),
    ),
  ];

  @override
  void initialize() async {
    await controller.addFillLayer('voronoi', 'fill', masterFillLayerProperties.copyWith(const FillLayerProperties(
      fillOpacity: [
        'case',
        ['get', 'accessed'],
        0.3,
        0.0,
      ],
    )));

    await controller.addLineLayer('voronoi', 'line', masterLineLayerProperties);
    await controller.addSymbolLayer('point', 'point', masterSymbolLayerProperties);

    renderVoronoi();
  }

  @override
  void onCameraIdle() {
    renderVoronoi();
  }

  @override
  void onMapLongClick(Point<double> point, LatLng latLng) async {
    if (_isSearchingStation) return;

    _isSearchingStation = true;
    parent.setOverlay(const Text('駅情報を取得中...'));

    final data = await StationSearchService.getNearestStation(latLng.latitude, latLng.longitude);

    if (_hideAccessState) {
      parent.setOverlay(Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            getAttrIcon(data.station.attr, context: parent.context.mounted ? parent.context : null),
            const SizedBox(width: 4),
            Text(data.station.name),
          ]
      ));
    } else {
      final accessLog = AccessCacheManager.get(data.station.id);
      final accessed = accessLog != null && accessLog.accessed;
      await AccessCacheManager.update(data.station.id, DateTime.now(), updateOnly: true, accessed: !accessed);
      await renderVoronoi(force: true);
      parent.setOverlay(Text('${data.station.name}を${accessed ? '未アクセス' : 'アクセス済み'}にしました'));
    }

    _isSearchingStation = false;
  }

  Future<void> renderVoronoi({ int? renderingLimit, bool force = false }) async {
    renderingLimit ??= Config.mapRenderingLimit;
    final isCooldown = parent.trackingMode != MyLocationTrackingMode.None && DateTime.now().difference(_lastRectUpdate).inMilliseconds < 1000;
    if (_isRendering || (isCooldown && !force)) return;
    _isRendering = true;

    final bounds = await controller.getVisibleRegion();
    final north = bounds.northeast.latitude;
    final east = bounds.northeast.longitude;
    final south = bounds.southwest.latitude;
    final west = bounds.southwest.longitude;

    final margin = min(max(north - south, east - west) * 0.5, 0.5);

    parent.showLoading();
    final stations = await StationManager.updateRect(
      north + margin,
      east + margin,
      south - margin,
      west - margin,
      maxResults: renderingLimit,
    );

    // マップが極端に拡大されていた場合は表示できる駅が無くなるため、画面中央から最短の駅を取得する
    if (stations.isEmpty) {
      final center = LatLng((north + south) / 2, (east + west) / 2);
      final data = await StationSearchService.getNearestStation(center.latitude, center.longitude);
      stations.add(data.station);
    }

    if (stations.length < renderingLimit) {
      controller.setGeoJsonSource('voronoi', buildVoronoi(stations, useAttrColor: attrMode));
      controller.setGeoJsonSource('point', buildPoint(stations, useAttr: attrMode));
      parent.removeOverlay();
    } else {
      parent.setOverlay(const Text('画面範囲内の駅数が多すぎるため、メッシュを描画できませんでした。地図を拡大してください。'));
    }

    _isRendering = false;
    _lastRectUpdate = DateTime.now();
  }
}