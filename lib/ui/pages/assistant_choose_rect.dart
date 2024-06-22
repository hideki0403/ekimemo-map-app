import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:crop_your_image/crop_your_image.dart';
import 'package:image_picker/image_picker.dart';

import 'package:ekimemo_map/services/log.dart';

class AssistantChooseRectView extends StatefulWidget {
  const AssistantChooseRectView({super.key});

  @override
  State<StatefulWidget> createState() => _AssistantChooseRectViewState();
}

class _AssistantChooseRectViewState extends State<AssistantChooseRectView> {
  final CropController _controller = CropController();
  final logger = Logger('AssistantChooseRectView');

  Uint8List? _image;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Choose Rect'),
        actions: [
          IconButton(
            icon: const Icon(Icons.check),
            onPressed: _image != null ? () {
              _controller.crop();
            } : null,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        child: const Icon(Icons.image),
        onPressed: () async {
          final image = await ImagePicker().pickImage(source: ImageSource.gallery);
          if (image == null) return;
          final bytes = await image.readAsBytes();

          if (!context.mounted) return;
          setState(() {
            _image = bytes;
          });
        },
      ),
      body: SizedBox(
        width: double.infinity,
        height: double.infinity,
        child: Center(
          child: _image != null ? Crop(
            image: _image!,
            controller: _controller,
            onCropped: (result) {
              List<double> rawRect = utf8.decode(result).split(',').map((e) => double.parse(e)).toList();
              final rect = Rect.fromLTRB(rawRect[0], rawRect[1], rawRect[2], rawRect[3]);
              logger.debug('Cropped Rect: $rect');
              Navigator.of(context).pop(rect);
            },
            imageCropper: const ImageCropperHandler(),
          ) : const Text('Select Image'),
        )
      )
    );
  }
}

class ImageCropperHandler extends ImageCropper<Image> {
  const ImageCropperHandler();

  @override
  FutureOr<Uint8List> call({
    required dynamic original,
    required Offset topLeft,
    required Offset bottomRight,
    dynamic outputFormat,
    dynamic shape,
  }) {
    // 画像はクロップせず、topLeft, bottomRightを元にRectを返す
    final rect = Rect.fromPoints(topLeft, bottomRight);
    return Uint8List.fromList(utf8.encode('${rect.left},${rect.top},${rect.right},${rect.bottom}'));
  }
}