# SumCtrl

Offline calculator, unit converter and work tools in the VKNXWorks Ctrl series. Kotlin, Jetpack Compose, Material 3, manual DI (`AppContainer`), a JSON file in private storage for history (encrypted at rest) plus DataStore for settings — the same structure as RideCtrl and BeamCtrl.

## Layout

- `data/calc/Engine.kt`: the expression engine. An expression is a list of keypad tokens; arithmetic is `BigDecimal` (DECIMAL128), functions go through `Double`. Pure Kotlin, no Android imports, so it can be tested with plain `kotlinc`.
- `data/calc/Editor.kt`: `CalcState`, the immutable keypad state machine (what each key does to the line).
- `data/convert/Units.kt`: converter categories and units. `data/tools/WorkMath.kt`: VAT, discount, margin, instalments, percentages.
- `data/NumFmt.kt`: all number formatting and parsing (decimal separator, grouping, currency).

## Hard rules

- **No `INTERNET` permission, ever** (the manifest removes it; CI fails the build if the APK has any network permission).
- **Google-free:** no Google libraries, services or SDKs, including transitive ones (CI checks `releaseRuntimeClasspath`). No WebView. Fonts are bundled.
- **Telemetry-free:** no analytics or crash reporting.
- arm64-v8a only. Every user-facing string goes in `strings.xml`.
- Settings open only from the profile picture. No settings tab, menu or gear icon, no You tab.
- Money and arithmetic stay exact (`BigDecimal`); round only for display. Number inputs accept both `,` and `.` as the decimal separator.

## Encryption at rest

- `history.json` is sealed with AES-256-GCM (`data/crypto/SealedFile`, marker `SUMCTRL-REST1`) under the data key (`DataKeyStore`: 32 random bytes in `filesDir/data.key`, wrapped by the Keystore key `sumctrl_data_at_rest` through `KeystoreWrapper`/`KeystoreAes`; no user authentication, independent of the app lock). Settings (DataStore) and the app lock vault are not touched.
- `HistoryRepository` accepts both sealed and ordinary files (`SealedFile.isSealed`). Without a data key it reads and writes the ordinary file as before. All of it runs on `Dispatchers.IO`; nothing asks the Keystore on the main thread.
- An ordinary file only becomes a sealed one through `SealedFile.sealInPlace` (temp file, sync, read back, compare, atomic rename). Never replace an existing `data.key`, and never create a Keystore key on the decrypt path (`KeystoreAes.existing`).
- A sealed history that can't be opened is never overwritten: it is renamed to `history.json.unreadable`, a new history starts (`HistoryRepository.setAside` drives the note in the History sheet), and it is merged back when it opens at a later start. Only `clear()` deletes it.
- There are no unit tests in this app (no test source set), so none were added for this.

## Dependency verification

Gradle checks every library and plugin download against the sha256 checksums in `gradle/verification-metadata.xml` (checksums only, no PGP). When a dependency, plugin or the Gradle version changes, start the `verification-metadata.yml` workflow (`gh api -X POST repos/VoidedKN0X/SumCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), take the file from its log (gzipped and base64-encoded between the BEGIN/END markers), check the diff (only Maven Central, Google's Maven and the Gradle plugin portal) and commit it in the same push. Never turn verification off or make it lenient.

## Releasing

- A release is published only when `versionName` in `app/build.gradle.kts` is bumped. Bump `versionCode` by 1 at the same time.
- Tags are `vX.Y.Z` and releases are named "SumCtrl X.Y.Z". Never add a build number (`-build.N`, "(build N)").
- Use the same version for every change that ships together; README, licence and CI changes don't need a bump.
- Push straight to `main`; no branches or pull requests.

## Changelog

Every new version needs a detailed entry at the top of `CHANGELOG.md`, under a heading for that version (match the existing headings). CI refuses to release without one, and the entry becomes the release notes on GitHub, in Obtainium and in the CtrlF-Droid repo.

- Write for people using the app, not for developers: what's new, what changed, what was fixed.
- Cover every user-visible change in the version, one bullet each, with enough detail to understand it without opening the app.
- Group with `### New`, `### Changed`, `### Fixed` and `### Security` when there's more than a couple of items.
