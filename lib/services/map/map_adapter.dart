import 'dart:math';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:ekimemo_map/ui/pages/map.dart';

export 'adapter/core.dart';
export 'adapter/viewer.dart';
export 'adapter/attribute.dart';
export 'adapter/radar.dart';

abstract class MapAdapter {
  late final MaplibreMapController controller;
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

  /// AppBarのactions
  List<Widget> get appBarActions => [];

  void initialize() {}
  void onCameraIdle() {}
  void onMapClick(Point<double> point, LatLng latLng) {}
  void onMapLongClick(Point<double> point, LatLng latLng) {}

}