import 'package:flutter/material.dart';

class SelectDialog extends StatefulWidget {
  final String? title;
  final Map<String, String> data;
  final String? defaultValue;
  final String? caption;
  final bool? noRadio;
  final bool? showOkButton;
  final Function(String?)? onChanged;

  const SelectDialog({super.key, required this.data, this.defaultValue, this.title, this.caption, this.noRadio, this.onChanged, this.showOkButton });

  @override
  State<SelectDialog> createState() => _SelectDialogState();
}

class _SelectDialogState extends State<SelectDialog> {
  String? selectedValue;

  @override
  void initState() {
    super.initState();
    selectedValue = widget.defaultValue;
  }

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
              groupValue: selectedValue,
              onChanged: (value) {
                if (widget.onChanged != null) widget.onChanged!(value);
                if (widget.showOkButton != true) {
                  Navigator.of(context).pop(value);
                } else {
                  setState(() {
                    selectedValue = value;
                  });
                }
              },
            ) : ListTile(
              title: Text(widget.data[key]!),
              onTap: () {
                if (widget.showOkButton != true) {
                  Navigator.of(context).pop(key);
                } else {
                  setState(() {
                    selectedValue = key;
                  });
                }
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
        if (widget.showOkButton == true) TextButton(
          onPressed: () {
            Navigator.of(context).pop(selectedValue);
          },
          child: const Text('OK'),
        ),
      ],
    );
  }
}