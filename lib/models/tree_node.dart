import '_abstract.dart';

class TreeNode extends AbstractModel {
  late final int code;
  late final String name;
  late final double lat;
  late final double lng;
  late final int? left;
  late final int? right;
  late final String? segment;

  @override
  TreeNode fromMap(Map<String, dynamic> map) {
    final node = TreeNode();
    node.code = map['code'];
    node.name = map['name'];
    node.lat = map['lat'];
    node.lng = map['lng'];
    node.left = map['left'];
    node.right = map['right'];
    node.segment = map['segment'];
    return node;
  }

  @override
  TreeNode fromJson(Map<String, dynamic> json) {
    final node = TreeNode();
    node.code = json['code'];
    node.name = json['name'];
    node.lat = json['lat'];
    node.lng = json['lng'];
    node.left = json['left'];
    node.right = json['right'];
    node.segment = json['segment'];
    return node;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'code': code,
      'name': name,
      'lat': lat,
      'lng': lng,
      'left': left,
      'right': right,
      'segment': segment,
    };
  }
}