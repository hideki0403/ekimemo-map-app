import 'dart:async';
import 'package:flutter/material.dart';

class RelativeTime extends StatefulWidget {
  final DateTime? time;
  final String? prefix;
  final TextScaler? textScaler;

  const RelativeTime({required this.time, this.prefix, this.textScaler, super.key});

  @override
  State<RelativeTime> createState() => _RelativeTimeState();
}

class _RelativeTimeState extends State<RelativeTime> {
  String _timeString = '不明';
  Timer? _timer;

  @override
  initState() {
    super.initState();

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!context.mounted) return timer.cancel();
      if (widget.time == null) {
        setState(() {
          _timeString = '不明';
        });
        return;
      }

      final diff = DateTime.now().difference(widget.time!);

      if (diff.inSeconds < 60) {
        setState(() {
          _timeString = '${diff.inSeconds}秒前';
        });
        return;
      }

      if (diff.inMinutes < 60) {
        setState(() {
          _timeString = '${diff.inMinutes}分前';
        });
        return;
      }

      if (diff.inHours < 24) {
        setState(() {
          _timeString = '${diff.inHours}時間前';
        });
        return;
      }

      setState(() {
        _timeString = '24時間以上前';
      });
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Text('${widget.prefix}$_timeString', textScaler: widget.textScaler);
  }
}