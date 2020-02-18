# flutter_file_dialog

Dialogs for picking and saving files in Android and in iOS.

## Features

- Supports Android and iOS.
- Modern plugin implementation based on Kotlin (Android) and Swift (iOS).
- Pick image files and other documents.
- Save a file to a selected location.
- iOS dialog types: document and image.
- iOS source types: camera, photo library, saved photos album.
- Allow user to edit picked image in iOS.
- Set file extension filter and mime types filter when picking a document.
- Possibility to limit picking a file from the local device only (Android).

## Examples

### Pick an image file

```dart
  final params = OpenFileDialogParams(
    dialogType: OpenFileDialogType.image,
    sourceType: SourceType.photoLibrary,
  );
  final filePath = await FlutterFileDialog.pickFile(params: params);
  print(filePath);
```

### Pick a document

```dart
  final params = OpenFileDialogParams(
    dialogType: OpenFileDialogType,
    sourceType: SourceType.photoLibrary,
  );
  final filePath = await FlutterFileDialog.pickFile(params: params);
  print(filePath);
```

### Save a file

```dart
final params = SaveFileDialogParams(sourceFilePath: "path_of_file_to_save");
final filePath = await FlutterFileDialog.saveFile(params: params);
print(filePath);
```
