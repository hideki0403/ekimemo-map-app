import 'package:flutter/material.dart';
import 'dialog_template.dart';

class CheckboxDialog extends StatefulWidget {
  final String? title;
  final Map<String, bool> data;
  final String? caption;
  final IconData? icon;

  const CheckboxDialog({super.key, required this.data, this.title, this.caption, this.icon});

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