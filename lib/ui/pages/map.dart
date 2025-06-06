import 'dart:async';
import 'dart:math';
import 'package:geolocator/geolocator.dart';
import 'package:provider/provider.dart';
import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:maplibre_gl/maplibre_gl.dart';

import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/log.dart';
import 'package:ekimemo_map/services/map/map_adapter.dart';
import 'package:ekimemo_map/services/map/utils.dart' as map_utils;

final logger = Logger('MapView');

typedef MapAdapterBuilder<T extends MapAdapter> = T Function(MapViewState parent);

enum MapAdapterType { core, viewer, attribute, radar, movementLog }
final mapAdapters = <MapAdapterType, MapAdapterBuilder>{
  MapAdapterType.core: (parent) => CoreMapAdapter(parent),
  MapAdapterType.viewer: (parent) => ViewerMapAdapter(parent),
  MapAdapterType.attribute: (parent) => AttributeMapAdapter(parent),
  MapAdapterType.radar: (parent) => RadarMapAdapter(parent),
  MapAdapterType.movementLog: (parent) => MovementLogMapAdapter(parent),
};

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
  final int? stationId;
  final int? lineId;
  final int? radarId;
  final List<String>? sessionIds;

  const MapView({this.stationId, this.lineId, this.radarId, this.sessionIds, super.key});

  @override
  State<StatefulWidget> createState() => MapViewState();
}

class MapViewState extends State<MapView> {
  final _mapReadyCompleter = Completer<MapLibreMapController>();
  MyLocationTrackingMode _trackingMode = MyLocationTrackingMode.none;
  Widget? _overlayWidget;

  MyLocationTrackingMode get trackingMode => _trackingMode;
  MapLibreMapController? controller;

  // #region MapAdapter
  MapAdapter? _adapter;
  List<Widget> _adapterFloatingWidgets = [];
  List<Widget> _adapterBottomWidgets = [];

  Future<void> useAdapter(MapAdapterType type) async {
    logger.info('Loading adapter: $type');

    if (controller == null) {
      logger.info('Controller is not ready. Waiting...');
      controller = await _mapReadyCompleter.future;
    }

    final adapter = mapAdapters[type]!(this);

    // 初期化
    final layerIds = ['fill', 'point', 'line'];
    for (final layerId in layerIds) {
      await controller!.removeLayer(layerId);
    }

    if (!context.mounted) return;
    setState(() {
      _adapter = adapter;
    });

    rebuildWidget();
    adapter.initialize();
    logger.info('Adapter loaded: $type');
  }

  void rebuildWidget() {
    logger.debug('Rebuilding widget');
    setState(() {
      _adapterFloatingWidgets = _adapter?.floatingWidgets ?? [];
      _adapterBottomWidgets = _adapter?.bottomWidgets ?? [];
    });
  }
  // #endregion MapAdapter

  void showLoading([String? message]) {
    setOverlay(Row(
      spacing: 32,
      children: [
        const SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(),
        ),
        Text(message ?? '計算中...', style: const TextStyle(fontSize: 18)),
      ],
    ));
  }

  void setOverlay(Widget widget) {
    setState(() {
      _overlayWidget = widget;
    });
  }

  void removeOverlay() {
    setState(() {
      _overlayWidget = null;
    });
  }

  @override
  void dispose() {
    _adapter?.onDispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final config = Provider.of<ConfigProvider>(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(_adapter?.title ?? 'マップ'),
        actions: _adapter?.appBarActions,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          if (!context.mounted) return;
          setState(() {
            _trackingMode = _trackingMode == MyLocationTrackingMode.none ? MyLocationTrackingMode.tracking : MyLocationTrackingMode.none;
          });
        },
        foregroundColor: _trackingMode != MyLocationTrackingMode.none ? Theme.of(context).colorScheme.primary : null,
        child: Icon(_trackingMode != MyLocationTrackingMode.none ? Icons.gps_fixed_rounded : Icons.gps_not_fixed_rounded),
      ),
      bottomNavigationBar: _adapterBottomWidgets.isEmpty ? null : Container(
        color: Theme.of(context).colorScheme.surfaceContainer,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: _adapterBottomWidgets,
          ),
        ),
      ),
      body: SafeArea(
        child: Stack(children: [
          MapLibreMap(
            initialCameraPosition: const CameraPosition(target: LatLng(35.681236, 139.767125), zoom: 10.0),
            trackCameraPosition: true,
            onMapCreated: (controller) {
              _mapReadyCompleter.complete(controller);
            },
            onStyleLoadedCallback: () async {
              final controller = await _mapReadyCompleter.future;

              // fallback of onMapClick
              controller.onFeatureTapped.add((dynamic _, Point<double> point, LatLng latLng, String layerId) => _adapter?.onMapClick(point, latLng));

              // add resources
              await controller.addImage('pin', (await rootBundle.load('assets/icon/location.png')).buffer.asUint8List(), true);
              await controller.addImage('heat', (await rootBundle.load('assets/icon/heat.png')).buffer.asUint8List(), true);
              await controller.addImage('cool', (await rootBundle.load('assets/icon/cool.png')).buffer.asUint8List(), true);
              await controller.addImage('eco', (await rootBundle.load('assets/icon/eco.png')).buffer.asUint8List(), true);
              await controller.addImage('unknown', (await rootBundle.load('assets/icon/unknown.png')).buffer.asUint8List(), true);

              // add sources
              await controller.addGeoJsonSource('voronoi', map_utils.buildVoronoi([]));
              await controller.addGeoJsonSource('point', map_utils.buildPoint([]));

              if (widget.stationId != null || widget.lineId != null) {
                useAdapter(MapAdapterType.viewer);
              } else if(widget.radarId != null) {
                useAdapter(MapAdapterType.radar);
              } else if(widget.sessionIds != null) {
                useAdapter(MapAdapterType.movementLog);
              } else {
                await useAdapter(MapAdapterType.core);
                final location = await Geolocator.getCurrentPosition();
                controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
                  target: LatLng(location.latitude, location.longitude),
                  zoom: 12.0,
                )));
              }
            },
            onCameraIdle: () => _adapter?.onCameraIdle(),
            onCameraTrackingDismissed: () {
              setState(() {
                _trackingMode = MyLocationTrackingMode.none;
              });
            },
            onMapClick: (point, latLng) => _adapter?.onMapClick(point, latLng),
            onMapLongClick: (point, latLng) => _adapter?.onMapLongClick(point, latLng),
            styleString: config.mapStyle.toString(),
            myLocationEnabled: true,
            myLocationTrackingMode: _trackingMode,
          ),
          Positioned(
            bottom: 0,
            left: 0,
            child: Container(
              padding: const EdgeInsets.all(8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: _adapterFloatingWidgets,
              )
            ),
          ),
          Positioned(
            top: 32,
            left: 32,
            right: 32,
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 100),
              transitionBuilder: (child, animation) {
                final scaleTween = Tween<double>(begin: 0.95, end: 1.0);
                final curvedAnimation = CurvedAnimation(
                  parent: animation,
                  curve: Curves.easeInOut,
                );

                return FadeTransition(
                  opacity: curvedAnimation,
                  child: ScaleTransition(
                    scale: curvedAnimation.drive(scaleTween),
                    child: child,
                  ),
                );
              },
              child: _overlayWidget == null ? null : Container(
                padding: const EdgeInsets.all(16),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: Theme.of(context).scaffoldBackgroundColor,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
                      spreadRadius: 4,
                      blurRadius: 8,
                      offset: const Offset(0, 0),
                    ),
                  ],
                ),
                child: _overlayWidget,
              ),
            ),
          ),
        ]),
      )
    );
  }
}
