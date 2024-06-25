import 'package:ekimemo_map/models/meta.dart';
import '_abstract.dart';

class MetaRepository extends AbstractRepository<Meta> {
  static MetaRepository? _instance;
  MetaRepository._internal() : super(Meta(), 'meta', 'key');

  factory MetaRepository() {
    _instance ??= MetaRepository._internal();
    return _instance!;
  }

  Future<String> getValue(String key) async {
    final meta = await get(key);
    return meta?.value ?? '';
  }

  Future<void> setValue(String key, String value) async {
    await insert({'key': key, 'value': value});
  }
}