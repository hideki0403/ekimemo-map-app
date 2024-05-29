import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'services/database.dart';
import 'services/config.dart';
import 'services/gps.dart';
import 'services/station.dart';
import 'services/notification.dart';
import 'services/log.dart';
import 'services/cache.dart';

import 'ui/pages/home.dart';
import 'ui/pages/settings.dart';
import 'ui/pages/map.dart';
import 'ui/pages/station_detail.dart';
import 'ui/pages/line_detail.dart';
import 'ui/pages/assistant_flow.dart';
import 'ui/pages/assistant_choose_rect.dart';
import 'ui/pages/log.dart';

final navigatorKey = GlobalKey<NavigatorState>();
final logger = Logger('App');

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  FlutterError.onError = (details) {
    logger.error('FlutterError: ${details.exceptionAsString()}, stack: ${details.stack}');

  };

  PlatformDispatcher.instance.onError = (error, stack) {
    logger.error('PlatformError: $error, $stack');
    return false;
  };

  await DatabaseHandler.init();

  final configProvider = ConfigProvider();
  await configProvider.init();

  final systemStateProvider = SystemStateProvider();
  await systemStateProvider.init();

  Config.init(configProvider);
  await CacheManager.initialize();

  SystemState.init(systemStateProvider);
  NotificationManager.initialize();
  await StationManager.initialize();

  logger.info('App initialized');

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => configProvider),
        ChangeNotifierProvider(create: (_) => systemStateProvider),
        ChangeNotifierProvider(create: (_) => StationStateNotifier()),
        ChangeNotifierProvider(create: (_) => GpsStateNotifier()),
        ChangeNotifierProvider(create: (_) => LogStateNotifier()),
      ],
      child: Root(),
    ),
  );
}

class Root extends StatelessWidget {
  Root({super.key});

  final router = GoRouter(
    navigatorKey: navigatorKey,
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
        path: '/assistant-flow',
        builder: (context, state) => const AssistantFlowView(),
      ),
      GoRoute(
        path: '/assistant-choose-rect',
        builder: (context, state) => const AssistantChooseRectView(),
      ),
      GoRoute(
        path: '/log',
        builder: (context, state) => const LogView(),
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
      GoRoute(
        path: '/line',
        builder: (context, state) {
          return LineDetailView(lineId: state.uri.queryParameters['id']);
        },
      ),
    ],
  );

  @override
  Widget build(BuildContext context) {
    final config = Provider.of<ConfigProvider>(context);
    return DynamicColorBuilder(
      builder: (lightColorScheme, darkColorScheme) => MaterialApp.router(
        theme: _buildTheme(Brightness.light, lightColorScheme, config.fontFamily),
        darkTheme: _buildTheme(Brightness.dark, darkColorScheme, config.fontFamily),
        themeMode: config.themeMode,
        routerConfig: router,
      ),
    );
  }
}

ThemeData _buildTheme(Brightness brightness, ColorScheme? colorScheme, String? fontFamily) {
  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: colorScheme,
    fontFamily: fontFamily,
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: <TargetPlatform, PageTransitionsBuilder>{
        TargetPlatform.android: ZoomPageTransitionsBuilder(
          allowEnterRouteSnapshotting: false,
        ),
      },
    ),
  );
}
