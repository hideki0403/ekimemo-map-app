import 'package:flutter/material.dart';

class SelectDialog extends StatefulWidget {
  final String? title;
  final Map<String, String> data;
  final String? defaultValue;
  final String? caption;
  final bool? noRadio;

  const SelectDialog({super.key, required this.data, this.defaultValue, this.title, this.caption, this.noRadio });

  @override
  State<SelectDialog> createState() => _SelectDialogState();
}

class _SelectDialogState extends State<SelectDialog> {
  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: widget.title != null ? Text(widget.title!) : null,
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (widget.caption != null) Text(widget.caption!),
          for (final key in widget.data.keys)
            widget.noRadio != true ? RadioListTile<String>(
              title: Text(widget.data[key]!),
              value: key,
              groupValue: widget.defaultValue,
              onChanged: (value) {
                Navigator.of(context).pop(value);
              },
            ) : ListTile(
              title: Text(widget.data[key]!),
              onTap: () {
                Navigator.of(context).pop(key);
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
        )
      ],
    );
  }
}