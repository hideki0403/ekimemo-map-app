import 'dart:math';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:bottom_sheet/bottom_sheet.dart';

import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/gps.dart';
import 'package:ekimemo_map/ui/pages/map.dart';
import '../map_adapter.dart';
import '../utils.dart';

class CoreMapAdapter extends MapAdapter {
  bool _hidePointLayer = false;
  bool _hideAccessState = false;
  bool _isRendering = false;
  DateTime _lastRectUpdate = DateTime.now();

  bool get attrMode => false;

  CoreMapAdapter(super.parent);

  @override
  List<Widget> get floatingWidgets => [
    ElevatedButton(
      onPressed: () => parent.useAdapter(attrMode ? MapAdapterType.core : MapAdapterType.attribute),
      child: Text(attrMode ? '通常マップに切り替え' : '属性マップに切り替え'),
    ),
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
  void initialize() async {
    await controller.addFillLayer('voronoi', 'fill', masterFillLayerProperties.copyWith(const FillLayerProperties(
      fillColor: [Expressions.get, 'color'],
      fillOpacity: [
        'case',
        ['get', 'accessed'],
        0.3,
        0.0,
      ],
    )));

    await controller.addLineLayer('voronoi', 'line', masterLineLayerProperties.copyWith(const LineLayerProperties(
      lineColor: [Expressions.get, 'color'],
    )));

    await controller.addSymbolLayer('point', 'point', masterSymbolLayerProperties.copyWith(const SymbolLayerProperties(
      iconColor: [Expressions.get, 'color'],
    )));

    renderVoronoi();
  }

  @override
  void onCameraIdle() {
    renderVoronoi();
  }

  @override
  void onMapClick(Point<double> point, LatLng latLng) async {
    parent.setOverlay(const Text('駅情報を取得中...'));

    final stations = await StationSearchService.getNearestStations(latLng.latitude, latLng.longitude, maxResults: Config.maxResults);
    for (final data in stations) {
      data.distance = beautifyDistance(measure(latLng.latitude, latLng.longitude, data.station.lat, data.station.lng));
    }

    final data = stations.first;

    final location = GpsManager.lastLocation;
    final distanceFromCurrent = location == null || !GpsManager.isEnabled ? null : beautifyDistance(measure(location.latitude, location.longitude, data.station.lat, data.station.lng));
    final accessLog = AccessCacheManager.get(data.station.id);
    final accessed = accessLog != null && accessLog.accessed;
    parent.removeOverlay();

    if (!context.mounted) return;

    await showFlexibleBottomSheet(
      context: context,
      anchors: [0, 0.5, 1],
      isSafeArea: true,
      bottomSheetBorderRadius: const BorderRadius.vertical(top: Radius.circular(25.0)),
      builder: (context, scrollController, _) {
        return ListView(
          controller: scrollController,
          children: [
            Padding(
              padding: const EdgeInsets.only(bottom: 32),
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    child: Center(
                      child: Container(
                        width: 40,
                        height: 4,
                        decoration: BoxDecoration(
                          color: Theme.of(context).dividerColor,
                          borderRadius: BorderRadius.circular(2),
                        ),
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 32),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(data.station.name, textScaler: const TextScaler.linear(1.75)),
                        const SizedBox(height: 4),
                        Text(data.station.nameKana),
                        const SizedBox(height: 12),
                        Row(children: [
                          getAttrIcon(data.station.attr, context: context),
                          const SizedBox(width: 4),
                          Expanded(child: Text(data.station.attr.name)),
                          if (distanceFromCurrent != null) Text('現在地から $distanceFromCurrent'),
                        ]),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  const Divider(height: 16),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        Expanded(child: IconButton(
                          onPressed: () {
                            if (context.mounted) Navigator.of(context).pop();
                            context.push(Uri(path: '/map', queryParameters: {'radar-id': data.station.id.toString()}).toString());
                          },
                          icon: const Padding(
                            padding: EdgeInsets.all(4),
                            child: Column(
                              spacing: 4,
                              children: [
                                Icon(Icons.radar_rounded),
                                Text('レーダー範囲'),
                              ]
                            ),
                          ),
                        )),
                        Expanded(child: IconButton(
                          onPressed: () {
                            context.push(Uri(path: '/station', queryParameters: {'id': data.station.id.toString()}).toString());
                          },
                          icon: const Padding(
                            padding: EdgeInsets.all(4),
                            child: Column(
                              spacing: 4,
                              children: [
                                Icon(Icons.pending_rounded),
                                Text('詳細'),
                              ]
                            ),
                          ),
                        )),
                        Expanded(child: IconButton(
                          onPressed: () async {
                            await AccessCacheManager.setAccessState(data.station.id, !accessed);
                            await renderVoronoi(force: true);
                            if (context.mounted) Navigator.of(context).pop();
                            parent.setOverlay(Text('${data.station.name}を${accessed ? '未アクセス' : 'アクセス済み'}にしました'));
                          },
                          icon: Padding(
                            padding: const EdgeInsets.all(4),
                            child: Column(
                              spacing: 4,
                              children: [
                                Icon(accessed ? Icons.cancel_rounded : Icons.check_circle_rounded),
                                const Text('アクセス状態切替', textAlign: TextAlign.center),
                              ],
                            ),
                          ),
                        )),
                      ],
                    ),
                  ),
                  const Divider(height: 16),
                  const SizedBox(height: 4),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: Column(
                      spacing: 8,
                      children: [
                        const Text('タップした地点からレーダーで届く駅'),
                        ListView.builder(
                          padding: EdgeInsets.zero,
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          itemCount: stations.length,
                          itemBuilder: (context, index) {
                            return _StationSimple(stations[index], index + 1);
                          },
                        ),
                      ]
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  @override
  void onMapLongClick(Point<double> point, LatLng latLng) async {
    parent.setOverlay(const Text('駅情報を取得中...'));

    final data = (await StationSearchService.getNearestStations(latLng.latitude, latLng.longitude)).first;

    if (_hideAccessState) {
      parent.setOverlay(Row(
          mainAxisAlignment: MainAxisAlignment.center,
          spacing: 4,
          children: [
            getAttrIcon(data.station.attr, context: parent.context.mounted ? parent.context : null),
            Text(data.station.name),
          ]
      ));
    } else {
      final accessLog = AccessCacheManager.get(data.station.id);
      final accessed = accessLog != null && accessLog.accessed;
      await AccessCacheManager.setAccessState(data.station.id, !accessed);
      await renderVoronoi(force: true);
      parent.setOverlay(Text('${data.station.name}を${accessed ? '未アクセス' : 'アクセス済み'}にしました'));
    }
  }

  Future<void> renderVoronoi({ int? renderingLimit, bool force = false }) async {
    renderingLimit ??= Config.mapRenderingLimit;
    final isCooldown = parent.trackingMode != MyLocationTrackingMode.none && DateTime.now().difference(_lastRectUpdate).inMilliseconds < 1000;
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
      final data = (await StationSearchService.getNearestStations(center.latitude, center.longitude)).first;
      stations.add(data.station);
    }

    if (stations.length < renderingLimit) {
      final stationList = GpsManager.isEnabled ? StationSearchService.list : null;
      controller.setGeoJsonSource('voronoi', buildVoronoi(stations, useAttrColor: attrMode, stationList: stationList));
      controller.setGeoJsonSource('point', buildPoint(stations, useAttr: attrMode, stationList: stationList));
      parent.removeOverlay();
    } else {
      parent.setOverlay(const Text('画面範囲内の駅数が多すぎるため、メッシュを描画できませんでした。地図を拡大してください。'));
    }

    _isRendering = false;
    _lastRectUpdate = DateTime.now();
  }
}

class _StationSimple extends StatelessWidget {
  final StationData data;
  final int index;

  const _StationSimple(this.data, this.index);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/station', queryParameters: {'id': data.station.id.toString()}).toString());
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Text('$index'),
              const SizedBox(width: 16),
              getAttrIcon(data.station.attr, context: context),
              const SizedBox(width: 8),
              Expanded(
                child: Text(data.station.name, textScaler: const TextScaler.linear(1.2)),
              ),
              Text(data.distance ?? '???m'),
            ],
          ),
        ),
      ),
    );
  }
}