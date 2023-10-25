import '_abstract.dart';
import 'dart:convert';

class Line extends AbstractModel {
  late final int code;
  late final String id;
  late final String name;
  late final String nameKana;
  late final int stationSize;
  late final int? companyCode;
  late final bool closed;
  late final String? color;
  late final List<Map<String, dynamic>> stationList;
  late final List<String> uniqueStationList;
  late final Map<String, dynamic>? polylineList;

  @override
  Line fromMap(Map<String, dynamic> map) {
    final line = Line();
    line.code = map['code'];
    line.id = map['id'];
    line.name = map['name'];
    line.nameKana = map['name_kana'];
    line.stationSize = map['station_size'];
    line.companyCode = map['company_code'];
    line.closed = map['closed'] == 1;
    line.color = map['color'];
    line.stationList = jsonDecode(map['station_list']).cast<Map<String, dynamic>>() as List<Map<String, dynamic>>;
    line.uniqueStationList = jsonDecode(map['unique_station_list']).cast<String>() as List<String>;
    line.polylineList = jsonDecode(map['polyline_list']);
    return line;
  }

  @override
  Line fromJson(Map<String, dynamic> json) {
    final line = Line();
    line.code = json['code'];
    line.id = json['id'];
    line.name = json['name'];
    line.nameKana = json['name_kana'];
    line.stationSize = json['station_size'];
    line.companyCode = json['company_code'];
    line.closed = json['closed'];
    line.color = json['color'];
    line.stationList = json['station_list'].cast<Map<String, dynamic>>() as List<Map<String, dynamic>>;
    line.uniqueStationList = [];
    line.polylineList = json['polyline_list'];
    return line;
  }

  @override
  Map<String, dynamic> toMap() {
    return {
      'code': code,
      'id': id,
      'name': name,
      'name_kana': nameKana,
      'station_size': stationSize,
      'company_code': companyCode,
      'closed': closed ? 1 : 0,
      'color': color,
      'station_list': jsonEncode(stationList),
      'unique_station_list': jsonEncode(uniqueStationList),
      'polyline_list': jsonEncode(polylineList),
    };
  }
}