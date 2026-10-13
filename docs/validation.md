# Validation and reproduction

Supported demonstration: the included offline browser dashboard and Android
homework interface. All demo data is invented; the browser date is fixed to
2026-10-13. The Python helper suite reads/writes temporary synthetic data.

Tooling: Python 3.14.4, Node 22.22.1, Playwright 1.60.0/Chromium, Java 17,
Gradle 8.11.1, Android Gradle Plugin 8.10.1, Kotlin 1.9.24, SDK/platform 36.
Java/Android tooling is needed only for the Android build. Internet downloads
may be needed to install tools/dependencies, but not to use the offline app.

Run the commands in README.md from a fresh checkout. Browser tests open the real
HTML file in an offline Chromium context; no HTTP server is involved. Browsers
can vary in file-URL storage support; Chromium was the tested path.

- 18 Python tests: retained normalization/storage edges plus synthetic completion,
  reopen, notes and shared-fixture consistency. Network sockets are denied during
  the representative offline flow.
- 13 Android JVM tests: codec/status persistence, swipe intent thresholds,
  appearance and subject-colour behaviour. A debug APK builds for local testing.
- Android API 35 emulator: complete a task, restart the app, verify saved status,
  switch children and find the completed task in the archive.
- Two browser workflows: complete/reload, child isolation, notes/reload, four real
  screenshots, narrow layout checks and no fetch/XHR/socket/beacon attempts.

The original network/authentication, credential-store and external gateway tests
are excluded together with those features; no inherited 33-test claim is made.
No live school account, physical device, iOS build, release signing, app-store
qualification, general security certification or complete test coverage is claimed.
The Swift folder is model/codec reference source only.

Screenshots were captured with an isolated browser profile at 1100×900 CSS pixels
(full-page capture), light appearance, en-GB locale and UTC timezone. Source:
fixtures/homework.json, copied identically into the browser and Android fixtures.
No image-generation tool was used. Screenshot hashes and source-input provenance
are recorded by the private publication process.
