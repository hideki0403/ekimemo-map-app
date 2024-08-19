import 'package:flutter/material.dart';
import 'package:expandable/expandable.dart';
import 'package:ekimemo_map/ui/widgets/blink.dart';
import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';
import 'package:ekimemo_map/services/interval_timer.dart';
import 'package:ekimemo_map/services/notification.dart';
import 'package:ekimemo_map/services/utils.dart';

class IntervalTimerView extends StatefulWidget {
  const IntervalTimerView({super.key});

  @override
  State<IntervalTimerView> createState() => _IntervalTimerViewState();
}

class _IntervalTimerViewState extends State<IntervalTimerView> {
  final _timers = <IntervalTimerHandler>[];

  @override
  void initState() {
    super.initState();
    _loadTimers();
  }

  Future<void> _loadTimers() async {
    final timers = await IntervalTimerManager.getTimers();
    setState(() {
      _timers.clear();
      _timers.addAll(timers);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('インターバルタイマー'),
        actions: [
          IconButton(
            icon: const Icon(Icons.help),
            onPressed: () {
              showMessageDialog(
                title: 'インターバルタイマーについて',
                message: '''
                  指定した時間おきに通知を行うことができる機能です。
                  タイマーは右下の「＋」ボタンから追加できます。
                  表示されている時間をタップすることで、通知を行う間隔を変更可能です。
                  また、タイトル(タイマー名)をタップすることでタイトルを編集することができます。
                '''.replaceAll(RegExp(r' {2,}'), ''),
              );
            },
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        child: const Icon(Icons.add),
        onPressed: () async {
          await IntervalTimerManager.addTimer();
          await _loadTimers();
        }
      ),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.all(16),
            sliver: SliverList(
              delegate: SliverChildListDelegate.fixed(_timers.isNotEmpty ? [
                ListView.separated(
                  padding: EdgeInsets.zero,
                  shrinkWrap: true,
                  reverse: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _timers.length,
                  itemBuilder: (context, index) {
                    return IntervalTimerItem(
                      key: ValueKey(_timers[index].timer.id),
                      item: _timers[index],
                      deleteCallback: _loadTimers,
                    );
                  },
                  separatorBuilder: (BuildContext context, int index) {
                    return const SizedBox(height: 12);
                  },
                )
              ] : [
                const SizedBox(
                  width: double.infinity,
                  child: Padding(
                    padding: EdgeInsets.only(top: 36, bottom: 24, left: 12, right: 12),
                    child: Center(
                      child: Text('インターバルタイマーがありません'),
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

class IntervalTimerItem extends StatefulWidget {
  final IntervalTimerHandler item;
  final Function deleteCallback;
  const IntervalTimerItem({required this.item, required this.deleteCallback, super.key});

  @override
  State<IntervalTimerItem> createState() => _IntervalTimerItemState();
}

class _IntervalTimerItemState extends State<IntervalTimerItem> {
  final ExpandableController _expandableController = ExpandableController();
  bool _isExpanded = false;

  String _name = '';
  int _remainingTime = 0;
  bool _isPaused = false;
  bool _isStopped = true;

  @override
  void initState() {
    super.initState();
    widget.item.onRemainingUpdated(() {
      setState(() {
        _remainingTime = widget.item.remaining;
      });
    });
    widget.item.onTimerStateChanged(() {
      setState(() {
        _isPaused = widget.item.paused;
        _isStopped = widget.item.stopped;
      });
    });

    widget.item.refresh();

    setState(() {
      _name = widget.item.timer.name;
    });
  }

  @override
  void dispose() {
    widget.item.disposeCallback();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          LinearProgressIndicator(
            value: _remainingTime / widget.item.timer.duration.inSeconds,
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(child: GestureDetector(
                      onTap: () async {
                        final name = await showEditorDialog(
                          title: 'タイマー名',
                          data: widget.item.timer.name,
                        );
                        if (name != null) {
                          widget.item.setName(name);
                          setState(() {
                            _name = widget.item.timer.name;
                          });
                        }
                      },
                      child: Text(_name, textScaler: const TextScaler.linear(1.1)),
                    )),
                    IconButton(
                      icon: Icon(_isExpanded ? Icons.expand_less : Icons.expand_more),
                      onPressed: () {
                        _expandableController.toggle();
                        setState(() {
                          _isExpanded = !_isExpanded;
                        });
                      },
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Blink(enabled: _isPaused && !_isStopped, child: GestureDetector(
                  onTap: () async {
                    final remaining = await showEditorDialog(
                      title: '残り時間',
                      caption: '秒数を入力してください',
                      suffix: '秒',
                      data: widget.item.timer.duration.inSeconds.toString(),
                      type: EditorDialogType.integer,
                    );

                    if (remaining != null) {
                      widget.item.setDuration(Duration(seconds: int.parse(remaining)));
                    }
                  },
                  child: Text(beautifySeconds(_remainingTime), textScaler: const TextScaler.linear(2)),
                )),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: Wrap(
                    alignment: WrapAlignment.end,
                    spacing: 12,
                    children: [
                      if (_isStopped || _isPaused) ...[
                        FilledButton(
                          onPressed: () {
                            widget.item.start();
                          },
                          child: const Icon(Icons.play_arrow),
                        ),
                      ] else ...[
                        FilledButton(
                          onPressed: () {
                            widget.item.pause();
                          },
                          child: const Icon(Icons.pause),
                        ),
                      ],
                      FilledButton.tonal(
                        onPressed: () {
                          widget.item.stop();
                        },
                        child: const Icon(Icons.stop),
                      ),
                      FilledButton.tonal(
                        onPressed: _isStopped ? null : () {
                          widget.item.reset();
                        },
                        child: const Icon(Icons.restart_alt),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          ExpandableNotifier(
            controller: _expandableController,
            child: Expandable(
              collapsed: const SizedBox(
                width: double.infinity,
              ),
              expanded: Column(
                children: [
                  const Divider(),
                  SwitchListTile(
                    title: const Text('通知'),
                    value: widget.item.timer.enableNotification,
                    onChanged: (value) {
                      widget.item.setEnableNotification(value);
                      setState(() {});
                    },
                  ),
                  SwitchListTile(
                    title: const Text('読み上げ'),
                    value: widget.item.timer.enableTts,
                    onChanged: (value) {
                      widget.item.setEnableTts(value);
                      setState(() {});
                    },
                  ),
                  ListTile(
                    title: const Text('通知音'),
                    subtitle: Text(widget.item.timer.notificationSound?.displayName ?? 'OFF'),
                    onTap: () async {
                      final (isCanceled, result) = await notificationSoundSelector(widget.item.timer.notificationSound?.name, withNone: true);
                      if (!isCanceled) {
                        widget.item.setNotificationSound(result);
                        setState(() {});
                      }
                    },
                  ),
                  ListTile(
                    title: const Text('バイブレーション'),
                    subtitle: Text(widget.item.timer.vibrationPattern?.displayName ?? 'OFF'),
                    onTap: () async {
                      final (isCanceled, result) = await vibrationPatternSelector(widget.item.timer.vibrationPattern?.name, withNone: true);
                      if (!isCanceled) {
                        widget.item.setVibrationPattern(result);
                        setState(() {});
                      }
                    },
                  ),
                  ListTile(
                    title: const Text('削除'),
                    textColor: Colors.red,
                    onTap: () async {
                      final isDeleted = await showConfirmDialog(
                        title: '削除',
                        caption: 'このタイマーを削除しますか？',
                      );
                      if (isDeleted == true) {
                        await IntervalTimerManager.removeTimer(widget.item);
                        widget.deleteCallback();
                      }
                    },
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
          ),
        ],
      )
    );
  }
}


