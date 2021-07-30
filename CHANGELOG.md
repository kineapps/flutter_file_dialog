## 2.1.1

[iOS] Fixed #14 Document picker dialog could not be closed by dragging it down

## 2.1.0

[iOS] Fixed #14 FlutterFileDialog.pickFile doesn't return value if picker is slid down
[iOS] Fixed issue with relative paths in saveFile
[Android] #12 Added a new parameter OpenFileDialogParams.copyFileToCacheDir (Android only)
[Android] Updated compileSdkVersion and targetSdkVersion to 30

## 2.0.0

- null safety

## 1.0.0

- added SaveFileDialogParams.fileName for specifying a suggested file name
- added SaveFileDialogParams.data for prodiving file data instead of source file path

## 0.0.5

[Android] Fixed: any thrown exception caused crash. Now exception is delivered correctly to caller.

## 0.0.4

- [Android] Improved error handling.

## 0.0.3

- [iOS] Use background threads where needed to keep UI more responsive.

## 0.0.2

- Fixed a possible crash in apps using mixed Android V1/V2 embedding (issue #1)

## 0.0.1+1

- Minor cleanup.

## 0.0.1

- Initial release.
