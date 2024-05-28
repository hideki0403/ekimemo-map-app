import 'dart:async';
import 'package:dio/dio.dart';
import 'package:msgpack_dart/msgpack_dart.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:install_plugin/install_plugin.dart';

import 'package:ekimemo_map/main.dart';
import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/tree_node.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/native.dart';
import 'package:ekimemo_map/services/log.dart';
import 'package:ekimemo_map/services/cache.dart';

final logger = Logger('Updater');

class AssetUpdater {
  static final _dio = Dio();
  static bool _isChecking = false;
  static bool _isInitialized = false;

  static void check({force = false, silent = false, first = false}) async {
    if (_isChecking) return;

    if (first) {
      if (_isInitialized) return;
      _isInitialized = true;
    }

    logger.debug('Checking for station-database updates');

    _isChecking = true;
    final response = await _dio.get('https://api.github.com/repos/hideki0403/ekimemo-map-database/releases/latest');
    _isChecking = false;
    var updateAvailable = false;
    if (response.statusCode != 200) throw Exception('Failed to get latest info');

    final release = response.data;
    final latestVersion = release['tag_name'].toString();
    final size = release['assets'].firstWhere((x) => x['name'] == 'station_database.msgpack')?['size'] ?? 0;
    if (SystemState.getString('station_data_version') != latestVersion) updateAvailable = true;

    logger.debug('Latest version: $latestVersion, Current version: ${SystemState.getString('station_data_version')}');

    if (force) return _update(size, latestVersion);
    if (!updateAvailable && silent) return;

    showDialog(context: navigatorKey.currentContext!, builder: (ctx) {
      return updateAvailable ? AlertDialog(
        title: const Text('駅データ更新'),
        content: Text('新しい駅データ ($latestVersion) が利用可能です。更新しますか？'),
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
              _update(size, latestVersion);
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

  static void _update(int size, String version) {
    final streamController = StreamController<double>();
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showDialog(context: navigatorKey.currentContext!, barrierDismissible: false, builder: (ctx) {
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

    logger.debug('Downloading station-database.msgpack');

    _dio.get('https://github.com/hideki0403/ekimemo-map-database/releases/download/$version/station_database.msgpack', onReceiveProgress: (current, _) {
      streamController.add(current / size);
    }, options: Options(responseType: ResponseType.bytes)).then((response) {
      isCanPop = true;
      popContext(null);
      if (response.statusCode != 200 || response.data == null) throw Exception('Failed to download asset');
      logger.debug('Downloaded station-database.msgpack');
      _apply(deserialize(response.data).cast<String, dynamic>(), version);
    });
  }

  static void _apply(Map<String, dynamic> data, String version) async {
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showDialog(context: navigatorKey.currentContext!, barrierDismissible: false, builder: (ctx) {
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

    logger.debug('Applying latest station database');

    final stationsRepository = StationRepository();
    final linesRepository = LineRepository();
    final treeNodesRepository = TreeNodeRepository();

    final stationsData = data['station']!.map((x) => Station().fromJson(x.cast<String, dynamic>())).toList();
    final linesData = data['line']!.map((x) => Line().fromJson(x.cast<String, dynamic>())).toList();
    final treeNodesData = data['tree']['node_list'].map((x) => TreeNode().fromJson(x.cast<String, dynamic>())).toList();

    SystemState.setString('tree_node_root', data['tree']['root'].toString());

    await Future.wait([
      stationsRepository.clear().then((_) => stationsRepository.bulkInsertModel(stationsData.cast<Station>() as List<Station>)),
      linesRepository.clear().then((_) => linesRepository.bulkInsertModel(linesData.cast<Line>() as List<Line>)),
      treeNodesRepository.clear().then((_) => treeNodesRepository.bulkInsertModel(treeNodesData.cast<TreeNode>() as List<TreeNode>)),
    ]);

    isCanPop = true;
    popContext(null);

    SystemState.setString('station_data_version', version);

    // データベースのキャッシュを再構築
    await CacheManager.initialize();

    // TreeNodeを再構築する
    StationSearchService.clear();
    await StationSearchService.initialize();

    logger.debug('Applied latest station database');

    showDialog(context: navigatorKey.currentContext!, builder: (ctx) {
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
  }
}

class AppUpdater {
  static final _dio = Dio();
  static bool _isChecking = false;

  static void check({silent = false}) async {
    if (_isChecking) return;

    logger.debug('Checking for app updates');

    _isChecking = true;
    final response = await _dio.get('https://pages.yukineko.me/ekimemo-map-app/version.json');
    _isChecking = false;
    var updateAvailable = false;
    if (response.statusCode != 200) throw Exception('Failed to get latest info');

    final info = response.data;
    final releaseCommitHash = info['commit'].toString();
    final appCommitHash = await NativeMethods.getCommitHash();
    if (appCommitHash != releaseCommitHash) updateAvailable = true;

    logger.debug('Latest version: v${info['version']} ($releaseCommitHash), Current version: v${info['version']} ($appCommitHash)');

    if (!updateAvailable && silent) return;

    showDialog(context: navigatorKey.currentContext!, builder: (ctx) {
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
              _update('https://pages.yukineko.me/ekimemo-map-app/app-release.apk', info['size']!);
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

  static void _update(String assetUrl, int size) async {
    final streamController = StreamController<double>();
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showDialog(context: navigatorKey.currentContext!, barrierDismissible: false, builder: (ctx) {
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

    logger.debug('Downloading app-release.apk');

    final downloadPath = '${(await getTemporaryDirectory()).path}/app-release.apk';
    final response = await _dio.download(assetUrl, downloadPath, onReceiveProgress: (current, _) {
      streamController.add(current / size);
      Options(responseType: ResponseType.bytes);
    });

    isCanPop = true;
    popContext(null);
    if (response.statusCode != 200) throw Exception('Failed to download artifact');

    logger.debug('Downloaded app-release.apk');

    InstallPlugin.installApk(downloadPath);
  }
}
