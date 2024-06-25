import 'package:ekimemo_map/models/tree_node.dart';
import '_abstract.dart';

class TreeNodeRepository extends AbstractRepository<TreeNode> {
  static TreeNodeRepository? _instance;
  TreeNodeRepository._internal() : super(TreeNode(), 'tree_node', 'id');

  factory TreeNodeRepository() {
    _instance ??= TreeNodeRepository._internal();
    return _instance!;
  }
}