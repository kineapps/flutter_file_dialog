// Copyright (c) 2020 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_file_dialog/flutter_file_dialog.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _isBusy = false;
  OpenFileDialogType _dialogType = OpenFileDialogType.image;
  SourceType _sourceType = SourceType.photoLibrary;
  bool _allowEditing = false;
  File _currentFile;
  String _savedFilePath;
  bool _localOnly = false;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('FlutterFileDialog test app'),
        ),
        body: Center(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const SizedBox(
                height: 24,
              ),
              CupertinoSegmentedControl<OpenFileDialogType>(
                children: {
                  OpenFileDialogType.document: Text("document"),
                  OpenFileDialogType.image: Text("image"),
                },
                groupValue: _dialogType,
                onValueChanged: (value) => setState(() => _dialogType = value),
              ),
              const SizedBox(
                height: 24,
              ),
              CupertinoSegmentedControl<SourceType>(
                children: {
                  SourceType.photoLibrary: Text("photoLibrary"),
                  SourceType.savedPhotosAlbum: Text("savedPhotosAlbum"),
                  SourceType.camera: Text("camera"),
                },
                groupValue: _sourceType,
                onValueChanged: (value) => setState(() => _sourceType = value),
              ),
              const SizedBox(
                height: 24,
              ),
              if (_dialogType == OpenFileDialogType.image)
                CheckboxListTile(
                  title: Text("Allow editing"),
                  value: _allowEditing,
                  onChanged: (v) => setState(() => _allowEditing = v),
                ),
              const SizedBox(
                height: 24,
              ),
              RaisedButton(
                child: Text('Pick file'),
                onPressed: () => _pickFile(),
              ),
              const SizedBox(
                height: 24,
              ),
              if (_currentFile?.existsSync() == true) ...[
                Text(_currentFile.path),
                Text(
                    "${(_currentFile.lengthSync() / 1024.0).toStringAsFixed(1)} KB"),
                SizedBox(
                  width: 200,
                  height: 200,
                  child: Image.file(
                    _currentFile,
                  ),
                ),
              ],
              RaisedButton(
                child: Text('Save file'),
                onPressed: _currentFile == null ? null : () => _saveFile(),
              ),
              Text(_savedFilePath ?? "-"),
              if (_isBusy) CircularProgressIndicator(),
              CheckboxListTile(
                title: Text("Local only"),
                value: _localOnly,
                onChanged: (v) => setState(() {
                  _localOnly = v;
                }),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _pickFile() async {
    String result;
    try {
      setState(() {
        _isBusy = true;
        _currentFile = null;
      });
      final params = OpenFileDialogParams(
        dialogType: _dialogType,
        sourceType: _sourceType,
        allowEditing: _allowEditing,
        localOnly: _localOnly,
      );
      result = await FlutterFileDialog.pickFile(params: params);
      print(result);
    } on PlatformException catch (e) {
      print(e);
    } finally {
      setState(() {
        if (result != null) {
          _currentFile = File(result);
        } else {
          _currentFile = null;
        }
        _isBusy = false;
      });
    }
  }

  Future<void> _saveFile() async {
    String result;
    try {
      setState(() {
        _isBusy = true;
      });
      final params = SaveFileDialogParams(
          sourceFilePath: _currentFile.path, localOnly: _localOnly);
      result = await FlutterFileDialog.saveFile(params: params);
      print(result);
    } on PlatformException catch (e) {
      print(e);
    } finally {
      setState(() {
        _savedFilePath = result ?? _savedFilePath;
        _isBusy = false;
      });
    }
  }
}
