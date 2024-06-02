import 'dart:math' as math;

import 'types.dart';
import 'point.dart' as point;
import 'edge.dart' as edge;

// a: num | Point, b: num | Point, c: num?
Line init(dynamic a, dynamic b, [num? c]) {
  if (a is num && b is num) {
    if (c != null) {
      if (b == 0) {
        if (a == 0) {
          throw Exception('a = b = 0');
        }
        return Line(1.0, 0.0, c / a);
      } else {
        return Line(a / b, 1.0, c / b);
      }
    } else {
      return Line(-a.toDouble(), 1.0, -b.toDouble());
    }
  } else if (a is Point && b is Point) {
    if (point.equals(a, b)) {
      throw Exception('duplicated point: $a');
    } else if (a.x == b.x) {
      return Line(1.0, 0.0, -(a.x + b.x) / 2);
    } else {
      return Line((b.y - a.y) / (a.x - b.x), 1.0, (b.x * a.y - a.x * b.y) / (a.x - b.x));
    }
  }

  throw Exception('cannot get an instance with a:$a b:$b c:$c');
}

// l2: Line | Edge
Point? getIntersection(Line l1, dynamic l2) {
  if (l2 is Edge) {
    final line = l1;
    final e = l2;

    if ((line.a * e.a.x + line.b * e.a.y + line.c) * (line.a * e.b.x + line.b * e.b.y + line.c) <= 0) {
      l2 = edge.toLine(e);
    } else {
      return null;
    }
  }

  if (l2 is! Line) throw Exception('l2 is not Line: $l2');

  final det = l1.a * l2.b - l2.a * l1.b;
  if (det == 0) return null;

  var x = (l1.b * l2.c - l2.b * l1.c) / det;
  var y = (l2.a * l1.c - l1.a * l2.c) / det;

  if (l1.b == 0) x = -l1.c;
  if (l2.b == 0) x = -l2.c;
  if (l1.a == 0) y = -l1.c;
  if (l2.a == 0) y = -l2.c;

  return Point(x, y);
}

// p1: Point | Edge
Line getPerpendicularBisector(dynamic p1, [Point? p2]) {
  if (p1 is Edge) {
    final e = p1;
    p1 = e.a;
    p2 = e.b;
  }

  if (p1 is! Point) throw Exception('p1 is not Point: $p1');
  if (p2 == null) throw Exception('invalid arguments: p1=$p1 p2=$p2');

  return init(
    p1.x - p2.x,
    p1.y - p2.y,
    (-math.pow(p1.x, 2) - math.pow(p1.y, 2) + math.pow(p2.x, 2) + math.pow(p2.y, 2)) / 2,
  );
}

bool onSameSide(Line line, Point p1, Point p2) {
  final v1 = line.a * p1.x + line.b * p1.y + line.c;
  final v2 = line.a * p2.x + line.b * p2.y + line.c;
  return v1 * v2 >= 0;
}
