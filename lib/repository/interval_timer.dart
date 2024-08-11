import 'package:ekimemo_map/models/interval_timer.dart';
import '_abstract.dart';

class IntervalTimerRepository extends AbstractRepository<IntervalTimer> {
  static IntervalTimerRepository? _instance;
  IntervalTimerRepository._internal() : super(IntervalTimer(), 'interval_timer', 'id');

  factory IntervalTimerRepository() {
    _instance ??= IntervalTimerRepository._internal();
    return _instance!;
  }
}