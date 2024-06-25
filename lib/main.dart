import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import 'services/database.dart';
import 'services/config.dart';
import 'services/gps.dart';
import 'services/station.dart';
import 'services/notification.dart';
import 'services/log.dart';

import 'repository/station.dart';
import 'repository/line.dart';
import 'repository/tree_node.dart';

import 'ui/pages/home.dart';
import 'ui/pages/settings.dart';
import 'ui/pages/map.dart';
import 'ui/pages/station_detail.dart';
import 'ui/pages/line_detail.dart';
import 'ui/pages/assistant_flow.dart';
import 'ui/pages/assistant_choose_rect.dart';
import 'ui/pages/log.dart';
import 'ui/pages/tools.dart';
import 'ui/pages/search.dart';
import 'ui/pages/history.dart';
import 'ui/pages/route_search.dart';
import 'ui/pages/license.dart';

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

  // Initialize database cache
  await StationRepository().buildCache();
  await LineRepository().buildCache();
  await TreeNodeRepository().buildCache();

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
        path: '/tools',
        builder: (context, state) => const ToolsView(),
      ),
      GoRoute(
        path: '/search',
        builder: (context, state) => const SearchView(),
      ),
      GoRoute(
        path: '/history',
        builder: (context, state) => const HistoryView(),
      ),
      GoRoute(
        path: '/route-search',
        builder: (context, state) => const RouteSearchView(),
      ),
      GoRoute(
        path: '/license',
        builder: (context, state) => const LicenseView(),
      ),
      GoRoute(
        path: '/map',
        builder: (context, state) {
          final params = state.uri.queryParameters;
          return MapView(stationId: params['station-id'], lineId: params['line-id'], radarId: params['radar-id']);
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
    final font = GoogleFonts.asMap().keys.contains(config.fontFamily) ? config.fontFamily : null;

    ThemeData buildTheme(Brightness brightness, ColorScheme? colorScheme) {
      final baseTheme = ThemeData(
        useMaterial3: true,
        brightness: brightness,
        colorScheme: config.useMaterialYou ? colorScheme : ColorScheme.fromSeed(seedColor: config.themeSeedColor, brightness: brightness),
        pageTransitionsTheme: const PageTransitionsTheme(
          builders: <TargetPlatform, PageTransitionsBuilder>{
            TargetPlatform.android: ZoomPageTransitionsBuilder(
              allowEnterRouteSnapshotting: false,
            ),
          },
        ),
      );

      return baseTheme.copyWith(
        textTheme: font != null ? GoogleFonts.getTextTheme(font, baseTheme.textTheme) : GoogleFonts.notoSansJpTextTheme(baseTheme.textTheme),
      );
    }

    return DynamicColorBuilder(
      builder: (lightColorScheme, darkColorScheme) => MaterialApp.router(
        theme: buildTheme(Brightness.light, lightColorScheme),
        darkTheme: buildTheme(Brightness.dark, darkColorScheme),
        themeMode: config.themeMode,
        routerConfig: router,
      ),
    );
  }
}
