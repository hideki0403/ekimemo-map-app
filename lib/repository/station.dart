import 'package:ekimemo_map/models/station.dart';
import '_abstract.dart';

class StationRepository extends AbstractRepository<Station> {
  static StationRepository? _instance;
  StationRepository._internal() : super(Station(), 'station', 'id');

  factory StationRepository() {
    _instance ??= StationRepository._internal();
    return _instance!;
  }
}