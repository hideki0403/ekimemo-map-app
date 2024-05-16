import 'package:flutter/material.dart';
import 'package:ekimemo_map/services/utils.dart';

class ReorderableItem extends StatelessWidget {
  final String title;
  final String description;
  final int? id;
  final Function(int id)? onDeleted;

  const ReorderableItem({super.key, required this.title, required this.description, this.onDeleted, this.id});

  @override
  Widget build(BuildContext context) {
    return Card(
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Expanded(child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, textScaler: const TextScaler.linear(1.1)),
                  Opacity(opacity: 0.8, child: Text(description)),
                ],
              )),
              if (onDeleted != null && id != null) IconButton(
                onPressed: () async {
                  final result = await showConfirmDialog(
                    title: '削除',
                    caption: '削除しますか？'
                  );

                  if (result == true) {
                    onDeleted!(id!);
                  }
                },
                icon: const Icon(Icons.delete),
              ),
              const Icon(Icons.drag_handle),
            ],
          ),
        ),
    );
  }
}