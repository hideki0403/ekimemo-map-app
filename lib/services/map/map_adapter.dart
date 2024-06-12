import 'dart:math';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:ekimemo_map/ui/pages/map.dart';

export 'adapter/core.dart';
export 'adapter/viewer.dart';
export 'adapter/attribute.dart';
export 'adapter/radar.dart';

abstract class MapAdapter {
  late final MapLibreMapController controller;
  late final MapViewState parent;
  late final BuildContext context;

  MapAdapter(this.parent) {
    context = parent.context;
    controller = parent.controller!;
  }

  /// nullでなければページタイトルを上書きする
  String? get title => null;

  /// マップ左下に表示するウィジェット
  List<Widget> get floatingWidgets => [];

  /// マップの下部に表示するウィジェット
  /// (アダプタ固有のコントローラー等を表示する場合等に使用する)
  List<Widget> get bottomWidgets => [];

  /// AppBarのactions
  List<Widget> get appBarActions => [];

  void initialize() {}
  void onDispose() {}
  void onCameraIdle() {}
  void onMapClick(Point<double> point, LatLng latLng) {}
  void onMapLongClick(Point<double> point, LatLng latLng) {}

}