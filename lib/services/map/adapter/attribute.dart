import 'package:geolocator/geolocator.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import '../map_adapter.dart';
import '../utils.dart';
import 'core.dart';

class AttributeMapAdapter extends CoreMapAdapter {
  AttributeMapAdapter(super.controller);

  @override
  bool get attrMode => true;

  @override
  void initialize() async {
    await controller.addFillLayer('voronoi', 'fill', masterFillLayerProperties.copyWith(const FillLayerProperties(
      fillColor: ['get', 'color'],
    )));

    await controller.addLineLayer('voronoi', 'line', masterLineLayerProperties.copyWith(const LineLayerProperties(
      lineColor: ['get', 'color'],
    )));

    await controller.addSymbolLayer('point', 'point', masterSymbolLayerProperties.copyWith(const SymbolLayerProperties(
      iconImage: ['get', 'icon'],
      iconColor: ['get', 'color'],
    )));

    final location = await Geolocator.getCurrentPosition();
    controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
      target: LatLng(location.latitude, location.longitude),
      zoom: 12.0,
    )));
  }
}