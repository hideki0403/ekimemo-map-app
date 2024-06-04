typedef Comparator<T> = bool Function(T obj1, T obj2);
typedef HashGetter<T> = int Function(T obj);

int stringHash(String str) {
  var hash = 5381;
  var i = str.length - 1;
  while (i >= 0) {
    hash = (hash * 33) ^ str.codeUnitAt(i);
    i--;
  }

  return hash & hash;
}

class ObjectSet<T> {
  late Comparator<T> _comparator;
  late HashGetter<T> _hashGetter;
  late Map<int, List<T>> _map;

  ObjectSet(Comparator<T> comparator, HashGetter<T> hashGetter, [List<T>? collection]) {
    _comparator = comparator;
    _hashGetter = hashGetter;
    _map = {};
    if (collection != null) {
      for (var obj in collection) {
        add(obj);
      }
    }
  }

  bool add(T obj) {
    final hash = _hashGetter(obj);
    final list = _map[hash];

    if (list != null) {
      if (list.any((element) => _comparator(element, obj))) return false;
      list.add(obj);
      return true;
    }

    _map[hash] = [obj];
    return true;
  }

  bool has(T obj) {
    var list = _map[_hashGetter(obj)];
    return list != null && list.any((element) => _comparator(element, obj));
  }

  bool remove(T obj) {
    final hash = _hashGetter(obj);
    final list = _map[hash];
    if (list == null) return false;

    final filteredList = list.where((element) => !_comparator(element, obj)).toList();
    if (list.length == filteredList.length) return false;
    if (filteredList.isEmpty) {
      _map.remove(hash);
    } else {
      _map[hash] = filteredList;
    }

    return true;
  }

  int size() {
    return _map.values.fold(0, (prev, element) => prev + element.length);
  }

  void clear() {
    _map.clear();
  }

  Iterable<T> get iterator sync* {
    for (var list in _map.values) {
      for (var obj in list) {
        yield obj;
      }
    }
  }
}