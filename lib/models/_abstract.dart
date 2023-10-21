abstract class AbstractModel<T> {
  T fromMap(Map<String, dynamic> map);
  T fromJson(Map<String, dynamic> json);
  Map<String, dynamic> toMap();
}