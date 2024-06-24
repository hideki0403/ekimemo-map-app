import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';

import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/cache.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/passing_log.dart';
import 'package:ekimemo_map/repository/passing_log.dart';

class HistoryView extends StatefulWidget {
  const HistoryView({super.key});

  @override
  State<HistoryView> createState() => HistoryViewState();
}

class HistoryViewState extends State<HistoryView> {
  final passingLog = <PassingLog>[];

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    final logs = await PassingLogRepository().getAll();
    if (!context.mounted) return;
    setState(() {
      passingLog.clear();
      passingLog.addAll(logs);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('アクセス履歴'),
        actions: [
          IconButton(
            onPressed: () async {
              await _loadHistory();
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('最新のアクセス履歴を読み込みました'), duration: Duration(seconds: 1)));
              }
            },
            icon: const Icon(Icons.sync),
          ),
          IconButton(
            onPressed: () async {
              final result = await showYesNoDialog(
                title: 'アクセス履歴の削除',
                message: 'アクセス履歴を全て削除しますか？\nこの操作は取り消せません。',
              );

              if (result == true) {
                await PassingLogRepository().clear();
                await _loadHistory();
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('アクセス履歴を削除しました')));
                }
              }
            },
            icon: const Icon(Icons.delete_forever),
          ),
        ],
      ),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 32),
            sliver: SliverList(
              delegate: SliverChildListDelegate.fixed(passingLog.isNotEmpty ? [
                ListView.builder(
                    padding: EdgeInsets.zero,
                    shrinkWrap: true,
                    reverse: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: passingLog.length,
                    itemBuilder: (context, index) {
                      return _StationHistory(data: passingLog[index]);
                    }
                )
              ] : [
                const SizedBox(
                  width: double.infinity,
                  child: Padding(
                    padding: EdgeInsets.only(top: 36, bottom: 24, left: 12, right: 12),
                    child: Center(
                      child: Text('アクセス履歴がありません'),
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

class _StationHistory extends StatefulWidget {
  final PassingLog data;
  const _StationHistory({required this.data});

  @override
  State<_StationHistory> createState() => _StationHistoryState();
}

class _StationHistoryState extends State<_StationHistory> {
  Station? stationData;

  @override
  void initState() {
    super.initState();
    _loadStationData();
  }

  Future<void> _loadStationData() async {
    final data = await StationCache.get(widget.data.id);
    if (!context.mounted) return;
    setState(() {
      stationData = data;
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
          context.push(Uri(path: '/station', queryParameters: {'id': stationData?.id}).toString());
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: stationData == null ? [] : [
              getAttrIcon(stationData!.attr, context: context),
              const SizedBox(width: 20),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(stationData!.name, textScaler: const TextScaler.linear(1.2), overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 2),
                  Wrap(
                    direction: Axis.horizontal,
                    alignment: WrapAlignment.end,
                    spacing: 8,
                    children: [
                      Text('距離: ${beautifyDistance(widget.data.distance)}'),
                      Text('精度: ${widget.data.accuracy.toStringAsFixed(1)}m'),
                      Text('速度: ${widget.data.speed.toStringAsFixed(1)}km/h'),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Opacity(
                    opacity: 0.8,
                    child: Text(DateFormat('yyyy/MM/dd HH:mm:ss').format(widget.data.timestamp), textScaler: const TextScaler.linear(0.9)),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}