import 'dart:async';
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

enum MapAdapterType { core, viewer }
final mapAdapters = <MapAdapterType, MapAdapterBuilder>{
  MapAdapterType.core: (parent) => CoreMapAdapter(parent),
  MapAdapterType.viewer: (parent) => ViewerMapAdapter(parent),
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
  final String? stationId;
  final String? lineId;

  const MapView({this.stationId, this.lineId, super.key});

  @override
  State<StatefulWidget> createState() => MapViewState();
}

class MapViewState extends State<MapView> {
  final _mapReadyCompleter = Completer<MaplibreMapController>();
  MyLocationTrackingMode _trackingMode = MyLocationTrackingMode.None;
  Widget? _overlayWidget;

  MyLocationTrackingMode get trackingMode => _trackingMode;
  MaplibreMapController? controller;

  // #region MapAdapter
  MapAdapter? _adapter;
  List<Widget> _adapterFloatingWidgets = [];

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
    });
  }
  // #endregion MapAdapter

  void showLoading() {
    setOverlay(const Row(
      children: [
        SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(),
        ),
        SizedBox(width: 32),
        Text('計算中...', style: TextStyle(fontSize: 18)),
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
  Widget build(BuildContext context) {
    final config = Provider.of<ConfigProvider>(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(_adapter?.title ?? 'マップ'),
        actions: _adapter?.appBarActions,
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
              } else {
                useAdapter(MapAdapterType.core);
              }
            },
            onCameraIdle: () => _adapter?.onCameraIdle(),
            onCameraTrackingDismissed: () {
              setState(() {
                _trackingMode = MyLocationTrackingMode.None;
              });
            },
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
