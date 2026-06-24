## 3.3.1

- [Android] Fix "Could not find method kotlin()" build failure on AGP 9 when built-in Kotlin is disabled (the Flutter template default) — only configure the kotlin {} DSL when the Kotlin Gradle Plugin is applied, and apply KGP when built-in Kotlin is off

## 3.3.0

- [iOS] Raised declared deployment target to 13.0 to match the minimum already required by Flutter 3.44+
- [Android] Bumped Kotlin to 2.3.20, raised minSdk to 24, and modernized the `lint`/test configuration
- [Example] Migrated the iOS example to Swift Package Manager and the UIScene lifecycle; migrated the Android example to Kotlin DSL + AGP 9 / built-in Kotlin

## 3.2.0

- [iOS] Added Swift Package Manager (SPM) support alongside existing CocoaPods support. Thanks to [@cjouannet](https://github.com/cjouannet)!
- [iOS] Raised minimum iOS deployment target to 12.0

## 3.1.0

- Updates minimum supported SDK version to Flutter 3.44 / Dart 3.12
- [Android] Migrates to built-in Kotlin (applies the Kotlin Gradle Plugin only on AGP < 9)

## 3.0.3

- [Android] Updated compileSdkVersion to 36, Gradle to 8.14 and Java/Kotlin compatibility to version 17
- [Android] Additional fix for #31: java.lang.IllegalStateException Reply already submitted

## 3.0.2

- Fix AGP 8.0 compile error. Thanks to @FauconSpartiate(https://github.com/FauconSpartiate)!

## 3.0.1

- Adds a namespace for compatibility with AGP 8.0. Thanks to @[asaarnak](https://github.com/asaarnak)!

## 3.0.0

- Allow picking a directory and saving files to the picked directory without further dialogs. Thanks to @[Doflatango](https://github.com/Doflatango)!
- minimum iOS version is now 11

## 2.3.2

- [Android] Fixed #28: Extra content being saved to end of file sometimes. Thanks to @[hygehyge](https://github.com/hygehyge)!

## 2.3.1

- [Android] Fixed #31: java.lang.IllegalStateException Reply already submitted

## 2.3.0

- [iOS] Added support for iOS 10

## 2.2.0

- [Android] upgraded gradle
- [Android] jcenter => mavenCentral
- [Android] removed obsolete android.enableR8
- [Android] updated to Kotlin 1.5.30
- [Android] set compileSdkVersion to 31
- [Android] Removed V1 embedding

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
