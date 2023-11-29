import 'dart:async';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:url_launcher/url_launcher_string.dart';
// import 'package:latlong2/latlong.dart';

class MapView extends StatefulWidget {
  const MapView({Key? key}) : super(key: key);

  @override
  State<StatefulWidget> createState() => _MapViewState();
}

class _MapViewState extends State<MapView> {
  final _mapReadyCompleter = Completer<MaplibreMapController>();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('マップ'),
      ),
      body: SafeArea(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(child: MaplibreMap(
              initialCameraPosition: const CameraPosition(target: LatLng(35.681236, 139.767125), zoom: 10.0),
              onMapCreated: (controller) {
                _mapReadyCompleter.complete(controller);
              },
              onStyleLoadedCallback: () async {
                final controller = await _mapReadyCompleter.future;
                // controller.matchMapLanguageWithDeviceDefault();
              },
              styleString: 'https://assets.yukineko.dev/map/style/google_maps_style.json',
            )),
            Padding(
              padding: const EdgeInsets.only(top: 8, bottom: 8),
              child: RichText(
                text: TextSpan(
                  style: const TextStyle(color: Colors.black),
                  children: [
                    const TextSpan(text: '©'),
                    TextSpan(text: 'OpenStreetMap', style: const TextStyle(color: Colors.blue), recognizer: TapGestureRecognizer()..onTap = () => launchUrlString('https://www.openstreetmap.org/copyright')),
                    const TextSpan(text: ' contributors'),
                  ],
                ),
              ),
            ),
          ],
        )
      )
    );
  }
}