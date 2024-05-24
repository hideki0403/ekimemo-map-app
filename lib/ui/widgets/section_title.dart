import 'package:flutter/material.dart';

class SectionTitle extends StatelessWidget {
  final String title;
  const SectionTitle({super.key, required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 16, right: 16, top: 24, bottom: 8),
      child: Text(title, style: TextStyle(color: Theme.of(context).colorScheme.primary)),
    );
  }
}