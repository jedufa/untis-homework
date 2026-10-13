# Third-party notices

The root MIT license covers the authorized first-party material. It does not
replace upstream terms, grant rights to external trademarks or license private
data. The project is independent of Untis/WebUntis.

## Redistributed tooling and assets

The Gradle 8.11.1 wrapper JAR was obtained from the official versioned Gradle
repository and verified against its published SHA-256. Wrapper scripts retain
their Apache notices. The official distribution license and notice are preserved
in [third_party/gradle/LICENSE](third_party/gradle/LICENSE) and
[third_party/gradle/NOTICE](third_party/gradle/NOTICE).

Wrapper SHA-256: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.
The distribution SHA-256 is pinned in gradle-wrapper.properties.
The only app icon is a new simple vector document shape; no original raster
artwork is included. Screenshots show the included app and fictional fixtures.

## Build-time dependencies (not vendored)

Android uses Kotlin, Compose/AndroidX and coroutines under Apache-2.0; tests add
JUnit 4.13.2 (EPL-1.0), Hamcrest 1.3 (New BSD) and org.json 20240303 (Public Domain
as declared by its exact POM). See the [97-coordinate resolved graph](docs/dependencies.md)
for precise versions and inherited license declarations. Android SDK/JDK tools
are acquired separately under their own terms and are not redistributed here.

The browser tests use @playwright/test, playwright and playwright-core 1.60.0
(Apache-2.0), resolved by package-lock.json. These packages and downloaded browsers
are not included in this source release. No third-party Python runtime dependency
is required. The Swift references import Apple platform frameworks; Apple tools
and frameworks are not redistributed.

This review is bounded to the actual source payload and recorded build graph.
A fork that vendors new code, images, libraries or binaries must review those
materials separately.
