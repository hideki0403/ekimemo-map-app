import 'dart:convert';

import 'package:collection/collection.dart';
import 'package:msgpack_dart/msgpack_dart.dart';
import 'package:file_picker/file_picker.dart';

import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/services/station.dart';
import 'package:ekimemo_map/services/log.dart';
import 'package:ekimemo_map/services/cache.dart';
import 'package:ekimemo_map/repository/access_log.dart';
import 'package:ekimemo_map/models/access_log.dart';

final logger = Logger('BackupService');

class BackupService {
  static final _repository = AccessLogRepository();

  static Future<void> backup() async {
    final accessLogs = await _repository.getAll();
    final data = accessLogs.map((accessLog) => accessLog.toMap()).toList();
    final msgpack = serialize(data);
    final result = await FilePicker.platform.saveFile(
      fileName: 'ekimemo_map_backup.bin',
      bytes: msgpack,
    );

    if (result != null) {
      showMessageDialog(message: 'バックアップに成功しました');
      logger.info('Backup success: $result');
    }
  }

  static Future<void> restore() async {
    final replace = await showYesNoDialog(
      title: 'データの復元',
      message: '既に記録されているデータを削除して復元しますか？\n「上書きして復元」を選択すると、現在のデータを残したまま復元します。',
      yesText: '上書きして復元',
      noText: '削除して復元',
    );
    if (replace == null) return;

    final result = await FilePicker.platform.pickFiles(
      type: FileType.any,
      withData: true,
    );
    if (result == null) return;

    List<AccessLog> accessLogs = [];

    try {
      final bytes = result.files.single.bytes;
      final data = deserialize(bytes!);
      accessLogs = data.map((e) => AccessLog().fromMap(e.cast<String, dynamic>())).toList().cast<AccessLog>() as List<AccessLog>;
    } catch (e) {
      showMessageDialog(message: 'バックアップファイルの読み込みに失敗しました');
      logger.warning('Restore failed: $e');
      return;
    }

    if (!replace) {
      await _repository.clear();
    }

    await _repository.bulkInsertModel(accessLogs);
    await AccessCacheManager.initialize(force: true);

    showMessageDialog(message: 'データの復元に成功しました');
  }

  static Future<void> importCsv() async {
    final replace = await showYesNoDialog(
      title: 'データのインポート',
      message: '既に記録されているデータを削除してインポートしますか？\n「上書きしてインポート」を選択すると、現在のデータを残したままインポートします。',
      yesText: '上書きしてインポート',
      noText: '削除してインポート',
    );
    if (replace == null) return;

    final result = await FilePicker.platform.pickFiles(
      type: FileType.any,
      withData: true,
    );
    if (result == null) return;

    final file = result.files.first;
    if (file.extension != 'csv') {
      showMessageDialog(message: '非対応のファイルです。\nCSVファイルを選択してください。');
      return;
    }

    final accessLogs = <AccessLog>[];
    final failedStations = <String>[];
    final now = DateTime.now();

    try {
      final csv = utf8.decode(file.bytes!);
      final lines = csv.split('\n');

      final stationCache = await StationCache.getAll();
      if (stationCache == null || stationCache.isEmpty) {
        showMessageDialog(message: 'インポート中にエラーが発生しました');
        return;
      }

      for (var line in lines) {
        if (line.startsWith('#') || !line.contains(',')) continue;
        final values = line.split(',');

        var stationData = stationCache.firstWhereOrNull((station) => station.originalName == values[1]);
        stationData ??= stationCache.firstWhereOrNull((station) => station.code == int.parse(values[0]));

        if (stationData == null) {
          failedStations.add(values[1]);
          continue;
        }

        final accessLog = AccessLog();
        accessLog.id = stationData.id;
        accessLog.firstAccess = now;
        accessLog.lastAccess = now;
        accessLog.accessCount = 1;
        accessLog.accessed = true;

        accessLogs.add(accessLog);
      }
    } catch (e) {
      showMessageDialog(message: 'CSVファイルの読み込みに失敗しました');
      logger.warning('Import failed: $e');
      return;
    }

    if (!replace) {
      await _repository.clear();
    }

    await _repository.bulkInsertModel(accessLogs);
    await AccessCacheManager.initialize(force: true);

    var message = 'データをインポートしました。\n\nインポートに成功した駅数: ${accessLogs.length}\nインポートに失敗した駅数: ${failedStations.length}';
    if (failedStations.isNotEmpty) {
      message += '\n\n以下の駅のインポートに失敗しました:\n${failedStations.join(', ')}';
    }

    showMessageDialog(title: 'インポート', message: message);
  }
}