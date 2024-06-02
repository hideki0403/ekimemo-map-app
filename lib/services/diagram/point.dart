import 'types.dart';
import 'utils.dart';

bool equals(Point p1, Point p2) {
  return p1.x == p2.x && p1.y == p2.y;
}

int compare(Point p1, Point p2) {
  if (p1.x == p2.x) {
    if (p1.y == p2.y) return 0;
    return p1.y < p2.y ? -1 : 1;
  }

  return p1.x < p2.x ? -1 : 1;
}

int hashCode(Point p) {
  return stringHash('${p.x},${p.y}');
}