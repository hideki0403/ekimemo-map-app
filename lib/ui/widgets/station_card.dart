import 'dart:async';
import 'package:flutter/material.dart';

import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:go_router/go_router.dart';

class StationCard extends StatelessWidget {
  final StationData stationData;
  final int index;
  const StationCard({required this.stationData, required this.index, super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/station', queryParameters: {'id': stationData.station.id}).toString());
        },
        onLongPress: () {
          // TODO: アクセス済みの駅にできるなどの機能を追加
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Text('${index + 1}'),
                  const SizedBox(height: 4),
                  getAttrIcon(stationData.station.attr),
                ],
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(stationData.station.name, textScaler: const TextScaler.linear(1.2), overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 4),
                    Opacity(opacity: 0.8, child: Text(stationData.lineName ?? '', overflow: TextOverflow.ellipsis)),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(stationData.distance ?? '', textScaler: const TextScaler.linear(1.2)),
                  Opacity(opacity: 0.8, child: _CooldownTimer(stationData: stationData, index: index)),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// timer部分をStatefulWidgetにする
class _CooldownTimer extends StatefulWidget {
  final StationData stationData;
  final int index;

  const _CooldownTimer({required this.stationData, required this.index});

  @override
  _CooldownTimerState createState() => _CooldownTimerState();
}

class _CooldownTimerState extends State<_CooldownTimer> {
  int _coolDown = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    rebuildTimer();
  }

  @override
  void didUpdateWidget(covariant _CooldownTimer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.stationData.station.id != widget.stationData.station.id) {
      _timer?.cancel();
      rebuildTimer();
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void rebuildTimer() {
    _coolDown = getCoolDownTime(widget.stationData.station.id);
    if (_coolDown == 0) return;

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        _coolDown--;
      });

      if (_coolDown == 0) {
        if (!Config.enableReminder || widget.index != 0) return timer.cancel();
        _coolDown = Config.cooldownTime;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return _coolDown != 0 ? Text(beautifySeconds(_coolDown)) : const SizedBox();
  }
}