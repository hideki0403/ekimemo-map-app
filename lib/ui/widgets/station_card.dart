import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/config.dart';

class StationCard extends StatefulWidget {
  final StationData stationData;
  final int index;
  final bool viewOnly;

  const StationCard({required this.stationData, required this.index, this.viewOnly = false, super.key});

  @override
  State<StationCard> createState() => _StationCardState();
}

class _StationCardState extends State<StationCard> {
  int _coolDown = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    rebuildTimer();
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
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/station', queryParameters: {'id': widget.stationData.station.id.toString()}).toString());
        },
        onLongPress: widget.viewOnly ? null : () async {
          final rebuild = await showDialog<bool?>(context: context, builder: (context) => _StationMenu(station: widget.stationData.station, index: widget.index, lineName: widget.stationData.lineName));
          if (rebuild == true) {
            rebuildTimer();
          }
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                spacing: 4,
                children: [
                  Text('${widget.index + 1}'),
                  getAttrIcon(widget.stationData.station.attr, context: context),
                ],
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  spacing: 4,
                  children: [
                    Text(widget.stationData.station.name, textScaler: const TextScaler.linear(1.2), overflow: TextOverflow.ellipsis),
                    Opacity(opacity: 0.8, child: Text(widget.stationData.lineName ?? '', overflow: TextOverflow.ellipsis)),
                  ],
                ),
              ),
              if (!widget.viewOnly) Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(widget.stationData.distance ?? '', textScaler: const TextScaler.linear(1.2)),
                  Opacity(opacity: 0.8, child: _coolDown != 0 ? Text(beautifySeconds(_coolDown)) : const SizedBox()),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// 駅部分を長押しした時に表示されるメニュー
class _StationMenu extends StatelessWidget {
  const _StationMenu({required this.station, required this.index, this.lineName});
  final Station station;
  final String? lineName;
  final int index;

  @override
  Widget build(BuildContext context) {
    final cooldown = getCoolDownTime(station.id);
    final accessLog = AccessCacheManager.get(station.id);
    final isAccessed = accessLog != null && accessLog.accessed;

    return AlertDialog(
      title: Text(station.name),
      contentPadding: const EdgeInsets.fromLTRB(0, 8, 0, 24),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (lineName != null) ...[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Opacity(opacity: 0.8, child: Text(lineName!)),
            ),
            const SizedBox(height: 16),
          ],
          if (index != 0) ...[
            ListTile(
              title: Text(isAccessed ? '未アクセスにする' : 'アクセス済みにする'),
              leading: Icon(isAccessed ? Icons.cancel_rounded : Icons.check_circle_rounded),
              onTap: () async {
                await AccessCacheManager.setAccessState(station.id, !isAccessed);
                if (context.mounted) Navigator.of(context).pop(true);
              },
            ),
            if (Config.enableReminder) ListTile(
              title: Text(cooldown == 0 ? 'タイマーをセットする' : 'タイマーをリセットする'),
              leading: Icon(cooldown == 0 ? Icons.timer_rounded : Icons.timer_off_rounded),
              onTap: () async {
                final lastAccessedTime = AccessCacheManager.getTime(station.id) ?? DateTime.now();
                final time = cooldown == 0 ? DateTime.now() : lastAccessedTime.subtract(Duration(seconds: cooldown));
                await AccessCacheManager.update(station.id, time, updateOnly: true);
                if (context.mounted) Navigator.of(context).pop(true);
              },
            ),
          ],
          ListTile(
            title: const Text('この駅のレーダー範囲を見る'),
            leading: const Icon(Icons.radar_rounded),
            onTap: () async {
              if (context.mounted) Navigator.of(context).pop(true);
              await context.push(Uri(path: '/map', queryParameters: {'radar-id': station.id.toString()}).toString());
            },
          ),
        ],
      ),
    );
  }
}
