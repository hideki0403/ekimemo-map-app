import 'package:flutter/material.dart';

class CheckboxDialog extends StatefulWidget {
  final String? title;
  final Map<String, bool> data;
  final String? caption;

  const CheckboxDialog({super.key, required this.data, this.title, this.caption});

  @override
  State<CheckboxDialog> createState() => _CheckboxDialogState();
}

class _CheckboxDialogState extends State<CheckboxDialog> {
  Map<String, bool> selectedValue = {};

  @override
  void initState() {
    super.initState();
    setState(() {
      selectedValue = widget.data;
    });
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: widget.title != null ? Text(widget.title!) : null,
      contentPadding: const EdgeInsets.fromLTRB(0, 16, 0, 24),
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
          for (final key in selectedValue.keys)
            CheckboxListTile(
              title: Text(key),
              value: selectedValue[key] ?? false,
              contentPadding: const EdgeInsets.symmetric(horizontal: 24),
              onChanged: (value) {
                if (value == null) return;
                setState(() {
                  selectedValue[key] = value;
                });
              },
            ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            Navigator.of(context).pop();
          },
          child: const Text('キャンセル'),
        ),
        ElevatedButton(
          onPressed: () {
            Navigator.of(context).pop(selectedValue);
          },
          child: const Text('OK'),
        ),
      ],
    );
  }
}