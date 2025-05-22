import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import 'package:ekimemo_map/ui/widgets/editor_dialog.dart';
import 'package:ekimemo_map/services/log.dart';
import 'package:ekimemo_map/services/utils.dart';

DateFormat _dateFormat = DateFormat('yyyy-MM-dd HH:mm:ss');

class LogView extends StatefulWidget {
  const LogView({super.key});

  @override
  State<StatefulWidget> createState() => _LogViewState();
}

class _LogViewState extends State<LogView> {
  final List<LogType> _filterTypes = [LogType.debug, LogType.info, LogType.warning, LogType.error];
  String? _filterTagName;

  List<LogObject> filter(List<LogObject> logs) {
    logs = logs.where((log) => _filterTypes.contains(log.type)).toList();

    if (_filterTagName != null) {
      logs = logs.where((log) => log.tag.contains(_filterTagName!)).toList();
    }

    return logs;
  }

  @override
  Widget build(BuildContext context) {
    final manager = Provider.of<LogStateNotifier>(context);
    final logs = filter(manager.logs);

    return Scaffold(
      appBar: AppBar(
        title: const Text('ログ'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final result = await showSelectDialog(
            title: 'Log filter',
            caption: 'Select filter type',
            data: Map.fromEntries(['type', 'tag'].map((e) => MapEntry(e, e))),
            noRadio: true,
          );

          switch(result) {
            case 'type':
              final type = await showCheckboxDialog(
                title: 'Type filter',
                caption: 'Select types to show',
                data: Map.fromEntries(LogType.values.map((e) => MapEntry(e.name, _filterTypes.contains(e)))),
              );

              if (type != null && context.mounted) {
                setState(() {
                  _filterTypes.clear();
                  _filterTypes.addAll(LogType.values.where((e) => type[e.name] ?? false));
                });
              }
              break;

            case 'tag':
              final tag = await showEditorDialog(
                title: 'Tag filter',
                type: EditorDialogType.text,
              );

              if (tag != null && context.mounted) {
                setState(() {
                  _filterTagName = tag;
                });
              }
              break;
          }
        },
        child: const Icon(Icons.filter_list),
      ),
      body: CustomScrollView(
        slivers: [
          SliverList(
            delegate: SliverChildListDelegate.fixed([
              ListView.separated(
                padding: EdgeInsets.zero,
                shrinkWrap: true,
                reverse: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: logs.length,
                itemBuilder: (context, index) {
                  return _LogObject(log: logs[index]);
                },
                separatorBuilder: (context, index) {
                  return const Divider(height: 8);
                },
              ),
            ]),
          ),
        ],
      ),
    );
  }
}

class _LogObject extends StatelessWidget {
  final LogObject log;

  const _LogObject({ required this.log });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      customBorder: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      onLongPress: () {
        Clipboard.setData(ClipboardData(text: log.object.toString()));
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Copied to clipboard')));
      },
      child: Padding(
        padding: const EdgeInsets.only(left: 12, top: 6, bottom: 6, right: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          spacing: 4,
          children: [
            Row(
              children: [
                Text(log.type.name.toUpperCase(), style: TextStyle(color: log.type.color), textScaler: const TextScaler.linear(0.9)),
                const SizedBox(width: 8),
                Expanded(child: Text(log.tag, textScaler: const TextScaler.linear(0.9))),
                Text(_dateFormat.format(log.time), style: const TextStyle(color: Colors.grey), textScaler: const TextScaler.linear(0.9)),
              ],
            ),
            Text(log.object.toString()),
          ],
        ),
      ),
    );
  }
}