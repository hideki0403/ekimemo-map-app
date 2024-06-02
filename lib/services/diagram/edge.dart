import 'types.dart';
import 'line.dart' as line;

Edge init(Point a, Point b) {
  if (a.x < b.x) {
    return Edge(a, b);
  } else if (a.x > b.x) {
    return Edge(b, a);
  } else {
    if (a.y < b.y) {
      return Edge(a, b);
    } else if (a.y > b.y) {
      return Edge(b, a);
    } else {
      throw Exception('Invalid edge');
    }
  }
}

Line toLine(Edge edge) {
  return line.init(edge.a, edge.b);
}