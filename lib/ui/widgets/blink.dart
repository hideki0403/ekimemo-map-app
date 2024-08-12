import 'package:flutter/material.dart';

class Blink extends StatefulWidget {
  final Widget child;
  final Duration duration;
  final bool enabled;

  const Blink({required this.child, this.duration = const Duration(milliseconds: 500), this.enabled = true, super.key});

  @override
  State<StatefulWidget> createState() => _BlinkState();
}

class _BlinkState extends State<Blink> {
  bool _visible = true;

  @override
  void initState() {
    super.initState();
    _toggleVisibility();
  }

  @override
  void didUpdateWidget(covariant Blink oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.enabled != widget.enabled) {
      _toggleVisibility();
    }
  }

  void _toggleVisibility() {
    if (widget.enabled) {
      Future.delayed(widget.duration, () {
        if (mounted) {
          setState(() {
            _visible = !_visible;
            _toggleVisibility();
          });
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: !widget.enabled || _visible ? 1.0 : 0.0,
      child: widget.child,
    );
  }
}
