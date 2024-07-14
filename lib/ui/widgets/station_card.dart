import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';

class StationCard extends StatelessWidget {
  final StationData stationData;
  final int index;
  final bool viewOnly;
  final _cooldownTimerState = GlobalKey<_CooldownTimerState>();
  StationCard({required this.stationData, required this.index, this.viewOnly = false, super.key});

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
        onLongPress: viewOnly ? null : () async {
          final rebuild = await showDialog(context: context, builder: (context) => _StationMenu(station: stationData.station, index: index)) as bool?;
          if (rebuild == true) {
            _cooldownTimerState.currentState?.rebuildTimer();
          }
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
                  getAttrIcon(stationData.station.attr, context: context),
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
              if (!viewOnly) Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(stationData.distance ?? '', textScaler: const TextScaler.linear(1.2)),
                  Opacity(opacity: 0.8, child: _CooldownTimer(stationData: stationData, index: index, key: _cooldownTimerState)),
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

  const _CooldownTimer({required this.stationData, required this.index, super.key});

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
    setState(() {
      _coolDown = getCoolDownTime(widget.stationData.station.id);
    });

    _timer?.cancel();
    if (_coolDown == 0) return;

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!context.mounted) return timer.cancel();
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

// 駅部分を長押しした時に表示されるメニュー
class _StationMenu extends StatelessWidget {
  const _StationMenu({required this.station, required this.index});
  final Station station;
  final int index;

  @override
  Widget build(BuildContext context) {
    final cooldown = getCoolDownTime(station.id);
    final accessLog = AccessCacheManager.get(station.id);
    final isAccessed = accessLog != null && accessLog.accessed;

    return AlertDialog(
      title: Text(station.name),
      contentPadding: const EdgeInsets.fromLTRB(0, 16, 0, 24),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (index != 0) ...[
            ListTile(
              title: Text(isAccessed ? '未アクセスにする' : 'アクセス済みにする'),
              leading: Icon(isAccessed ? Icons.cancel : Icons.check_circle),
              onTap: () async {
                await AccessCacheManager.update(station.id, DateTime.now(), accessed: !isAccessed);
                if (context.mounted) Navigator.of(context).pop(true);
              },
            ),
            if (Config.enableReminder) ListTile(
              title: Text(cooldown == 0 ? 'タイマーをセットする' : 'タイマーをリセットする'),
              leading: Icon(cooldown == 0 ? Icons.timer : Icons.timer_off),
              onTap: () async {
                final time = cooldown == 0 ? DateTime.now() : DateTime.now().subtract(Duration(seconds: cooldown));
                await AccessCacheManager.update(station.id, time, updateOnly: true);
                if (context.mounted) Navigator.of(context).pop(true);
              },
            ),
          ],
          ListTile(
            title: const Text('この駅のレーダー範囲を見る'),
            leading: const Icon(Icons.radar),
            onTap: () async {
              if (context.mounted) Navigator.of(context).pop(true);
              await context.push(Uri(path: '/map', queryParameters: {'radar-id': station.id}).toString());
            },
          ),
        ],
      ),
    );
  }
}
