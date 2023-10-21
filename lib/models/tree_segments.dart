import '_abstract.dart';
import 'dart:convert';

class TreeSegments extends AbstractModel {
  late final String name;
  late final int root;
  late final List<Map<String, dynamic>> nodeList;

  @override
  TreeSegments fromMap(Map<String, dynamic> map) {
    final segments = TreeSegments();
    segments.name = map['name'];
    segments.root = map['root'];
    segments.nodeList = jsonDecode(map['node_list']).cast<Map<String, dynamic>>() as List<Map<String, dynamic>>;
    return segments;
  }
  
  @override
  TreeSegments fromJson(Map<String, dynamic> json) {
    final segments = TreeSegments();
    segments.name = json['name'];
    segments.root = json['root'];
    segments.nodeList = json['node_list'].cast<Map<String, dynamic>>() as List<Map<String, dynamic>>;
    return segments;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'root': root,
      'node_list': jsonEncode(nodeList),
    };
  }
}