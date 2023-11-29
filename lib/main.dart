import 'package:flutter/material.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import 'services/config.dart';
import 'services/state.dart';
import 'services/gps.dart';
import 'services/station.dart';
import 'services/notification.dart';
import 'services/updater.dart';

import 'ui/pages/home.dart';
import 'ui/pages/settings.dart';
import 'ui/pages/map.dart';
import 'ui/pages/station_detail.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final configProvider = ConfigProvider();
  await configProvider.init();

  final stationManager = StationManager();
  await stationManager.initialize();

  Config.init(configProvider);
  NotificationManager().initialize();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => configProvider),
        ChangeNotifierProvider(create: (_) => stationManager),
        ChangeNotifierProvider(create: (_) => StateManager()),
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
    AssetUpdater.check(context, silent: true);

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
    fontFamily: GoogleFonts.notoSansJp().fontFamily,
  );
}

