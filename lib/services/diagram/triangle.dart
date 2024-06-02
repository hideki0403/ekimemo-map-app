import 'types.dart';

bool containsPoint(Triangle triangle, Point p, [num error = 0.0]) {
  final x1 = triangle.a.x - p.x;
  final y1 = triangle.a.y - p.y;
  final x2 = triangle.b.x - p.x;
  final y2 = triangle.b.y - p.y;
  final x3 = triangle.c.x - p.x;
  final y3 = triangle.c.y - p.y;
  final v1 = x1 * y2 - y1 * x2;
  final v2 = x2 * y3 - y2 * x3;
  final v3 = x3 * y1 - y3 * x1;

  if (error > 0) {
    return (v1 > -error && v2 > -error && v3 > -error) || (v1 < error && v2 < error && v3 < error);
  }

  return (v1 >= 0 && v2 >= 0 && v3 >= 0) || (v1 <= 0 && v2 <= 0 && v3 <= 0);
}