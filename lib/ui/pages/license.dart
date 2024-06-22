import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';

class LicenseView extends StatefulWidget {
  const LicenseView({super.key});

  @override
  State<StatefulWidget> createState() => _LicenseViewState();
}

class _LicenseViewState extends State<LicenseView> {
  String _version = '?';

  @override
  void initState() {
    super.initState();

    LicenseRegistry.addLicense(() async* {
      yield const LicenseEntryWithLineBreaks(<String>['station_database'], '''
        "station_database" by Seo-4d696b75, Licensed under CC BY 4.0  
        
          https://github.com/Seo-4d696b75/station_database  
          
          https://creativecommons.org/licenses/by/4.0/  
        ''');

      yield const LicenseEntryWithLineBreaks(<String>['Sound Effects'], '''
        - Sound Effect by UNIVERSFIELD, LIECIO from Pixabay  
        
          https://pixabay.com/sound-effects/  
          
          https://pixabay.com/users/universfield-28281460  
          
          https://pixabay.com/users/liecio-3298866  
        
        - 効果音ラボ
        
          https://soundeffect-lab.info/
        
        - ポケットサウンド
        
          https://pocket-se.info/
        ''');

      yield LicenseEntryWithLineBreaks(<String>['ekisagasu'], await rootBundle.loadString('assets/license/ekisagasu.txt'));
    });

    PackageInfo.fromPlatform().then((packageInfo) {
      if (!context.mounted) return;
      setState(() {
        _version = packageInfo.version;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return LicensePage(
      applicationName: '駅メモマップ',
      applicationVersion: _version,
      applicationIcon: Image.asset('assets/icon/app.png', width: 128, height: 128),
    );
  }
}