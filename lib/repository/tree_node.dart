import 'package:ekimemo_map/models/tree_node.dart';
import '_abstract.dart';

class TreeNodeRepository extends AbstractRepository<TreeNode> {
  TreeNodeRepository() : super(TreeNode(), 'tree_node', 'id');
}