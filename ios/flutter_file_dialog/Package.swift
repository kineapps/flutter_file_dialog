// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "flutter_file_dialog",
    platforms: [
        .iOS(.v12)
    ],
    products: [
        .library(name: "flutter-file-dialog", targets: ["flutter_file_dialog"])
    ],
    targets: [
        .target(
            name: "flutter_file_dialog"
        )
    ]
)
