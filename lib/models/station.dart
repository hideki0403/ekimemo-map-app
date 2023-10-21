import '_abstract.dart';
import 'dart:convert';

enum StationAttr { eco, heat, cool, unknown }

class Station extends AbstractModel  {
  late final int code;
  late final String id;
  late final String name;
  late final String originalName;
  late final String nameKana;
  late final bool closed;
  late final double lat;
  late final double lng;
  late final int prefecture;
  late final List<int> lines;
  late final StationAttr attr;
  late final String postalCode;
  late final String address;
  late final List<int> next;
  late final Map<String, dynamic> voronoi;

  @override
  Station fromMap(Map<String, dynamic> map) {
    final station = Station();
    station.code = map['code'];
    station.id = map['id'];
    station.name = map['name'];
    station.originalName = map['original_name'];
    station.nameKana = map['name_kana'];
    station.closed = map['closed'] == 1;
    station.lat = map['lat'];
    station.lng = map['lng'];
    station.prefecture = map['prefecture'];
    station.lines = jsonDecode(map['lines']).cast<int>() as List<int>;
    station.attr = StationAttr.values.byName(map['attr']);
    station.next = jsonDecode(map['next']).cast<int>() as List<int>;
    station.voronoi = jsonDecode(map['voronoi']).cast<String, dynamic>() as Map<String, dynamic>;
    return station;
  }

  @override
  Station fromJson(Map<String, dynamic> json) {
    final station = Station();
    station.code = json['code'];
    station.id = json['id'];
    station.name = json['name'];
    station.originalName = json['original_name'];
    station.nameKana = json['name_kana'];
    station.closed = json['closed'];
    station.lat = json['lat'];
    station.lng = json['lng'];
    station.prefecture = json['prefecture'];
    station.lines = json['lines'].cast<int>() as List<int>;
    station.attr = StationAttr.values.byName(json['attr']);
    station.next = json['next'].cast<int>() as List<int>;
    station.voronoi = json['voronoi'].cast<String, dynamic>() as Map<String, dynamic>;
    return station;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'code': code,
      'id': id,
      'name': name,
      'original_name': originalName,
      'name_kana': nameKana,
      'closed': closed ? 1 : 0,
      'lat': lat,
      'lng': lng,
      'prefecture': prefecture,
      'lines': jsonEncode(lines),
      'attr': attr.name,
      'next': jsonEncode(next),
      'voronoi': jsonEncode(voronoi),
    };
  }
}