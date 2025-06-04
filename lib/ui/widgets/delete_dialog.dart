import 'package:flutter/material.dart';
import 'dialog_template.dart';

class DeleteDialog extends StatefulWidget {
  final String? title;
  final Map<String, String> data;
  final Future<bool> Function(String key)? onDelete;
  final String? caption;
  final IconData? icon;

  const DeleteDialog({super.key, this.title, required this.data, required this.onDelete, this.caption, this.icon});

  @override
  State<DeleteDialog> createState() => _DeleteDialogState();
}

class _DeleteDialogState extends State<DeleteDialog> {
  final Map<String, String> data = {};

  @override
  void initState() {
    super.initState();
    setState(() {
      data.addAll(widget.data);
    });
  }

  @override
  Widget build(BuildContext context) {
    return DialogTemplate(
      title: widget.title,
      icon: widget.icon,
      padding: const EdgeInsets.fromLTRB(0, 16, 0, 24),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (widget.caption != null) ...[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(widget.caption!),
            ),
            const SizedBox(height: 16),
          ],
          if (data.isEmpty) ...[
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 24),
              child: Text('削除できるデータがありません'),
            ),
          ]
          else for (final entry in data.entries) ...[
            ListTile(
              title: Text(entry.value),
              trailing: IconButton(
                icon: Icon(Icons.delete_forever_rounded, color: Theme.of(context).colorScheme.error),
                onPressed: () async {
                  if (widget.onDelete != null) {
                    final result = await widget.onDelete!(entry.key);
                    if (result) {
                      setState(() {
                        data.remove(entry.key);
                      });
                    }
                  }
                },
              ),
            ),
          ],
        ]
      ),
      actions: [
        ElevatedButton(
          onPressed: () {
            Navigator.of(context).pop();
          },
          child: const Text('閉じる'),
        ),
      ],
    );
  }
}