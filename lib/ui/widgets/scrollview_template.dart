import 'package:flutter/material.dart';

class ScrollViewTemplate extends StatelessWidget {
  final SliverChildBuilderDelegate delegate;
  final double? itemHeight;
  final Widget empty;
  final bool isEmpty;
  final EdgeInsets sliverPadding;
  final EdgeInsets emptyBoxPadding;
  final List<Widget>? slivers;

  const ScrollViewTemplate({
    super.key,
    required this.delegate,
    this.itemHeight,
    required this.empty,
    this.isEmpty = false,
    this.sliverPadding = const EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 32),
    this.emptyBoxPadding = const EdgeInsets.only(top: 36, bottom: 24, left: 12, right: 12),
    this.slivers = const [],
  });

  @override
  Widget build(BuildContext context) {
    return CustomScrollView(
      slivers: [
        ...slivers!,
        SliverPadding(
          padding: sliverPadding,
          sliver: !isEmpty ? itemHeight != null ? SliverFixedExtentList(
            itemExtent: itemHeight!,
            delegate: delegate,
          ) : SliverList(delegate: delegate) : SliverToBoxAdapter(
            child: SizedBox(
            width: double.infinity,
            child: Padding(
              padding: emptyBoxPadding,
              child: empty,
            ),
          )),
        ),
      ],
    );
  }
}
