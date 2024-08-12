import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class ToolsView extends StatelessWidget {
  const ToolsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ツール'),
      ),
      body: const CustomScrollView(
        slivers: [
          SliverPadding(
            padding: EdgeInsets.only(left: 12, right: 12, top: 8, bottom: 64),
            sliver: SliverList(
              delegate: SliverChildListDelegate.fixed([
                _ToolItem(title: '検索', description: '駅の検索を行うことが出来ます', icon: Icons.search, path: '/search'),
                SizedBox(height: 12),
                _ToolItem(title: 'アクセス履歴', description: 'アクセスした駅の履歴を確認することが出来ます', icon: Icons.history, path: '/history'),
                SizedBox(height: 12),
                _ToolItem(title: 'ルート探索', description: 'GPXファイルを読み込み、通過する駅の一覧を表示することが出来ます', icon: Icons.directions, path: '/route-search'),
                SizedBox(height: 12),
                _ToolItem(title: 'インターバルタイマー', description: 'インターバルタイマーを設定することが出来ます', icon: Icons.timer, path: '/interval-timer'),
              ]),
            ),
          ),
        ],
      ),
    );
  }
}

class _ToolItem extends StatelessWidget {
  const _ToolItem({required this.title, required this.icon, required this.path, required this.description});

  final String title;
  final IconData icon;
  final String path;
  final String description;

  @override
  Widget build(BuildContext context) {
    final border = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(24),
    );
    return Card(
      shape: border,
      child: InkWell(
        customBorder: border,
        onTap: () {
          context.push(path);
        },
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(icon, size: 24),
                  const SizedBox(width: 12),
                  Text(title, textScaler: const TextScaler.linear(1.2)),
                ],
              ),
              const SizedBox(height: 12),
              Text(description),
            ],
          ),
        )
      ),
    );
  }
}
