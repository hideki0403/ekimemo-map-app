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
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (widget.caption != null) ...[
            Text(widget.caption!),
            const SizedBox(height: 8),
          ],
          for (final key in selectedValue.keys)
            CheckboxListTile(
              title: Text(key),
              value: selectedValue[key] ?? false,
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
        TextButton(
          onPressed: () {
            Navigator.of(context).pop(selectedValue);
          },
          child: const Text('OK'),
        ),
      ],
    );
  }
}