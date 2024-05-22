import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:ekimemo_map/models/station.dart';

class StationSimple extends StatelessWidget {
  Station station;
  bool isAccessed = false;
  StationSimple({required this.station, this.isAccessed = false, super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        customBorder: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        onTap: () {
          context.push(Uri(path: '/station', queryParameters: {'id': station.id}).toString());
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