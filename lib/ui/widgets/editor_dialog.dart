import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

enum EditorDialogType {
  text,
  integer,
  double,
}

class EditorDialog extends StatefulWidget {
  final String? title;
  final String? data;
  final String? caption;
  final String? suffix;
  final EditorDialogType? type;

  const EditorDialog({super.key, required this.data, this.title, this.caption, this.suffix, this.type});

  @override
  State<EditorDialog> createState() => _EditorDialogState();
}

class _EditorDialogState extends State<EditorDialog> {
  final controller = TextEditingController();
  final focusNode = FocusNode();
  var keyboardType = TextInputType.text;
  var inputFormatters = <TextInputFormatter>[];

  @override
  void dispose() {
    controller.dispose();
    focusNode.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();

    switch (widget.type) {
      case EditorDialogType.integer:
        keyboardType = TextInputType.number;
        inputFormatters.add(FilteringTextInputFormatter.digitsOnly);
        break;
      case EditorDialogType.double:
        keyboardType = const TextInputType.numberWithOptions(decimal: true);
        inputFormatters.add(FilteringTextInputFormatter.allow(RegExp(r'^\d+\.?\d{0,4}')));
        break;
      default:
        break;
    }

    controller.text = widget.data ?? '';
    focusNode.addListener(() {
        if (focusNode.hasFocus) {
          controller.selection = TextSelection(baseOffset: 0, extentOffset: controller.text.length);
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: widget.title != null ? Text(widget.title!) : null,
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: controller,
            focusNode: focusNode,
            keyboardType: keyboardType,
            inputFormatters: inputFormatters,
            decoration: InputDecoration(
              suffixText: widget.suffix,
            ),
          ),
          if (widget.caption != null) const SizedBox(height: 16),
          if (widget.caption != null) Opacity(opacity: 0.8, child: Text(widget.caption!)),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            Navigator.of(context).pop(controller.text);
          },
          child: const Text('決定'),
        )
      ],
    );
  }
}