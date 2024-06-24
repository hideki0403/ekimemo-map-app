import 'package:ekimemo_map/models/station.dart';
import '_abstract.dart';

class StationRepository extends AbstractRepository<Station> {
  StationRepository() : super(Station(), 'station', 'id');
}