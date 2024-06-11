import 'dart:async';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:msgpack_dart/msgpack_dart.dart';
import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:install_plugin/install_plugin.dart';

import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/tree_node.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/log.dart';
import 'package:ekimemo_map/services/cache.dart';
import 'package:ekimemo_map/services/utils.dart';

final logger = Logger('Updater');

class AssetUpdater {
  static bool _isInitialized = false;

  static void check({force = false, silent = false, first = false}) async {
    if (first) {
      if (_isInitialized) return;
      _isInitialized = true;
    }

    logger.debug('Checking for station-database updates');
    var updateAvailable = false;

    final resource = await _UpdateUtils.getLatestRelease('hideki0403/ekimemo-map-database', 'station_database.msgpack');
    if (SystemState.getString('station_data_version') != resource.version) updateAvailable = true;

    logger.debug('Latest version: ${resource.version}, Current version: ${SystemState.getString('station_data_version')}');

    if (force) return _update(resource);

    if (!updateAvailable) {
      if (!silent) showMessageDialog(title: '駅データ更新', message: '最新の駅データです。');
      return;
    }

    final result = await showYesNoDialog(
      title: '駅データ更新',
      message: '新しい駅データ (${resource.version}) が利用可能です。更新しますか？',
      yesText: '更新',
      noText: 'キャンセル',
    );

    if (result) _update(resource);
  }

  static Future<void> _update(_GitHubResource resource) async {
    dynamic data;
    try {
      data = await _UpdateUtils.downloadResource(resource, '駅データ更新');
    } catch (e) {
      if (e is DioException && e.type == DioExceptionType.cancel) {
        logger.debug('Download canceled');
        return;
      }
      logger.error('Failed to download station database: $e');
      showMessageDialog(title: '駅データ更新', message: 'ダウンロード中にエラーが発生しました');
      return;
    }

    _apply(deserialize(data).cast<String, dynamic>(), resource.version);
  }

  static void _apply(Map<String, dynamic> data, String version) async {
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showMessageDialog(
      title: '駅データ更新',
      disableActions: true,
      receiver: (ctx) => popContext(ctx),
      content: const Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('適用しています...\n(時間がかかる場合があります)'),
          SizedBox(height: 16),
          LinearProgressIndicator(),
        ],
      ),
    );

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

    showMessageDialog(title: '駅データ更新', message: '駅データを更新しました');
  }
}

class AppUpdater {
  static void check({silent = false}) async {
    logger.debug('Checking for app updates');
    var updateAvailable = false;

    final resource = await _UpdateUtils.getLatestRelease('hideki0403/ekimemo-map-app', 'app-release.apk');
    final packageInfo = await PackageInfo.fromPlatform();
    final currentVersion = 'v${packageInfo.version}+${packageInfo.buildNumber}';

    if (currentVersion != resource.version) updateAvailable = true;

    logger.debug('Latest version: ${resource.version}, Current version: $currentVersion');

    if (!updateAvailable) {
      if (!silent) showMessageDialog(title: 'アプリ更新', message: '更新はありませんでした');
      return;
    }

    final result = await showYesNoDialog(
      title: 'アプリ更新',
      message: '新しいバージョン (${resource.version}) が利用可能です。更新しますか？',
      yesText: '更新',
      noText: 'キャンセル',
    );

    if (result) _update(resource);
  }

  static void _update(_GitHubResource resource) async {
    final downloadPath = '${(await getTemporaryDirectory()).path}/app-release.apk';

    try {
      await _UpdateUtils.downloadResource(resource, 'アプリ更新', path: downloadPath);
    } catch (e) {
      if (e is DioException && e.type == DioExceptionType.cancel) {
        logger.debug('Download canceled');
        return;
      }
      logger.error('Failed to download app: $e');
      showMessageDialog(title: 'アプリ更新', message: 'ダウンロード中にエラーが発生しました');
      return;
    }

    InstallPlugin.installApk(downloadPath);
  }
}

class _UpdateUtils {
  static List<String> checking = [];

  static Future<_GitHubResource> getLatestRelease(String repository, String resourceName) async {
    final key = 'check:$repository:$resourceName';
    if (checking.contains(key)) throw Exception('Already checking');
    checking.add(key);

    final response = await Dio().get('https://api.github.com/repos/$repository/releases/latest');
    if (response.statusCode != 200) throw Exception('Failed to get latest info');

    final release = response.data;
    final resource = (release['assets'] as List<dynamic>).firstWhereOrNull((x) => x['name'] == resourceName);
    if (resource == null) throw Exception('Target resource not found');

    checking.remove(key);
    return _GitHubResource(release['tag_name'].toString(), resource['browser_download_url'], resource['size']);
  }

  static Future<T?> downloadResource<T extends dynamic>(_GitHubResource resource, String title, { String? path }) async {
    final key = 'download:${resource.downloadUrl}';
    if (checking.contains(key)) throw Exception('Already downloading');
    checking.add(key);

    final streamController = StreamController<int>();
    final cancelToken = CancelToken();
    BuildContext? internalContext;
    bool isCanPop = false;

    void popContext(BuildContext? ctx) {
      if (internalContext == null && ctx != null) internalContext = ctx;
      if (!isCanPop || internalContext == null) return;
      Navigator.pop(internalContext!);
    }

    showMessageDialog(
      title: title,
      receiver: (ctx) => popContext(ctx),
      actions: [
        TextButton(
          child: const Text('キャンセル'),
          onPressed: () {
            cancelToken.cancel();
            checking.remove(key);
            isCanPop = true;
            popContext(null);
          },
        ),
      ],
      content: StreamBuilder<int>(
        stream: streamController.stream,
        initialData: 0,
        builder: (_, snapshot) {
          final progress = snapshot.data! / resource.size;
          return Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('ダウンロードしています...'),
              const SizedBox(height: 16),
              LinearProgressIndicator(value: progress),
              const SizedBox(height: 4),
              Row(children: [
                Expanded(child: Text('${(progress * 100).toStringAsFixed(0)}%')),
                Text('${formatBytes(snapshot.data!)} / ${formatBytes(resource.size)}'),
              ]),
            ],
          );
        },
      ),
    );

    logger.debug('Downloading ${resource.downloadUrl}');

    final options = Options(responseType: ResponseType.bytes);
    onReceiveProgress(current, _) {
      streamController.add(current);
    }

    Response<dynamic>? response;

    try {
      if (path == null) {
        response = await Dio().get(resource.downloadUrl, onReceiveProgress: onReceiveProgress, options: options, cancelToken: cancelToken);
        if (response.statusCode != 200 || response.data == null) throw Exception('Failed to download asset');
      } else {
        response = await Dio().download(resource.downloadUrl, path, onReceiveProgress: onReceiveProgress, options: options, cancelToken: cancelToken);
        if (response.statusCode != 200) throw Exception('Failed to download asset');
      }
    } catch (e) {
      checking.remove(key);
      rethrow;
    }

    logger.debug('Downloaded ${resource.downloadUrl}');
    isCanPop = true;
    popContext(null);

    checking.remove(key);
    return path == null ? response.data : null;
  }
}

class _GitHubResource {
  final String version;
  final String downloadUrl;
  final int size;

  _GitHubResource(this.version, this.downloadUrl, this.size);
}