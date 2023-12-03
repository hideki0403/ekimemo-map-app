import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:ekimemo_map/services/native.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:install_plugin/install_plugin.dart';

import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/tree_segments.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/tree_segments.dart';
import 'package:ekimemo_map/services/config.dart';

class AssetUpdater {
  static final _dio = Dio();
  static bool _isChecking = false;

  static void check(BuildContext context, {bool force = false, silent = false}) async {
    if (_isChecking) return;
    _isChecking = true;
    final response = await _dio.get('https://raw.githubusercontent.com/Seo-4d696b75/station_database/main/latest_info.json');
    _isChecking = false;
    var updateAvailable = false;
    if (response.statusCode != 200) throw Exception('Failed to get latest info');

    final latestInfo = jsonDecode(response.data);
    if (Config.getString('station_data_version') != latestInfo['version'].toString()) updateAvailable = true;

    if (!context.mounted) return;
    if (force) return _update(context, latestInfo['url']!, latestInfo['size']!);
    if (!updateAvailable && silent) return;

    showDialog(context: context, builder: (ctx) {
      return updateAvailable ? AlertDialog(
        title: const Text('駅データ更新'),
        content: Text('新しい駅データ (${latestInfo['version']}) が利用可能です。更新しますか？'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
            },
            child: const Text('キャンセル'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              _update(context, latestInfo['url']!, latestInfo['size']!);
            },
            child: const Text('更新'),
          ),
        ],
      ) : AlertDialog(
        title: const Text('駅データ更新'),
        content: const Text('最新の駅データです。'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
            },
            child: const Text('OK'),
          ),
        ],
      );
    });
  }

  static void _update(BuildContext context, String assetUrl, int size) {
    final streamController = StreamController<double>();
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showDialog(context: context, barrierDismissible: false, builder: (ctx) {
      popContext(ctx);
      return AlertDialog(
        title: const Text('駅データ更新'),
        content: StreamBuilder<double>(
          stream: streamController.stream,
          initialData: 0.0,
          builder: (_, snapshot) {
            return Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('ダウンロードしています... (${(snapshot.data! * 100).toStringAsFixed(0)}%)'),
                const SizedBox(height: 16),
                LinearProgressIndicator(value: snapshot.data),
              ],
            );
          },
        )
      );
    });

    _dio.get(assetUrl, onReceiveProgress: (current, _) {
      streamController.add(current / size);
    }).then((response) {
      isCanPop = true;
      popContext(null);
      if (response.statusCode != 200) throw Exception('Failed to download asset');

      _apply(context, jsonDecode(response.data));
    });
  }

  static void _apply(BuildContext context, Map<String, dynamic> data) {
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showDialog(context: context, barrierDismissible: false, builder: (ctx) {
      popContext(ctx);
      return const AlertDialog(
        title: Text('駅データ更新'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('適用しています...\n(時間がかかる場合があります)'),
            SizedBox(height: 16),
            LinearProgressIndicator(),
          ],
        ),
      );
    });

    final stationsRepository = StationRepository();
    final linesRepository = LineRepository();
    final treeSegmentsRepository = TreeSegmentsRepository();

    final stationsData = data['stations']!.map((x) => Station().fromJson(x)).toList();
    final linesData = data['lines']!.map((x) => Line().fromJson(x)).toList();
    final treeSegmentsData = data['tree_segments']!.map((x) => TreeSegments().fromJson(x)).toList();

    Future.wait([
      stationsRepository.clear().then((_) => stationsRepository.bulkInsertModel(stationsData.cast<Station>() as List<Station>)),
      linesRepository.clear().then((_) => linesRepository.bulkInsertModel(linesData.cast<Line>() as List<Line>)),
      treeSegmentsRepository.clear().then((_) => treeSegmentsRepository.bulkInsertModel(treeSegmentsData.cast<TreeSegments>() as List<TreeSegments>)),
    ]).then((_) async {
      await linesRepository.rebuildUniqueStationList();
    }).then((_) {
      isCanPop = true;
      popContext(null);

      Config.setString('station_data_version', data['version'].toString());

      showDialog(context: context, builder: (ctx) {
        return AlertDialog(
          title: const Text('駅データ更新'),
          content: const Text('駅データを更新しました'),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
              },
              child: const Text('OK'),
            ),
          ],
        );
      });
    });
  }
}

class AppUpdater {
  static final _dio = Dio();
  static bool _isChecking = false;

  static void check(BuildContext context, {silent = false}) async {
    if (_isChecking) return;
    _isChecking = true;
    final response = await _dio.get('https://pages.yukineko.me/ekimemo-map-app/version.json');
    _isChecking = false;
    var updateAvailable = false;
    if (response.statusCode != 200) throw Exception('Failed to get latest info');

    final info = response.data;
    final releaseCommitHash = info['commit'].toString();
    final appCommitHash = await NativeMethods().getCommitHash();
    if (appCommitHash != releaseCommitHash) updateAvailable = true;

    if (!context.mounted) return;
    if (!updateAvailable && silent) return;

    showDialog(context: context, builder: (ctx) {
      return updateAvailable ? AlertDialog(
        title: const Text('アプリ更新'),
        content: Text('新しいバージョン v${info['version']} ($releaseCommitHash) が利用可能です。更新しますか？'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
            },
            child: const Text('キャンセル'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              _update(context, 'https://pages.yukineko.me/ekimemo-map-app/app-release.apk', info['size']!);
            },
            child: const Text('更新'),
          ),
        ],
      ) : AlertDialog(
        title: const Text('アプリ更新'),
        content: const Text('更新はありませんでした'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
            },
            child: const Text('OK'),
          ),
        ],
      );
    });
  }

  static void _update(BuildContext context, String assetUrl, int size) async {
    final streamController = StreamController<double>();
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showDialog(context: context, barrierDismissible: false, builder: (ctx) {
      popContext(ctx);
      return AlertDialog(
          title: const Text('アプリ更新'),
          content: StreamBuilder<double>(
            stream: streamController.stream,
            initialData: 0.0,
            builder: (_, snapshot) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('ダウンロードしています... (${(snapshot.data! * 100).toStringAsFixed(0)}%)'),
                  const SizedBox(height: 16),
                  LinearProgressIndicator(value: snapshot.data),
                ],
              );
            },
          )
      );
    });

    final downloadPath = '${(await getTemporaryDirectory()).path}/app-release.apk';
    final response = await _dio.download(assetUrl, downloadPath, onReceiveProgress: (current, _) {
      streamController.add(current / size);
      Options(responseType: ResponseType.bytes);
    });

    isCanPop = true;
    popContext(null);
    if (response.statusCode != 200) throw Exception('Failed to download artifact');

    InstallPlugin.installApk(downloadPath);
  }
}
