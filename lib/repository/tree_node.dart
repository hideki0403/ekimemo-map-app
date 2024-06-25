import 'dart:collection';
import '_abstract.dart';
import 'package:ekimemo_map/models/tree_node.dart';
import 'package:ekimemo_map/services/config.dart';

class TreeNodeRepository extends AbstractRepository<TreeNode> {
  static TreeNodeRepository? _instance;
  TreeNodeRepository._internal() : super(TreeNode(), 'tree_node', 'id');

  factory TreeNodeRepository() {
    _instance ??= TreeNodeRepository._internal();
    return _instance!;
  }

  static final _useCache = !Config.disableDbCache;
  static final _cache = SplayTreeMap<String, TreeNode>();

  Future<void> buildCache() async {
    if (!_useCache) return;
    final nodes = await super.getAll();
    _cache.clear();
    for (final node in nodes) {
      _cache[node.id] = node;
    }
  }

  @override
  Future<TreeNode?> get(dynamic key, {String? column}) async {
    if (_useCache) {
      if (column == null) {
        return _cache[key.toString()];
      } else {
        logger.info('column is specified, fallback to database');
      }
    }
    return super.get(key, column: column);
  }

  @override
  Future<int> count() async {
    return _useCache ? _cache.length : await super.count();
  }
}
