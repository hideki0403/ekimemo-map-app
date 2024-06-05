import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/utils.dart';

Map<String, dynamic> buildVoronoi(List<Station> stations, { bool useAttrColor = false }) {
  final List<Map<String, dynamic>> features = [];
  for (var station in stations) {
    final voronoi = station.voronoi;
    final accessLog = AccessCacheManager.get(station.id);
    voronoi['properties'] = {
      'color': useAttrColor ? getAttrIcon(station.attr).color?.toHexStringRGB() : Colors.red.toHexStringRGB(),
      'accessed': accessLog != null && accessLog.accessed,
    };
    features.add(voronoi);
  }

  return {
    'type': 'FeatureCollection',
    'features': features,
  };
}

Map<String, dynamic> buildPoint(List<Station> stations, { bool useAttr = false }) {
  final List<Map<String, dynamic>> point = [];
  for (var station in stations) {
    final accessLog = AccessCacheManager.get(station.id);
    point.add({
      'type': 'Feature',
      'geometry': {
        'type': 'Point',
        'coordinates': [station.lng, station.lat],
      },
      'properties': {
        'name': station.name,
        'accessed': accessLog != null && accessLog.accessed,
        'color': useAttr ? getAttrIcon(station.attr).color?.toHexStringRGB() : Colors.red.toHexStringRGB(),
        'icon': useAttr ? station.attr.name : 'pin',
      },
    });
  }

  return {
    'type': 'FeatureCollection',
    'features': point,
  };
}

FillLayerProperties masterFillLayerProperties = FillLayerProperties(
  fillColor: Colors.red.toHexStringRGB(),
  fillOpacity: 0.3
);

LineLayerProperties masterLineLayerProperties = LineLayerProperties(
  lineColor: Colors.red.toHexStringRGB(),
  lineWidth: 1.0,
);

SymbolLayerProperties masterSymbolLayerProperties = SymbolLayerProperties(
  textField: [Expressions.get, 'name'],
  textHaloWidth: 2,
  textSize: 14,
  textColor: '#444',
  textHaloColor: '#ffffff',
  textFont: ['migu2m-regular'],
  textOffset: [
    Expressions.literal,
    [0, 2]
  ],
  iconImage: 'pin',
  iconSize: 0.2,
  iconColor: Colors.red.toHexStringRGB(),
);
