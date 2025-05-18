import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/services/utils.dart';
import '../map_adapter.dart';
import '../utils.dart';

final _stationRepository = StationRepository();
final _lineRepository = LineRepository();

class ViewerMapAdapter extends MapAdapter {
  ViewerMapAdapter(super.parent);

  @override
  String? get title => parent.widget.stationId != null ? 'マップ (駅情報)' : 'マップ (路線情報)';

  @override
  void initialize() async {
    await controller.addLineLayer('voronoi', 'line', masterLineLayerProperties.copyWith(const LineLayerProperties(
      lineWidth: 2.0,
    )));

    await controller.addSymbolLayer('point', 'point', masterSymbolLayerProperties);

    if (parent.widget.stationId != null) {
      await _renderSingleStation();
    } else if (parent.widget.lineId != null) {
      await _renderSingleLine();
    } else {
      throw Exception('Invalid arguments');
    }
  }

  Future<void> _renderSingleStation() async {
    final station = await _stationRepository.getOne(parent.widget.stationId!);
    if (station == null) return;

    controller.setGeoJsonSource('voronoi', buildVoronoi([station]));
    controller.setGeoJsonSource('point', buildPoint([station]));
    controller.moveCamera(CameraUpdate.newCameraPosition(CameraPosition(
      target: LatLng(station.lat, station.lng),
      zoom: 14.0,
    )));
  }

  Future<void> _renderSingleLine() async {
    final line = await _lineRepository.getOne(parent.widget.lineId);
    if (line == null || line.polylineList == null) return;

    final stations = await _stationRepository.get(line.stationList);

    controller.setGeoJsonSource('voronoi', line.polylineList as Map<String, dynamic>);
    controller.setGeoJsonSource('point', buildPoint(stations));

    final bounds = getBoundsFromLine(line);
    if (bounds != null) {
      controller.moveCamera(CameraUpdate.newLatLngBounds(bounds));
    }
  }
}