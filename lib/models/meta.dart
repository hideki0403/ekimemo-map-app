import '_abstract.dart';

class Meta extends AbstractModel {
  late String key;
  late String value;

  @override
  Meta fromMap(Map<String, dynamic> map) {
    final meta = Meta();
    meta.key = map['key'];
    meta.value = map['value'];
    return meta;
  }

  @override
  Meta fromJson(Map<String, dynamic> json) {
    final meta = Meta();
    meta.key = json['key'];
    meta.value = json['value'];
    return meta;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'key': key,
      'value': value,
    };
  }
}