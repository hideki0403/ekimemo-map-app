import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher_string.dart';
import 'package:package_info_plus/package_info_plus.dart';

import 'package:ekimemo_map/services/updater.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/database.dart';
import 'package:ekimemo_map/services/native.dart';
import 'package:ekimemo_map/services/notification.dart';
import 'package:ekimemo_map/services/assistant.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';
import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';

class SettingsView extends StatefulWidget {
  const SettingsView({super.key});

  @override
  State<StatefulWidget> createState() => _SettingsViewState();
}

class _SettingsViewState extends State<SettingsView> {
  String _version = '';
  String _buildNumber = '';
  String _commitHash = '';
  bool _hasPermission = false;
  bool _isDebug = kDebugMode;

  Future<void> _fetchAppInfo() async {
    final packageInfo = await PackageInfo.fromPlatform();
    final commitHash = await NativeMethods.getCommitHash();
    final hasPermission = await NativeMethods.hasPermission();
    setState(() {
      _version = packageInfo.version;
      _buildNumber = packageInfo.buildNumber;
      _commitHash = commitHash;
      _hasPermission = hasPermission;
    });
  }

  @override
  void initState() {
    super.initState();
    _fetchAppInfo();
  }

  @override
  Widget build(BuildContext context) {
    final config = Provider.of<ConfigProvider>(context);
    final state = Provider.of<SystemStateProvider>(context);
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar.large(
            title: const Text('設定'),
            actions: [
              // More Vert Icon
              PopupMenuButton(
                itemBuilder: (BuildContext context) {
                  return [
                    const PopupMenuItem(
                      value: 'enableDebug',
                      child: Text('デバッグ項目を有効化'),
                    ),
                  ];
                },
                onSelected: (String s) {
                  if (s == 'enableDebug') {
                    setState(() {
                      _isDebug = true;
                    });
                  }
                },
              )
            ],
          ),
          SliverList(
            delegate: SliverChildListDelegate([
              const SectionTitle(title: '探索設定'),
              ListTile(
                title: const Text('探索する駅数'),
                subtitle: Text('${config.maxResults}駅'),
                onTap: () async {
                  final result = await showEditorDialog(
                      title: '探索する駅数',
                      caption: '探索する最大駅数を指定できます。1以上で指定してください。',
                      data: config.maxResults.toString(),
                      suffix: '駅',
                      type: EditorDialogType.integer
                  );

                  if (result != null) {
                    config.setMaxResults(max(1, int.parse(result)));
                  }
                },
              ),
              ListTile(
                title: const Text('更新頻度'),
                subtitle: Text('${config.updateFrequency}秒'),
                onTap: () async {
                  final result = await showEditorDialog(
                      title: '更新頻度',
                      caption: '最高更新頻度を指定できます。1.0秒以上で指定してください。',
                      data: config.updateFrequency.toString(),
                      suffix: '秒',
                      type: EditorDialogType.double
                  );

                  if (result != null) {
                    config.setUpdateFrequency(max(1.0, double.parse(result)));
                  }
                },
              ),
              ListTile(
                title: const Text('GPSの取得精度制限'),
                subtitle: Text(config.maxAcceptableAccuracy == 0 ? 'なし' : '${config.maxAcceptableAccuracy}m'),
                onTap: () async {
                  final result = await showEditorDialog(
                      title: 'GPSの取得精度制限',
                      caption: 'GPSの精度がここで入力した値を超えた場合に、取得した位置情報を無視します。0mで無効になります。',
                      data: config.maxAcceptableAccuracy.toString(),
                      suffix: 'm',
                      type: EditorDialogType.integer
                  );

                  if (result != null) {
                    config.setMaxAcceptableAccuracy(max(0, int.parse(result)));
                  }
                },
              ),
              const SectionTitle(title: '通知設定'),
              SwitchListTile(
                title: const Text('通知を有効にする'),
                subtitle: const Text('最寄り駅が変更された場合などに通知を送信します。'),
                value: config.enableNotification,
                onChanged: (value) {
                  config.setEnableNotification(value);
                },
              ),
              SwitchListTile(
                title: const Text('リマインダーを有効にする'),
                subtitle: const Text('同じ駅に一定時間居た場合に、再度通知を送信します。'),
                value: config.enableReminder,
                onChanged: (value) {
                  config.setEnableReminder(value);
                },
              ),
              ListTile(
                title: const Text('リマインドを行う間隔'),
                subtitle: Text(beautifySeconds(config.cooldownTime, jp: true)),
                onTap: () async {
                  final result = await showEditorDialog(
                      title: 'リマインドを行う間隔',
                      caption: '秒数を指定してください',
                      data: config.cooldownTime.toString(),
                      suffix: '秒',
                      type: EditorDialogType.integer
                  );

                  if (result != null) {
                    config.setCooldownTime(max(10, int.parse(result)));
                  }
                },
              ),
              SwitchListTile(
                title: const Text('クールダウン中でも通知を送信する'),
                subtitle: const Text('次のリマインド通知が送信されるまでの間 (クールダウン中) でも通知を送信します。'),
                value: config.enableNotificationDuringCooldown,
                onChanged: (value) {
                  config.setEnableNotificationDuringCooldown(value);
                },
              ),
              ListTile(
                title: const Text('通知音'),
                subtitle: Text(config.notificationSound.displayName),
                onTap: () async {
                  final result = await showSelectDialog(
                      title: '通知音',
                      data: Map.fromEntries(NotificationSound.values.map((e) => MapEntry(e.name, e.displayName))),
                      defaultValue: config.notificationSound.name,
                  );

                  if (result != null) {
                    // TODO: 通知音選択時に鳴らすように
                    config.setNotificationSound(NotificationSound.values.byName(result));
                  }
                },
              ),
              ListTile(
                title: const Text('通知音量'),
                subtitle: Text('${config.notificationSoundVolume}%'),
                onTap: () async {
                  final result = await showEditorDialog(
                      title: '通知音量',
                      data: config.notificationSoundVolume.toString(),
                      suffix: '%',
                      type: EditorDialogType.integer
                  );

                  if (result != null) {
                    config.setNotificationSoundVolume(max(0, min(100, int.parse(result))));
                  }
                },
              ),
              SwitchListTile(
                title: const Text('バイブレーション'),
                subtitle: const Text('通知時にバイブレーションでお知らせします。'),
                value: config.enableVibration,
                onChanged: (value) {
                  config.setEnableVibration(value);
                },
              ),
              ListTile(
                title: const Text('バイブレーションパターン'),
                subtitle: Text(config.vibrationPattern.displayName),
                onTap: () async {
                  final result = await showSelectDialog(
                      title: 'バイブレーションパターン',
                      data: Map.fromEntries(VibrationPattern.values.map((e) => MapEntry(e.name, e.displayName))),
                      defaultValue: config.vibrationPattern.name,
                  );

                  if (result != null) {
                    // TODO: パターン選択時に動かすように
                    config.setVibrationPattern(VibrationPattern.values.byName(result));
                  }
                },
              ),
              const SectionTitle(title: '駅データ'),
              ListTile(
                title: const Text('バージョン'),
                subtitle: Text(state.stationDataVersion != '' ? state.stationDataVersion : '不明'),
                trailing: ElevatedButton(
                  onPressed: () {
                    AssetUpdater.check();
                  },
                  child: const Text('更新を確認'),
                ),
              ),
              ListTile(
                title: const Text('License'),
                subtitle: const Text('"station_database" by Seo-4d696b75, Licensed under CC BY 4.0'),
                onTap: () async {
                  const url = 'https://github.com/Seo-4d696b75/station_database';
                  if (await canLaunchUrlString(url)) await launchUrlString(url);
                },
              ),
              const SectionTitle(title: 'アプリ'),
              ListTile(
                title: const Text('バージョン'),
                subtitle: Text('v$_version'),
                trailing: ElevatedButton(
                  onPressed: () {
                    AppUpdater.check();
                  },
                  child: const Text('更新を確認'),
                ),
              ),
              if (_isDebug) ...[
                const SectionTitle(title: 'デバッグ'),
                ListTile(
                  title: const Text('駅データのバージョンをリセットする'),
                  onTap: () {
                    state.setStationDataVersion('');
                  },
                ),
                ListTile(
                  title: const Text('データベースを消し飛ばす'),
                  onTap: () async {
                    final result = await showConfirmDialog(
                        title: 'データベースのリセット',
                        caption: '本当にデータベースを吹き飛ばしますか？'
                    );

                    if (result == true) {
                      DatabaseHandler.reset();
                    }
                  },
                ),
                ListTile(
                  title: const Text('Tree node root'),
                  subtitle: Text(state.treeNodeRoot),
                ),
                ListTile(
                  title: const Text('App commit hash'),
                  subtitle: Text(_commitHash),
                ),
                ListTile(
                  title: const Text('App build number'),
                  subtitle: Text(_buildNumber),
                ),
                if (_hasPermission) ...[
                  const SectionTitle(title: 'アシスタント (デバッグ用)'),
                  ListTile(
                    title: const Text('デバッグ対象パッケージ名'),
                    subtitle: Text(state.debugPackageName),
                    onTap: () async {
                      final result = await showEditorDialog(
                          title: 'パッケージ名',
                          data: state.debugPackageName,
                          type: EditorDialogType.text
                      );

                      if (result != null) {
                        state.setDebugPackageName(result);
                        AssistantFlow.init();
                      }
                    },
                  ),
                  ListTile(
                    title: const Text('Assistant Flow Editor'),
                    onTap: () {
                      context.push('/assistant-flow');
                    },
                  )
                ],
                // End of _hasPermission
              ],
              // End of _isDebug
            ]),
          ),
        ],
      ),
    );
  }
}