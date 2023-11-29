import 'package:flutter/material.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'services/config.dart';
import 'services/state.dart';
import 'services/gps.dart';
import 'services/station.dart';
import 'services/notification.dart';

import 'ui/pages/home.dart';
import 'ui/pages/settings.dart';
import 'ui/pages/map.dart';
import 'ui/pages/station_detail.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final stationManager = StationManager();
  final configProvider = ConfigProvider();
  await configProvider.init();
  Config.init(configProvider);

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => StateManager()),
        ChangeNotifierProvider(create: (_) => configProvider),
        ChangeNotifierProvider(create: (_) {
          final gpsManager = GpsManager();
          gpsManager.setStationManager(stationManager);
          return gpsManager;
        }),
        ChangeNotifierProvider(create: (_) {
          stationManager.initialize();
          return stationManager;
        }),
      ],
      child: const Root(),
    ),
  );
}

class Root extends StatelessWidget {
  const Root({super.key});

  @override
  Widget build(BuildContext context) {
    // TODO: 起動時に駅情報のアップデートを確認
    NotificationManager().init();
    return DynamicColorBuilder(
      // TODO: テーマを変えられるように
      builder: (lightColorScheme, darkColorScheme) => MaterialApp.router(
        theme: _buildTheme(Brightness.light, lightColorScheme),
        darkTheme: _buildTheme(Brightness.dark, darkColorScheme),
        routerConfig: GoRouter(
          initialLocation: '/',
          routes: [
            GoRoute(
              path: '/',
              builder: (context, state) => const HomeView(),
            ),
            GoRoute(
              path: '/settings',
              builder: (context, state) => const SettingsView(),
            ),
            GoRoute(
              path: '/map',
              builder: (context, state) => const MapView(),
            ),
            GoRoute(
              path: '/station',
              builder: (context, state) {
                return StationDetailView(stationId: state.uri.queryParameters['id']);
              },
            ),
          ],
        ),
      ),
    );
  }
}

ThemeData _buildTheme(Brightness brightness, ColorScheme? colorScheme) {
  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: colorScheme,
  );
}

