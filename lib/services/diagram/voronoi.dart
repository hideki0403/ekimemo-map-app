import 'dart:math' as math;

import 'package:ekimemo_map/services/log.dart';
import 'types.dart';
import 'line.dart' as line_util;
import 'point.dart' as point;
import 'triangle.dart' as triangle;
import 'utils.dart';

final logger = Logger('voronoi');

enum StepDirection {
  up,
  zero,
  down;

  StepDirection invert() {
    switch (this) {
      case StepDirection.up:
        return StepDirection.down;
      case StepDirection.down:
        return StepDirection.up;
      default:
        return StepDirection.zero;
    }
  }
}

final error = math.pow(2, -30);

class Node<T extends Point> implements Point {
  @override
  late double x;

  @override
  late double y;

  late bool _onBoundary;
  late num _index;
  Intersection<T>? _p1;
  Intersection<T>? _p2;

  Node(Point p, Intersection<T> a, Intersection<T> b) {
    x = p.x;
    y = p.y;
    _p1 = a;
    _p2 = b;

    var cnt = 0;
    if (a.line.isBoundary) cnt++;
    if (b.line.isBoundary) cnt++;

    if (cnt == 0) {
      _onBoundary = false;
      _index = -1;
    } else if (cnt == 1) {
      _onBoundary = true;
      _index = -1;
    } else {
      _onBoundary = false;
      _index = 0;
    }
  }

  Intersection<T> get p1 {
    if (_p1 == null) throw Exception('no intersection p1');
    return _p1!;
  }

  Intersection<T> get p2 {
    if (_p2 == null) throw Exception('no intersection p2');
    return _p2!;
  }

  Node<T> next(Point previous) {
    if (p1.hasNext && point.equals(p1.next, previous)) {
      return calcNext(p1, p2, false, p1.step.invert());
    } else if (p1.hasPrevious && point.equals(p1.previous, previous)) {
      return calcNext(p1, p2, true, p1.step);
    } else if (p2.hasNext && point.equals(p2.next, previous)) {
      return calcNext(p2, p1, false, p2.step.invert());
    } else if (p2.hasPrevious && point.equals(p2.previous, previous)) {
      return calcNext(p2, p1, true, p2.step);
    }

    throw Exception('no next node');
  }
  
  Node<T> calcNext(Intersection<T> current, Intersection<T> other, bool forward, StepDirection step) {
    if (_onBoundary && _index > 0) {
      return forward ? current.next.node : current.previous.node;
    } else {
      return other.neighbor(step.invert()).node;
    }
  }

  Node<T> nextDown(Point previous) {
    Intersection<T>? target;

    if (p1.isNeighbor(previous)) {
      target = p2;
    } else if (p2.isNeighbor(previous)) {
      target = p1;
    } else {
      throw Exception('no neighbor');
    }

    if (target.hasNeighbor(StepDirection.down)) {
      return target.neighbor(StepDirection.down).node;
    } else {
      return target.neighbor(StepDirection.zero).node;
    }
  }

  Node<T>? nextUp(Point previous) {
    Intersection<T>? t1;
    Intersection<T>? t2;

    if (p1.isNeighbor(previous)) {
      t1 = p2;
      t2 = p1;
    } else if (p2.isNeighbor(previous)) {
      t1 = p1;
      t2 = p2;
    } else {
      throw Exception('no neighbor');
    }

    if (t1.hasNeighbor(StepDirection.up)) {
      return t1.neighbor(StepDirection.up).node;
    }

    if (t2.hasNeighbor(StepDirection.up)) {
      return t2.neighbor(StepDirection.up).node;
    }

    return null;
  }

  void onSolved(int level) {
    p1.onSolved();
    p2.onSolved();

    if (_index < 0) {
      _index = p1.line.isBoundary || p2.line.isBoundary ? level : level + 0.5;
    } else if(_index.round() != _index) {
      if (_index + 0.5 != level) throw Exception('mismatched index, current: $level, node: $_index');
    }
  }

  bool hasSolved() {
    return _index >= 0;
  }

  void release() {
    _p1 = null;
    _p2 = null;
  }
}

class IntersectionItem<T extends Point> {
  Intersection<T>? _intersection;
  bool _isInitialized = false;

  IntersectionItem();

  Intersection<T>? get intersection {
    if (!_isInitialized) throw Exception('intersection is not initialized');
    return _intersection;
  }

  set intersection(Intersection<T>? i) {
    _intersection = i;
    _isInitialized = true;
  }

  bool get isInitialized => _isInitialized;

  void release() {
    _intersection = null;
    _isInitialized = false;
  }
}

class Intersection<T extends Point> implements Point {
  @override
  late double x;

  @override
  late double y;

  late Bisector<T> _line;
  late StepDirection _step;

  Intersection(Point intersection, Bisector<T> b, [Line? other, Point? center]) {
    _line = b;
    x = intersection.x;
    y = intersection.y;

    if (other != null && center != null) {
      var dx = b.line.b;
      var dy = -b.line.a;

      if (dx < 0 || (dx == 0 && dy < 0)) {
        dx *= -1;
        dy *= -1;
      }

      final p = Point(intersection.x + dx, intersection.y + dy);
      _step = line_util.onSameSide(other, p, center) ? StepDirection.down : StepDirection.up;
    } else {
      _step = StepDirection.zero;
    }
  }

  final _previous = IntersectionItem<T>();
  final _next = IntersectionItem<T>();
  int _index = 0;

  int get index => _index;
  Bisector<T> get line => _line;
  StepDirection get step => _step;

  Node<T>? _node;

  bool get hasPrevious {
    return _previous.intersection != null;
  }

  Intersection<T> get previous {
    if (_previous.intersection == null) throw Exception('previous is null');
    return _previous.intersection!;
  }

  set previous(Intersection<T> intersection) {
    _previous.intersection = intersection;
  }

  bool get hasNext {
    return _next.intersection != null;
  }

  Intersection<T> get next {
    if (_next.intersection == null) throw Exception('next is null');
    return _next.intersection!;
  }

  set next(Intersection<T> intersection) {
    _next.intersection = intersection;
  }

  Node<T> get node {
    if (_node == null) throw Exception('node is not initialized');
    return _node!;
  }

  set node(Node<T> n) {
    if (_node != null) throw Exception('node is already initialized');
    _node = n;
  }

  void insert(Intersection<T>? previous, Intersection<T>? next, int index) {
    _previous.intersection = previous;
    _next.intersection = next;

    if (_previous.intersection != null) {
      _previous.intersection!.next = this;
    }

    if (_next.intersection != null) {
      _next.intersection!.previous = this;
      _next.intersection!.incrementIndex();
    }

    _index = index;
  }

  void incrementIndex() {
    _index++;
    if (_next.intersection != null) {
      _next.intersection!.incrementIndex();
    }
  }

  bool isNeighbor(Point p) {
    return (hasNext && point.equals(_next.intersection!, p)) || (hasPrevious && point.equals(_previous.intersection!, p));
  }

  bool hasNeighbor(StepDirection step) {
    if (step == StepDirection.zero && _step == StepDirection.zero) return true;
    if (step != StepDirection.zero && _step != StepDirection.zero) return (step == _step) ? hasNext : hasPrevious;
    return false;
  }

  Intersection<T> neighbor(StepDirection step) {
    if (step == StepDirection.zero && _step == StepDirection.zero) {
      if (_previous.intersection != null) return _previous.intersection!;
      if (_next.intersection != null) return _next.intersection!;
    }

    if (step != StepDirection.zero && _step != StepDirection.zero) {
      return (step == _step) ? _next.intersection! : _previous.intersection!;
    }

    throw Exception('no neighbor');
  }

  void onSolved() {
    _line.onIntersectionSolved(this);
  }

  void release() {
    _previous.release();
    _next.release();
    if (_node != null) {
      _node!.release();
      _node = null;
    }
  }
}

class Bisector<T extends Point> {
  late Line _line;
  final List<Intersection<T>> _intersections = [];
  bool _isBoundary = true;
  T? _delaunayPoint;

  Line get line => _line;
  bool get isBoundary => _isBoundary;
  T? get delaunayPoint => _delaunayPoint;
  List<Intersection<T>> get intersections => _intersections;

  Bisector(Line bisector, [T? p]) {
    _line = bisector;

    if (p != null) {
      _delaunayPoint = p;
      _isBoundary = false;
    }
  }

  int _solvedPointIndexFrom = -1 >>> 1; // Max int value
  int _solvedPointIndexTo = -1;

  void inspectBoundary(Edge boundary) {
    final p = line_util.getIntersection(_line, boundary);
    if (p != null) {
      addIntersection(Intersection(p, this));
    }
  }

  void onIntersectionSolved(Intersection<T> intersection) {
    final index = intersection.index;
    _solvedPointIndexFrom = math.min(_solvedPointIndexFrom, index);
    _solvedPointIndexTo = math.max(_solvedPointIndexTo, index);
  }

  void addIntersection(Intersection<T> intersection) {
    final size = _intersections.length;
    final index = addIntersectionAt(intersection, 0, size);
    intersection.insert(
      index > 0 ? _intersections[index - 1] : null,
      index < size ? _intersections[index] : null,
      index,
    );

    _intersections.insert(index, intersection);
    if (_solvedPointIndexFrom < _solvedPointIndexTo) {
      if (index <= _solvedPointIndexFrom) {
        _solvedPointIndexFrom++;
        _solvedPointIndexTo++;
      } else if(index <= _solvedPointIndexTo) {
        throw Exception('new intersection added to solved range');
      }
    }
  }

  int addIntersectionAt(Point p, int indexFrom, int indexTo) {
    if (indexFrom == indexTo) return indexFrom;

    final mid = ((indexFrom + indexTo -1) / 2).floor();
    final r = point.compare(p, _intersections[mid]);

    if (r < 0) return addIntersectionAt(p, indexFrom, mid);
    if (r > 0) return addIntersectionAt(p, mid + 1, indexTo);

    throw Exception('same point already added in this bisector');
  }

  void release() {
    for (var i in _intersections) {
      i.release();
    }
    _intersections.clear();
  }
}

typedef NodeList<T extends Point> = List<List<Node<T>>>;
typedef PointProvider<T extends Point> = Future<List<T>> Function(T p);
typedef Callback = void Function(int index, List<Point> polygon);

class Voronoi<T extends Point> {
  late T _center;
  late Triangle _container;
  late PointProvider<T> _provider;

  final _bisectors = <Bisector<T>>[];
  final _delaunayPoints = ObjectSet(point.equals, point.hashCode);
  final _stopwatch = Stopwatch();
  bool _running = false;
  int _targetIndex = 1;
  Callback? _callback;

  Voronoi(T center, Triangle frame, PointProvider<T> provider) {
    _center = center;
    _container = frame;
    _provider = provider;
  }

  Future<NodeList<T>> execute(int level, Callback? callback) async {
    if (_running) throw Exception('already running');
    _running = true;

    _targetIndex = level;

    if (_stopwatch.isRunning) {
      _stopwatch.stop();
      _stopwatch.reset();
    }
    _stopwatch.start();

    _callback = callback;
    _bisectors.clear();
    _addBoundary(line_util.init(_container.a, _container.b));
    _addBoundary(line_util.init(_container.b, _container.c));
    _addBoundary(line_util.init(_container.c, _container.a));

    _delaunayPoints.clear();
    _delaunayPoints.add(_center);

    return _searchPolygon(1, []);
  }

  Future<NodeList<T>> _searchPolygon(int index, NodeList<T> result) async {
    final loopTime = Stopwatch()..start();
    final previousPolygon = index == 1 ? null : result.last;

    await _expandDelaunayPoints(previousPolygon);

    final polygon = _traverse(previousPolygon);
    for (var node in polygon) {
      node.onSolved(index);
    }

    logger.debug('searching polygon: $index, time: ${loopTime.elapsedMilliseconds}ms');

    if (_callback != null) {
      _callback!(index, polygon);
    }

    if (index < _targetIndex) {
      return _searchPolygon(index + 1, [...result, polygon]);
    } else {
      for(var n in _bisectors) {
        n.release();
      }

      logger.debug('searching polygon finished: ${_stopwatch.elapsedMilliseconds}ms');
      _running = false;

      return [...result, polygon];
    }
  }

  List<Node<T>> _traverse(List<Node<T>>? previousPolygon) {
    Node<T>? next;
    Node<T>? previous;

    if (previousPolygon == null) {
      final history = ObjectSet(point.equals, point.hashCode);
      final sample = _bisectors[0];
      next = sample.intersections[1].node;
      previous = sample.intersections[0].node;

      while (history.add(next!)) {
        final current = next;
        next = current.nextDown(previous!);
        previous = current;
      }
    } else {
      previous = previousPolygon.last;
      for (var n in previousPolygon) {
        next = n.nextUp(previous!);
        previous = n;
        if (next != null && !next.hasSolved()) break;
      }
    }

    if (next == null || previous == null || next.hasSolved()) {
      throw Exception('fail to traverse polygon');
    }

    final start = next;
    final polygon = [start];
    while (true) {
      final current = next;
      next = current!.next(previous!);
      previous = current;

      if (point.equals(start, next)) break;
      if (polygon.any((x) => point.equals(x, next!))) throw Exception('duplicated point in polygon');
      polygon.add(next);
    }

    return polygon;
  }

  Future<void> _expandDelaunayPoints([List<Node<T>>? polygon]) async {
    final queue = polygon != null ? polygon.map((x) => [x.p1.line.delaunayPoint, x.p2.line.delaunayPoint]).expand((x) => x).where((x) => x != null).toList() : [_center];

    for (var p in queue) {
      final neighbors = (await _provider(p!)).where((x) => _delaunayPoints.add(x)).toList();
      for (var n in neighbors) {
        _addBisector(n);
      }
    }
  }

  void _addBoundary(Line self) {
    final boundary = Bisector<T>(self);
    for (var preexist in _bisectors) {
      final p = line_util.getIntersection(boundary.line, preexist.line);
      if (p == null) throw Exception('no intersection');

      final a = Intersection(p, boundary);
      final b = Intersection(p, preexist);
      final node = Node(p, a, b);

      a.node = node;
      b.node = node;

      boundary.addIntersection(a);
      preexist.addIntersection(b);
    }

    _bisectors.add(boundary);
  }

  void _addBisector(T intersection) {
    final bisector = Bisector(line_util.getPerpendicularBisector(intersection, _center), intersection);
    for (var preexist in _bisectors) {
      final p = line_util.getIntersection(bisector.line, preexist.line);
      if (p == null || !triangle.containsPoint(_container, p, error)) continue;

      final a = Intersection(p, bisector, preexist.line, _center);
      final b = Intersection(p, preexist, bisector.line, _center);
      final node = Node(p, a, b);

      a.node = node;
      b.node = node;

      bisector.addIntersection(a);
      preexist.addIntersection(b);
    }

    _bisectors.add(bisector);
  }
}