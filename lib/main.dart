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
import 'ui/pages/assistant_flow.dart';
import 'ui/pages/assistant_choose_rect.dart';

final navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final configProvider = ConfigProvider();
  await configProvider.init();

  final systemStateProvider = SystemStateProvider();
  await systemStateProvider.init();

  Config.init(configProvider);
  SystemState.init(systemStateProvider);
  NotificationManager.initialize();

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
      builder: (lightColorScheme, darkColorScheme) {
        ColorScheme? lightScheme, darkScheme;

        if (lightColorScheme != null && darkColorScheme != null) {
          final (light, dark) = _generateDynamicColourSchemes(lightColorScheme, darkColorScheme);
          lightScheme = light;
          darkScheme = dark;
        } else {
          lightScheme = lightColorScheme;
          darkScheme = darkColorScheme;
        }

        return MaterialApp.router(
          theme: _buildTheme(Brightness.light, lightScheme),
          darkTheme: _buildTheme(Brightness.dark, darkScheme),
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
                path: '/assistant-flow',
                builder: (context, state) => const AssistantFlowView(),
              ),
              GoRoute(
                path: '/assistant-choose-rect',
                builder: (context, state) => const AssistantChooseRectView(),
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
        );
      }
    );
  }
}

ThemeData _buildTheme(Brightness brightness, ColorScheme? colorScheme) {
  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: colorScheme,
    fontFamily: GoogleFonts.notoSansJp().fontFamily,
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: <TargetPlatform, PageTransitionsBuilder>{
        TargetPlatform.android: ZoomPageTransitionsBuilder(
          allowEnterRouteSnapshotting: false,
        ),
      },
    ),
  );
}

// https://github.com/material-foundation/flutter-packages/issues/582
(ColorScheme light, ColorScheme dark) _generateDynamicColourSchemes(ColorScheme lightDynamic, ColorScheme darkDynamic) {
  var lightBase = ColorScheme.fromSeed(seedColor: lightDynamic.primary);
  var darkBase = ColorScheme.fromSeed(seedColor: darkDynamic.primary, brightness: Brightness.dark);

  var lightAdditionalColours = _extractAdditionalColours(lightBase);
  var darkAdditionalColours = _extractAdditionalColours(darkBase);

  var lightScheme = _insertAdditionalColours(lightBase, lightAdditionalColours);
  var darkScheme = _insertAdditionalColours(darkBase, darkAdditionalColours);

  return (lightScheme.harmonized(), darkScheme.harmonized());
}

List<Color> _extractAdditionalColours(ColorScheme scheme) => [
  scheme.surface,
  scheme.surfaceDim,
  scheme.surfaceBright,
  scheme.surfaceContainerLowest,
  scheme.surfaceContainerLow,
  scheme.surfaceContainer,
  scheme.surfaceContainerHigh,
  scheme.surfaceContainerHighest,
];

ColorScheme _insertAdditionalColours(ColorScheme scheme, List<Color> additionalColours) => scheme.copyWith(
  surface: additionalColours[0],
  surfaceDim: additionalColours[1],
  surfaceBright: additionalColours[2],
  surfaceContainerLowest: additionalColours[3],
  surfaceContainerLow: additionalColours[4],
  surfaceContainer: additionalColours[5],
  surfaceContainerHigh: additionalColours[6],
  surfaceContainerHighest: additionalColours[7],
);
