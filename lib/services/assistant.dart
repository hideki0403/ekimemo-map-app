import 'dart:convert';
import 'dart:ui';
import 'config.dart';
import 'utils.dart';

class AssistantFlow {
  static List<AssistantFlowItem> get() {
    final data = SystemState.assistantFlow;
    return AssistantFlowUtils.parse(data);
  }

  static void set(List<AssistantFlowItem> items) {
    final data = AssistantFlowUtils.stringify(items);
    SystemState.setString('assistant_flow', data);
  }

  static void push(AssistantFlowItem item) {
    final items = get();
    items.add(item);
    set(items);
  }
}

class AssistantFlowUtils {
  static List<AssistantFlowItem> parse(String data) {
    final List<dynamic> jsonList = jsonDecode(data);
    return jsonList.map((json) {
      switch (json['type']) {
        case 'tap':
          return TapItem.fromJson(json);
        case 'tapRect':
          return TapRectItem.fromJson(json);
        case 'wait':
          return WaitItem.fromJson(json);
        default:
          throw Exception('Unknown type');
      }
    }).toList();
  }

  static String stringify(List<AssistantFlowItem> data) {
    final List<Map<String, dynamic>> jsonList = data.map((item) => item.toJson()).toList();
    return jsonEncode(jsonList);
  }
}

abstract class AssistantFlowItem {
  String get type;
  String get content;

  Map<String, dynamic> toJson();
}

class TapItem extends AssistantFlowItem {
  TapItem(this.x, this.y);

  @override
  final String type = 'tap';

  @override
  String get content => '(${x.toInt()}, ${y.toInt()})';

  final double x;
  final double y;

  @override
  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'content': '$x,$y',
    };
  }

  factory TapItem.fromJson(Map<String, dynamic> json) {
    final raw = json['content'].split(',').map((e) => double.parse(e)).toList();
    return TapItem(raw[0], raw[1]);
  }
}

class TapRectItem extends AssistantFlowItem {
  TapRectItem(this.value);

  @override
  final String type = 'tapRect';

  @override
  String get content => '(${value.left.toInt()}, ${value.top.toInt()}) - (${value.right.toInt()}, ${value.bottom.toInt()})';

  final Rect value;

  @override
  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'content': rectToString(value),
    };
  }

  factory TapRectItem.fromJson(Map<String, dynamic> json) {
    return TapRectItem(stringToRect(json['content']));
  }
}

class WaitItem extends AssistantFlowItem {
  WaitItem(this.value);

  @override
  final String type = 'wait';

  @override
  String get content => '${value}ms';

  final int value;

  @override
  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'content': value,
    };
  }

  factory WaitItem.fromJson(Map<String, dynamic> json) {
    return WaitItem(json['content']);
  }
}

class WaitRandomItem extends AssistantFlowItem {
  WaitRandomItem(this.value);

  @override
  final String type = 'waitRandom';

  @override
  String get content => 'max ${value}ms';

  final int value;

  @override
  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'content': value,
    };
  }

  factory WaitRandomItem.fromJson(Map<String, dynamic> json) {
    return WaitRandomItem(json['content']);
  }
}