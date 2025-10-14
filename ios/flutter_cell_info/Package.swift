// swift-tools-version:5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "flutter_cell_info",
    platforms: [
        .iOS("11.0")
    ],
    products: [
        .library(
            name: "flutter_cell_info",
            targets: ["flutter_cell_info"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "flutter_cell_info",
            dependencies: [],
            path: ".",
            exclude: ["flutter_cell_info.podspec"],
            sources: ["Classes"],
            publicHeadersPath: "Classes",
            cSettings: [
                .headerSearchPath("Classes")
            ]
        )
    ]
)

