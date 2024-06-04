import 'dart:math' as math;
import 'types.dart';
import 'point.dart' as point;

Rect init(double left, double top, double right, double bottom) {
  if (left.isFinite && top.isFinite && right.isFinite && bottom.isFinite && left < right && bottom < top) {
    return Rect(left, top, right, bottom);
  } else {
    throw Exception('Invalid rect: $left, $top, $right, $bottom');
  }
}

Triangle getContainer(Rect rect) {
  final x = (rect.left + rect.right) / 2;
  final y = (rect.top + rect.bottom) / 2;
  final r = math.sqrt(math.pow(rect.left - rect.right, 2) + math.pow(rect.top - rect.bottom, 2));
  final a = point.init(x - math.sqrt(3) * r, y + r);
  final b = point.init(x + math.sqrt(3) * r, y + r);
  final c = point.init(x, y - 2 * r);
  return Triangle(a, b, c);
}
