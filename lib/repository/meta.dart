import 'package:ekimemo_map/models/meta.dart';
import '_abstract.dart';

class MetaRepository extends AbstractRepository<Meta> {
  MetaRepository() : super(Meta(), 'meta', 'key');

  Future<String> getValue(String key) async {
    final meta = await get(key);
    return meta?.value ?? '';
  }

  Future<void> setValue(String key, String value) async {
    await insert({'key': key, 'value': value});
  }
}