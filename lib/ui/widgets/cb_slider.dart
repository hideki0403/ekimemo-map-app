import 'package:flutter/material.dart';

class CbSlider extends StatefulWidget {
  const CbSlider({
    super.key,
    required this.defaultValue,
    required this.min,
    required this.max,
    required this.onChanged,
    this.disabled = false,
  });

  final int defaultValue;
  final int min;
  final int max;
  final ValueChanged<int> onChanged;
  final bool disabled;

  @override
  State<CbSlider> createState() => _CbSliderState();
}

class _CbSliderState extends State<CbSlider> {
  double _value = 0.0;
  int _currentMax = 0;

  @override
  void initState() {
    super.initState();
    resetValue();

    setState(() {
      _currentMax = widget.max;
    });
  }

  @override
  void didUpdateWidget(CbSlider oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.max != _currentMax) {
      resetValue();
      setState(() {
        _currentMax = widget.max;
      });
    }
  }

  void resetValue() {
    setState(() {
      _value = widget.defaultValue.toDouble();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Slider(
      value: _value,
      min: widget.min.toDouble(),
      max: widget.max.toDouble(),
      divisions: widget.max - widget.min,
      label: _value.toInt().toString(),
      onChanged: widget.disabled ? null : (value) {
        setState(() {
          _value = value;
        });
        widget.onChanged(value.toInt());
      },
    );
  }
}