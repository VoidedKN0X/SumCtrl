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

## Build toolchain

- AGP 9.5.1, Gradle 9.9.2, Kotlin 2.4.21, Compose BOM 2026.09.00, `compileSdk` 37 (Compose 1.12 needs it), `minSdk` 35, `targetSdk` 36. All versions are in `gradle/libs.versions.toml`.
- AGP 9 has built-in Kotlin: there is no `org.jetbrains.kotlin.android` plugin any more. The Kotlin version is pinned by the `kotlin-gradle-plugin` classpath entry in the root `build.gradle.kts`; the Compose and serialization compiler plugins use the same `kotlin` version. Don't re-add the Kotlin Android plugin.
- AGP 9's R8 is stricter (`-keep class X` without a member block no longer keeps the constructor; resources are shrunk together with code). Keep rules in `proguard-rules.pro` must name the members they need, and resources must be referenced through `R`, never by name.
- Any change here needs the dependency verification procedure below.

## Dependency verification

Gradle checks every library and plugin download against the sha256 checksums in `gradle/verification-metadata.xml` (checksums only, no PGP). When a dependency, plugin or the Gradle version changes, start the `verification-metadata.yml` workflow (`gh api -X POST repos/VoidedKN0X/SumCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), take the file from its log (gzipped and base64-encoded between the BEGIN/END markers), check the diff (only Maven Central and Google's Maven) and commit it in the same push. Never turn verification off or make it lenient.

`settings.gradle.kts` lists only `google()` and `mavenCentral()` for plugins, not `gradlePluginPortal()`: the portal serves different files for the Kotlin plugin markers, which made verification fail whenever Maven Central had a hiccup. Don't add it back unfiltered (see the comment in that file).

## Build and signing (CI)

`.github/workflows/build.yml` has four jobs: `version` (is this a release?), `build` (Gradle, **no secrets**, `UNSIGNED_RELEASE=true` gives `app-release-unsigned.apk`; runs every check), `sign` (no checkout, no Gradle: `zipalign` + `apksigner` with the release key, then checks the signer against `EXPECTED_CERT_SHA256` and that the APK's contents didn't change) and `release` (attestation, release notes, publish).

- Never give the `build` job a secret, and never run Gradle or check out the repo in `sign`. New build checks (tests, lint, APK checks) go in `build`.
- `EXPECTED_CERT_SHA256` in `build.yml` must match the fingerprint in `SECURITY.md` and `README.md`; the build fails if it doesn't.
- No Gradle cache in any workflow (`cache-disabled: true`): a cache saved by a failed run used to break every later build.

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
