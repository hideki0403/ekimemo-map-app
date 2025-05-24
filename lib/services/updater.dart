import 'dart:async';
import 'package:collection/collection.dart';
import 'package:dio/dio.dart';
import 'package:msgpack_dart/msgpack_dart.dart';
import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:android_package_installer/android_package_installer.dart';

import 'package:ekimemo_map/repository/station.dart';
import 'package:ekimemo_map/repository/line.dart';
import 'package:ekimemo_map/repository/tree_node.dart';
import 'package:ekimemo_map/models/station.dart';
import 'package:ekimemo_map/models/line.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/services/config.dart';
import 'package:ekimemo_map/services/search.dart';
import 'package:ekimemo_map/services/log.dart';
import 'package:ekimemo_map/services/utils.dart';

final logger = Logger('Updater');

class UpdateStateNotifier extends ChangeNotifier {
  static final _instance = UpdateStateNotifier._internal();
  factory UpdateStateNotifier() => _instance;

  UpdateStateNotifier._internal() {
    UpdateManager.init(this);
  }

  bool get hasUpdate => UpdateManager.hasUpdate;

  void updateStatus() {
    notifyListeners();
  }
}

class UpdateManager {
  static UpdateStateNotifier? _updateStateNotifier;

  static bool get hasUpdate => _StationSourceUpdater.hasUpdate || _AppUpdater.hasUpdate;

  static void init(UpdateStateNotifier notifier) {
    _updateStateNotifier = notifier;
  }

  static void notify() {
    _updateStateNotifier?.updateStatus();
  }

  static Future<void> checkForUpdates() async {
    await _AppUpdater.fetch(withNotify: false);
    await _StationSourceUpdater.fetch(withNotify: false);
    notify();
  }

  static Future<void> updateAppOrStationSource() async {
    if (_AppUpdater.hasUpdate) {
      await updateApp(manual: false);
    } else if (_StationSourceUpdater.hasUpdate) {
      await updateStationSource(manual: false);
    }
  }

  static Future<void> updateStationSource({manual = true}) async {
    if (!_StationSourceUpdater.hasUpdate) {
      final hasUpdate = manual && await _StationSourceUpdater.fetch();
      if (!hasUpdate) {
        if (manual) showMessageDialog(title: '駅データ更新', message: '最新の駅データです。');
        return;
      }
    }

    final result = await showYesNoDialog(
      title: '駅データ更新',
      message: '新しい駅データ (${_StationSourceUpdater.resource?.version}) が利用可能です。更新しますか？',
      yesText: '更新',
      noText: 'キャンセル',
    );

    if (result == true) _StationSourceUpdater.update();
  }

  static Future<void> updateApp({manual = true}) async {
    if (!_AppUpdater.hasUpdate) {
      final hasUpdate = manual && await _AppUpdater.fetch();
      if (!hasUpdate) {
        if (manual) showMessageDialog(title: 'アプリ更新', message: '最新のアプリです。');
        return;
      }
    }

    final result = await showYesNoDialog(
      title: 'アプリ更新',
      message: '新しいバージョン (${_AppUpdater.resource?.version}) が利用可能です。更新しますか？',
      yesText: '更新',
      noText: 'キャンセル',
    );

    if (result == true) _AppUpdater.update();
  }
}

class _AppUpdater {
  static String? _cachePath;
  static String? _currentVersion;
  static _GitHubResource? _resource;

  static _GitHubResource? get resource => _resource;
  static bool get hasUpdate => _resource != null && _currentVersion != null && _currentVersion != _resource!.version;

  static Future<bool> fetch({withNotify = true}) async {
    logger.debug('Checking for app updates');

    final resource = await _UpdateUtils.getLatestRelease('hideki0403/ekimemo-map-app', 'app-release.apk');
    final packageInfo = await PackageInfo.fromPlatform();
    _currentVersion = 'v${packageInfo.version}';

    logger.debug('[App] Latest: ${resource.version}, Current: $_currentVersion');

    if (!withNotify) {
      UpdateManager.notify();
    }

    return hasUpdate;
  }

  static void update() async {
    if (_resource == null) {
      throw Exception('No resource available for app update');
    }

    final downloadPath = '${(await getTemporaryDirectory()).path}/app-release.apk';
    String? path;

    if (_cachePath != null) {
      final result = await showYesNoDialog(
        title: 'アプリ更新',
        message: 'ダウンロードキャッシュがあります。\n再度ダウンロードしますか？',
        yesText: 'キャッシュを使用',
        noText: 'ダウンロード',
      );

      if (result == null) return;
      if (result) {
        path = _cachePath;
      }
    }

    if (path == null) {
      try {
        await _UpdateUtils.downloadResource(_resource!, 'アプリ更新', path: downloadPath);
        _cachePath = downloadPath;
        path = downloadPath;
      } catch (e) {
        if (e is DioException && e.type == DioExceptionType.cancel) {
          logger.debug('Download canceled');
          return;
        }
        logger.error('Failed to download app: $e');
        showMessageDialog(title: 'アプリ更新', message: 'ダウンロード中にエラーが発生しました');
        return;
      }
    }

    await AndroidPackageInstaller.installApk(
      apkFilePath: path,
    );
  }
}

class _StationSourceUpdater {
  static _GitHubResource? _resource;

  static _GitHubResource? get resource => _resource;
  static bool get hasUpdate => _resource != null && SystemState.getString('station_data_version') != _resource!.version;

  static Future<bool> fetch({withNotify = true}) async {
    logger.debug('Checking for station-database updates');

    _resource = await _UpdateUtils.getLatestRelease('hideki0403/ekimemo-map-database', 'station_database.msgpack');

    final currentVersion = SystemState.getString('station_data_version');
    logger.debug('[StationSource] Latest: ${_resource!.version}, Current: ${currentVersion.isEmpty ? 'N/A' : currentVersion}');

    if (withNotify) {
      UpdateManager.notify();
    }

    return hasUpdate;
  }

  static Future<void> update() async {
    if (_resource == null) {
      throw Exception('No resource available for source update');
    }

    dynamic data;
    try {
      data = await _UpdateUtils.downloadResource(_resource!, '駅データ更新');
    } catch (e) {
      if (e is DioException && e.type == DioExceptionType.cancel) {
        logger.debug('Download canceled');
        return;
      }
      logger.error('Failed to download station database: $e');
      showMessageDialog(title: '駅データ更新', message: 'ダウンロード中にエラーが発生しました');
      return;
    }

    _apply(deserialize(data).cast<String, dynamic>(), _resource!.version);
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
      disableClose: true,
      receiver: (ctx) => popContext(ctx),
      content: const Column(
        mainAxisSize: MainAxisSize.min,
        spacing: 16,
        children: [
          Text('適用しています...\n(時間がかかる場合があります)'),
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
    await StationRepository().buildCache();
    await LineRepository().buildCache();
    await TreeNodeRepository().buildCache();

    // TreeNodeを再構築する
    StationSearchService.clear();
    await StationSearchService.initialize();

    logger.debug('Applied latest station database');

    showMessageDialog(title: '駅データ更新', message: '駅データを更新しました');
    UpdateManager.notify();
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

  static Future<T?> downloadResource<T extends dynamic>(_GitHubResource resource, String title, {String? path}) async {
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
      disableClose: true,
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
