class Point {
  double x;
  double y;
  Point(this.x, this.y);
}

class Line {
  double a;
  double b;
  double c;
  Line(this.a, this.b, this.c);
}

class Triangle {
  Point a;
  Point b;
  Point c;
  Triangle(this.a, this.b, this.c);
}

class Edge {
  Point a;
  Point b;
  Edge(this.a, this.b);
}

class Rect {
  double left;
  double top;
  double right;
  double bottom;
  Rect(this.left, this.top, this.right, this.bottom);
}