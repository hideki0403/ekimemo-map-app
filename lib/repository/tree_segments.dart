import 'package:ekimemo_map/models/tree_segments.dart';
import '_abstract.dart';

class TreeSegmentsRepository extends AbstractRepository<TreeSegments> {
  TreeSegmentsRepository() : super(TreeSegments(), 'tree_segments', 'name');
}