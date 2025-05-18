import '_abstract.dart';

class TreeNode extends AbstractModel {
  late final int id;
  late final int? left;
  late final int? right;

  @override
  TreeNode fromMap(Map<String, dynamic> map) {
    final node = TreeNode();
    node.id = map['id'];
    node.left = map['left'];
    node.right = map['right'];
    return node;
  }

  @override
  TreeNode fromJson(Map<String, dynamic> json) {
    final node = TreeNode();
    node.id = json['id'];
    node.left = json['left'];
    node.right = json['right'];
    return node;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'left': left,
      'right': right,
    };
  }
}