import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/gps.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/updater.dart';
import 'package:ekimemo_map/services/assistant.dart';
import 'package:ekimemo_map/ui/widgets/station_card.dart';
import 'package:ekimemo_map/ui/widgets/relative_time.dart';
import 'package:ekimemo_map/ui/widgets/scrollview_template.dart';

class HomeView extends StatelessWidget {
  const HomeView({super.key});

  @override
  Widget build(BuildContext context) {
    final station = Provider.of<StationStateNotifier>(context);
    final gps = Provider.of<GpsStateNotifier>(context);
    final state = Provider.of<SystemStateProvider>(context);
    final updater = Provider.of<UpdateStateNotifier>(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('駅メモマップ'),
        actions: [
          if (updater.hasUpdate) IconButton(
            onPressed: () {
              UpdateManager.updateAppOrStationSource();
            },
            icon: Badge(
              child: const Icon(Icons.system_update_rounded),
            ),
            color: Theme.of(context).colorScheme.primary,
          ),
          IconButton(
            onPressed: () {
              context.push('/tools');
            },
            icon: const Icon(Icons.widgets_rounded),
          ),
          IconButton(
            onPressed: state.stationDataVersion == '' ? null : () {
              context.push('/map');
            },
            icon: const Icon(Icons.map_rounded),
          ),
          IconButton(
            onPressed: () {
              context.push('/settings');
            },
            icon: const Icon(Icons.settings_rounded),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Padding(
            padding: const EdgeInsets.only(left: 16, right: 16, top: 8, bottom: 8),
            child: Row(
              children: [
                Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('精度: ${gps.lastLocation?.accuracy != null ? '${gps.lastLocation!.accuracy.toStringAsFixed(1)}m' : '不明'}'),
                        Text('速度: ${gps.lastLocation?.speed != null ? '${(gps.lastLocation!.speed * 3.6).toStringAsFixed(1)}km/h' : '不明'}'),
                      ],
                    )
                ),
                Row(
                  spacing: 8,
                  children: [
                    Text(gps.isEnabled ? '探索 ON' : '探索 OFF'),
                    Switch(
                      value: gps.isEnabled,
                      onChanged: state.stationDataVersion == '' ? null : (value) {
                        AssistantFlow.init();
                        GpsManager.setGpsEnabled(value);
                        if (!value) {
                          station.cleanup();
                        }
                      },
                    ),
                  ],
                ),
              ],
            ),
          )
        ),
      ),
      bottomNavigationBar: !gps.isEnabled ? null : Container(
        color: Theme.of(context).colorScheme.surfaceContainer,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(children: [
            Expanded(child: RelativeTime(time: station.lastUpdateDate, prefix: '最終更新: ')),
            Opacity(opacity: 0.7, child: Text('${station.latestProcessingTime}ms', textScaler: const TextScaler.linear(0.8))),
          ]),
        ),
      ),
      body: ScrollViewTemplate(
        delegate: SliverChildBuilderDelegate((BuildContext context, int index) {
          return StationCard(
            key: ValueKey(station.list[index].station.id),
            stationData: station.list[index],
            index: index,
          );
        }, childCount: station.list.length),
        empty: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          spacing: 16,
          children: state.stationDataVersion == '' ? [
            const Text('駅データがありません', textScaler: TextScaler.linear(1.2)),
            const Text('下のボタンから駅データを更新することで、利用できるようになります。'),
            ElevatedButton(
              onPressed: () {
                UpdateManager.updateStationSource();
              },
              child: const Text('駅データを更新'),
            ),
          ] : [
            const Text('探索がOFFになっています', textScaler: TextScaler.linear(1.2)),
            const Text('右上のスイッチで探索をはじめることができます。'),
          ],
        ),
        isEmpty: state.stationDataVersion == '' || station.list.isEmpty,
      ),
    );
  }
}
