import 'dart:async';
import 'package:flutter/material.dart';

import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/notification.dart';
import 'package:go_router/go_router.dart';

class StationSimple extends StatelessWidget {
  final StationData stationData;
  final int index;
  const StationSimple({required this.stationData, required this.index, Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/station', queryParameters: {'id': stationData.station.code.toString()}).toString());
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
                  getAttrIcon(stationData.station.attr),
                ],
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(stationData.station.name, textScaler: const TextScaler.linear(1.2)),
                    const SizedBox(height: 4),
                    Opacity(opacity: 0.8, child: Text(stationData.lineName ?? '')),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(stationData.distance ?? '', textScaler: const TextScaler.linear(1.2)),
                  if (!stationData.isNew) Opacity(opacity: 0.8, child: _CooldownTimer(stationData: stationData, index: index)),
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

  const _CooldownTimer({Key? key, required this.stationData, required this.index}) : super(key: key);

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
    _coolDown = getCoolDownTime(widget.stationData);
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