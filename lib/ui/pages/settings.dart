import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher_string.dart';
import 'package:ekimemo_map/services/updater.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/ui/widgets/section_title.dart';
import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final config = Provider.of<ConfigProvider>(context);
    return Scaffold(
      appBar: AppBar(title: const Text('設定')),
      body: CustomScrollView(
        slivers: [
          SliverList(
            delegate: SliverChildListDelegate([
              const SectionTitle(title: '探索設定'),
              ListTile(
                title: const Text('探索する駅数'),
                subtitle: Text('${config.maxResults}駅'),
                onTap: () async {
                  final result = await showEditorDialog(context, title: '探索する駅数', data: config.maxResults.toString(), type: EditorDialogType.integer);
                  if (result != null) {
                    config.setMaxResults(int.parse(result));
                  }
                },
              ),
              ListTile(
                title: const Text('更新頻度'),
                subtitle: Text('${config.updateFrequency}秒'),
                onTap: () async {
                  final result = await showEditorDialog(
                      context,
                      title: '更新頻度',
                      caption: '最高更新頻度を指定できます。1.0秒以上で指定してください。',
                      data: config.updateFrequency.toString(),
                      type: EditorDialogType.double
                  );

                  if (result != null) {
                    final freq = double.parse(result);
                    if (freq < 1.0) return;
                    config.setUpdateFrequency(freq);
                  }
                },
              ),
              ListTile(
                title: const Text('GPSの取得精度制限'),
                subtitle: Text(config.maxAcceptableAccuracy == 0 ? 'なし' : '${config.maxAcceptableAccuracy}m'),
                onTap: () async {
                  final result = await showEditorDialog(
                      context,
                      title: 'GPSの取得精度制限',
                      caption: 'GPSの精度がここで入力した値を超えた場合に、取得した位置情報を無視します。0mで無効になります。',
                      data: config.maxAcceptableAccuracy.toString(),
                      type: EditorDialogType.integer
                  );

                  if (result != null) {
                    config.setMaxAcceptableAccuracy(int.parse(result));
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
                      context,
                      title: 'リマインドを行う間隔',
                      caption: '秒数を指定してください',
                      data: config.cooldownTime.toString(),
                      type: EditorDialogType.integer
                  );

                  if (result != null) {
                    config.setCooldownTime(int.parse(result));
                  }
                },
              ),
              const SectionTitle(title: '駅データ'),
              ListTile(
                title: const Text('バージョン'),
                subtitle: Text(config.stationDataVersion != '' ? 'v${config.stationDataVersion}' : '不明'),
                trailing: ElevatedButton(
                  onPressed: () {
                    AssetUpdater.check(context);
                  },
                  child: const Text('更新を確認'),
                ),
              ),
              ListTile(
                title: const Text('License'),
                subtitle: const Text('"station_database" by Seo-4d696b75 is licensed under CC BY-SA 4.0'),
                onTap: () async {
                  const url = 'https://github.com/Seo-4d696b75/station_database';
                  if (await canLaunchUrlString(url)) await launchUrlString(url);
                },
              ),
              const SectionTitle(title: 'デバッグ'),
              ListTile(
                title: const Text('駅データのバージョンをリセットする'),
                onTap: () {
                  Config.setString('station_data_version', '');
                },
              )
            ]),
          ),
        ],
      ),
    );
  }
}