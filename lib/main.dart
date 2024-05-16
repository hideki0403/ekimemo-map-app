import 'package:flutter/material.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import 'services/config.dart';
import 'services/gps.dart';
import 'services/station.dart';
import 'services/notification.dart';

import 'ui/pages/home.dart';
import 'ui/pages/settings.dart';
import 'ui/pages/map.dart';
import 'ui/pages/station_detail.dart';

final navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final configProvider = ConfigProvider();
  await configProvider.init();

  final systemStateProvider = SystemStateProvider();
  await systemStateProvider.init();

  Config.init(configProvider);
  SystemState.init(systemStateProvider);
  NotificationManager().initialize();

  final stationManager = StationManager();
  await stationManager.initialize();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => configProvider),
        ChangeNotifierProvider(create: (_) => systemStateProvider),
        ChangeNotifierProvider(create: (_) => stationManager),
        ChangeNotifierProvider(create: (_) => GpsManager()),
      ],
      child: const Root(),
    ),
  );
}

class Root extends StatelessWidget {
  const Root({super.key});

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(
      // TODO: テーマを変えられるように
      builder: (lightColorScheme, darkColorScheme) => MaterialApp.router(
        theme: _buildTheme(Brightness.light, lightColorScheme),
        darkTheme: _buildTheme(Brightness.dark, darkColorScheme),
        routerConfig: GoRouter(
          navigatorKey: navigatorKey,
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
              builder: (context, state) {
                final params = state.uri.queryParameters;
                return MapView(stationId: params['station-id'], lineId: params['line-id']);
              },
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
    fontFamily: GoogleFonts.notoSansJp().fontFamily,
  );
}

