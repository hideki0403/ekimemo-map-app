import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:ekimemo_map/models/station.dart';

class StationSimple extends StatelessWidget {
  final Station station;
  final bool isAccessed;
  const StationSimple({required this.station, required this.isAccessed, super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/station', queryParameters: {'id': station.code.toString()}).toString());
        },
        child: Padding(
          padding: const EdgeInsets.only(left: 20, top: 12, bottom: 12, right: 20),
          child: Row(
            children: [
              Expanded(
                child: Text(station.name, textScaler: const TextScaler.linear(1.2)),
              ),
              if (isAccessed) const Icon(Icons.check, color: Colors.green),
            ],
          ),
        ),
      ),
    );
  }
}