import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:ekimemo_map/services/assistant.dart';
import 'package:ekimemo_map/services/utils.dart';
import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';
import 'package:ekimemo_map/ui/widgets/reorderable_item.dart';

class AssistantFlowView extends StatefulWidget {
  const AssistantFlowView({super.key});

  @override
  State<StatefulWidget> createState() => _AssistantFlowViewState();
}

class _AssistantFlowViewState extends State<AssistantFlowView> {
  List<AssistantFlowItem> _data = [];

  @override
  void initState() {
    super.initState();
    setState(() {
      _data = AssistantFlow.get();
    });
  }

  Future<void> addTapItem() async {
    final result = await context.push('/assistant-choose-rect');
    if (result != null && result is Rect) {
      setState(() {
        _data.add(TapItem(result.top + result.bottom / 2, result.left + result.right / 2));
        AssistantFlow.set(_data);
      });
    }
  }

  Future<void> addTapRectItem() async {
    final result = await context.push('/assistant-choose-rect');
    if (result != null && result is Rect) {
      setState(() {
        _data.add(TapRectItem(result));
        AssistantFlow.set(_data);
      });
    }
  }

  Future<void> addWaitItem() async {
    final result = await showEditorDialog(
      title: 'wait',
      suffix: 'ms',
      type: EditorDialogType.integer,
    );

    if (result != null) {
      setState(() {
        _data.add(WaitItem(int.parse(result)));
        AssistantFlow.set(_data);
      });
    }
  }

  Future<void> waitRandomItem() async {
    final result = await showEditorDialog(
      title: 'waitRandom (max)',
      suffix: 'ms',
      type: EditorDialogType.integer,
    );

    if (result != null) {
      setState(() {
        _data.add(WaitRandomItem(int.parse(result)));
        AssistantFlow.set(_data);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final result = await showSelectDialog(
            title: 'Type',
            data: Map.fromEntries(['tap', 'tapRect', 'wait', 'waitRandom'].map((e) => MapEntry(e, e))),
            noRadio: true,
          );

          if (result != null) {
            switch(result) {
              case 'tap': addTapItem(); break;
              case 'tapRect': addTapRectItem(); break;
              case 'wait': addWaitItem(); break;
              case 'waitRandom': waitRandomItem(); break;
            }
          }
        },
        child: const Icon(Icons.add),
      ),
      body: CustomScrollView(
        slivers: [
          const SliverAppBar.large(
            title: Text('Assistant Flow'),
          ),
          SliverPadding(
            padding: const EdgeInsets.only(left: 20, right: 20),
            sliver: SliverReorderableList(
              itemBuilder: (_, index) => ReorderableDelayedDragStartListener(
                index: index,
                key: Key('$index'),
                child: ReorderableItem(
                  title: _data[index].type,
                  description: _data[index].content,
                  id: index,
                  onDeleted: (int id) {
                    setState(() {
                      _data.removeAt(id);
                      AssistantFlow.set(_data);
                    });
                  },
                ),
              ),
              itemCount: _data.length,
              onReorder: (oldIndex, newIndex) {
                setState(() {
                  if (oldIndex < newIndex) {
                    newIndex -= 1;
                  }
                  _data.insert(newIndex, _data.removeAt(oldIndex));
                  AssistantFlow.set(_data);
                });
              },
              proxyDecorator: (widget, _, __) {
                return Opacity(opacity: 0.5, child: widget);
              },
            ),
          ),
        ],
      ),
    );
  }
}