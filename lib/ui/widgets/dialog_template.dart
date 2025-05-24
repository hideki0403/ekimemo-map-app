import 'package:flutter/material.dart';

class DialogTemplate extends StatelessWidget {
  final String? title;
  final Widget? content;
  final List<Widget>? actions;
  final EdgeInsets? padding;
  final IconData? icon;

  const DialogTemplate({
    super.key,
    this.title,
    this.content,
    this.actions,
    this.padding,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      icon: icon != null ? Icon(icon, size: 30) : null,
      title: title != null ? Text(title!) : null,
      titleTextStyle: Theme.of(context).textTheme.titleLarge,
      contentPadding: padding,
      content: Scrollbar(child: SingleChildScrollView(child: content)),
      actions: actions,
    );
  }
}