import 'package:flutter/material.dart';

class SectionTitle extends StatelessWidget {
  final String title;
  final void Function()? onTap;
  const SectionTitle({super.key, required this.title, this.onTap});

  Widget w(BuildContext context) => Padding(
    padding: const EdgeInsets.only(left: 16, right: 16, top: 24, bottom: 8),
    child: Text(title, style: TextStyle(color: Theme.of(context).colorScheme.primary)),
  );

  @override
  Widget build(BuildContext context) {
    return onTap == null ? w(context) : GestureDetector(
      onTap: onTap,
      child: w(context),
    );
  }
}