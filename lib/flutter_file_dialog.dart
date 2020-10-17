// Copyright (c) 2020 KineApps. All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// File picking/saving dialogs for Android and iOS.
class FlutterFileDialog {
  static const MethodChannel _channel = MethodChannel('flutter_file_dialog');

  /// Displays a dialog for picking a file.
  ///
  /// Returns the path of the picked file or null if operation was cancelled.
  /// Throws exception on error.
  static Future<String> pickFile({OpenFileDialogParams params}) async {
    return await _channel.invokeMethod('pickFile', params?.toJson());
  }

  /// Displays a dialog for selecting a location where to save the file and
  /// saves the file to the selected location.
  ///
  /// Returns path of the saved file or null if operation was cancelled.
  /// Throws exception on error.
  static Future<String> saveFile({SaveFileDialogParams params}) async {
    return await _channel.invokeMethod('saveFile', params?.toJson());
  }
}

/// Dialog types for [pickFile] (iOS only)
enum OpenFileDialogType { document, image }

String _openFileDialogTypeToString(OpenFileDialogType dialogType) {
  switch (dialogType) {
    case OpenFileDialogType.document:
      return 'document';
    case OpenFileDialogType.image:
      return 'image';
    default:
      return null;
  }
}

/// Source types for [pickFile] (iOS only)
enum SourceType { camera, photoLibrary, savedPhotosAlbum }

String _sourceTypeToString(SourceType sourceType) {
  switch (sourceType) {
    case SourceType.camera:
      return 'camera';
    case SourceType.photoLibrary:
      return 'photoLibrary';
    case SourceType.savedPhotosAlbum:
      return 'savedPhotosAlbum';
    default:
      return null;
  }
}

/// Parameters for the [pickFile] method.
class OpenFileDialogParams {
  /// Dialog type (iOS)
  final OpenFileDialogType dialogType;

  /// Source type (iOS)
  final SourceType sourceType;

  // Allow editing? (iOS)
  final bool allowEditing;

  /// You need to register the document types that your application can open
  /// with iOS. To do this you need to add a document type to your app’s
  /// Info.plist for each document type that your app can open. Additionally
  /// if any of the document types are not known by iOS, you will need
  /// to provide an Uniform Type Identifier (UTI) for that document type.
  ///
  /// More info:
  /// https://developer.apple.com/library/archive/qa/qa1587/_index.html
  final List<String> allowedUtiTypes;

  /// Filter for file extensions (null to allow any extension)
  final List<String> fileExtensionsFilter;

  /// MIME types filter (Android only)
  /// Only files with the provided MIME types will be shown in the file picker.
  final List<String> mimeTypesFilter;

  /// Access files in local device only (Android)?
  final bool localOnly;

  /// Create parameters for the [pickFile] method.
  const OpenFileDialogParams({
    this.dialogType = OpenFileDialogType.document,
    this.sourceType = SourceType.photoLibrary,
    this.allowEditing = false,
    this.allowedUtiTypes,
    this.fileExtensionsFilter,
    this.mimeTypesFilter,
    this.localOnly,
  });

  Map<String, dynamic> toJson() {
    return {
      'dialogType': _openFileDialogTypeToString(dialogType),
      'sourceType': _sourceTypeToString(sourceType),
      'allowEditing': allowEditing,
      'allowedUtiTypes': allowedUtiTypes,
      'fileExtensionsFilter': fileExtensionsFilter,
      'mimeTypesFilter': mimeTypesFilter,
      'localOnly': localOnly,
    };
  }
}

/// Parameters for the [saveFile] method.
class SaveFileDialogParams {
  /// Path of the file to save.
  String sourceFilePath;

  /// MIME types filter (Android only)
  /// Only files with the provided MIME types will be shown in the file picker.
  final List<String> mimeTypesFilter;

  /// Access files in local device only (Android)?
  bool localOnly;

  /// Create parameters for the [saveFile] method.
  SaveFileDialogParams({
    @required this.sourceFilePath,
    this.mimeTypesFilter,
    this.localOnly = false,
  });

  Map<String, dynamic> toJson() {
    return {
      'sourceFilePath': sourceFilePath,
      'mimeTypesFilter': mimeTypesFilter,
      'localOnly': localOnly,
    };
  }
}
