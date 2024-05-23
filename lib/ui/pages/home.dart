import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/gps.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/updater.dart';
import 'package:ekimemo_map/services/assistant.dart';
import 'package:ekimemo_map/ui/widgets/station_card.dart';

class HomeView extends StatelessWidget {
  const HomeView({super.key});

  @override
  Widget build(BuildContext context) {
    AssetUpdater.check(silent: true, first: true);

    final station = Provider.of<StationStateNotifier>(context);
    final gps = Provider.of<GpsStateNotifier>(context);
    final state = Provider.of<SystemStateProvider>(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('駅メモマップ'),
        actions: [
          IconButton(
            onPressed: state.stationDataVersion == '' ? null : () {
              context.push('/map');
            },
            icon: const Icon(Icons.map),
          ),
          IconButton(
            onPressed: () {
              context.push('/settings');
            },
            icon: const Icon(Icons.settings),
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
                  children: [
                    Text(gps.isEnabled ? '探索 ON' : '探索 OFF'),
                    const SizedBox(width: 8),
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
      bottomNavigationBar: Container(
        color: Theme.of(context).colorScheme.surfaceContainer,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(children: [
            Expanded(child: Text('最終更新: ${station.lastUpdate}')),
            Text('${station.latestProcessingTime}ms'),
          ]),
        ),
      ),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 64),
            sliver: SliverList(
              delegate: SliverChildListDelegate.fixed(state.stationDataVersion != '' ? [
                ListView.builder(
                    padding: EdgeInsets.zero,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: station.list.length,
                    itemBuilder: (context, index) {
                      return StationCard(stationData: station.list[index], index: index);
                    }
                )
              ] : [
                SizedBox(
                  width: double.infinity,
                  child: Padding(
                    padding: const EdgeInsets.only(top: 36, bottom: 24, left: 12, right: 12),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text('駅データがありません', textScaler: TextScaler.linear(1.2)),
                        const Text('下のボタンから駅データを更新することで、利用できるようになります。'),
                        ElevatedButton(
                          onPressed: () {
                            AssetUpdater.check(force: true);
                          },
                          child: const Text('駅データを更新'),
                        ),
                      ],
                    ),
                  ),
                ),
              ]),
            ),
          ),
        ],
      ),
    );
  }
}
