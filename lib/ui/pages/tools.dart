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
                _ToolItem(title: '検索', description: '駅の検索を行うことが出来ます', icon: Icons.search_rounded, path: '/search'),
                SizedBox(height: 12),
                _ToolItem(title: 'アクセス履歴', description: 'アクセスした駅の履歴を確認することが出来ます', icon: Icons.history_rounded, path: '/history'),
                SizedBox(height: 12),
                _ToolItem(title: '移動ログ', description: '移動ログを確認することが出来ます', icon: Icons.route_rounded, path: '/movement-log'),
                SizedBox(height: 12),
                _ToolItem(title: 'ルート探索', description: 'GPXファイルを読み込み、通過する駅の一覧を表示することが出来ます', icon: Icons.directions_rounded, path: '/route-search'),
                SizedBox(height: 12),
                _ToolItem(title: 'インターバルタイマー', description: 'インターバルタイマーを設定することが出来ます', icon: Icons.timer_rounded, path: '/interval-timer'),
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
            spacing: 12,
            children: [
              Row(
                spacing: 12,
                children: [
                  Icon(icon, size: 24),
                  Text(title, textScaler: const TextScaler.linear(1.2)),
                ],
              ),
              Text(description),
            ],
          ),
        )
      ),
    );
  }
}
