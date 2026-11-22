# Changelog

What changed in each SumCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/SumCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.4.1 (2026-11-22)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Fixed

- **Odd numbers in the VAT and Instalments tools no longer close the app.** "Remove VAT" with a rate of −100 %, or Instalments with a negative yearly rate (−1200 %, −2400 %) or a huge number of months, used to make the app close. A negative VAT rate now just shows no result, and Instalments shows its "can't work this out" note, which now also says to use 1 to 600 months and a rate that isn't negative (the same limit as the Loan table). A yearly rate so tiny that it rounds away to nothing is now treated as 0 % in Instalments and the Loan table instead of failing. As a safety net, any other calculation error in VAT, Instalments or Loan table shows nothing instead of closing the app.
- **The history CSV export is no longer cut short when you close the History sheet.** If you closed the sheet right after picking where to save the file, the file could end up incomplete with no message. The file is now written in the background until it's done, and the "saved" / "couldn't save" message still shows after the sheet is closed.
- **Settings export and import finish even if you leave Settings straight away.** The same fix for the settings file: saving it, and applying an imported one after you confirmed, now finish in the background.

### Security

- **"Forgot PIN?" now asks for your phone's own screen lock first.** Before, anyone holding your unlocked phone could tap "Forgot PIN?" on SumCtrl's lock screen and reset it. Now, if your phone has a screen lock (PIN, pattern, password, or a strong fingerprint), SumCtrl asks for it before it deletes anything; cancelling leaves everything as it was. The dialog says so. If your phone has no screen lock, it works as before (you only confirm the dialog).
- **History notes that start with a tab or a line break are now made safe in the CSV export too.** Text starting with `=`, `+`, `-` or `@` already got a `'` in front so a spreadsheet doesn't run it as a formula; a note starting with a tab or a carriage return now gets one as well, because some spreadsheets skip those and still run what follows.

## v0.4.0 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### New

- **SumCtrl in French and German, following the phone's language.** Every screen, message and setting now also comes in French (polite "vous" form) and German (informal "du"), next to English and Dutch. Android's per-app language setting (Settings > Apps > SumCtrl > Language) now offers all four. Translated by Claude and checked by script for missing strings and placeholders; not yet read through by a native speaker or run on a phone.

## v0.3.1 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Fixed

- **The app lock can no longer be skipped through the "Calculate" pop-up.** If the pop-up was open when the "Lock again" time ran out and you then switched straight to SumCtrl, without the app going to the background in between, SumCtrl could open without asking for the PIN. Opening SumCtrl's own screen now always checks the time away first. Built and checked by CI only; not run on a phone.

## v0.3.0 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### New

- **"Calculate" in other apps' text-selection menu.** Select a sum anywhere, such as `12,50 + 3,20` in a message or `3 x 4.5` on a web page, and choose **Calculate** (on some phones it's under the ⋮ more menu of the selection). A small SumCtrl pop-up shows the sum and its answer; **Replace** puts the answer in place of the selection (only in apps where the text can be edited), **Copy** copies it. It reads `+ − × ÷` (also `*`, `x`, `/` and `:`), `%`, `^`, brackets, `√` and `π`; a comma or a point works as the decimal separator, a separator used more than once (`1.000.000`) is read as thousands grouping, and spaces and `€ $ £ ¥` are skipped. The answer goes back with the decimal separator the selection used. The pop-up only sees the selected text: it shows nothing of your history, memory or named values and saves nothing, so it doesn't ask for the app lock. Opening it doesn't count as returning to SumCtrl for **Lock again**, so the time away keeps running.
- **Memory keys and named values.** The scientific keys have a fourth row: **MC**, **MR**, **M+**, **M−** and **x=**. M+ and M− add the current result (or what the sum works out to so far) to the memory or take it off; MR puts `M` into the sum; an "M" next to DEG/RAD shows that something is in the memory. **x=** opens **Named values**: save the current result under a name of your own (`rate`, `rent`…: a letter, then letters, digits or `_`) and tap a name to use it in a sum. A named value is used with every digit it was saved with, not the 12 shown. The memory and the named values are kept between starts in a new file, `values.json`, encrypted on the phone with the same key and the same safeguards as the history (a file that can't be opened is moved aside, never deleted, and comes back if it opens later). "Forgot PIN?" deletes them along with the history.
- **Cursor editing.** Tap anywhere in the sum to put a cursor there; digits, operators, functions, backspace and ± then work at the cursor instead of at the end, so you can fix a number in the middle of a long sum. Tap the end of the sum to type at the end again. Tapping a result turns it back into an editable line. AC still clears everything.
- **Programmer mode** (Tools → Programmer): whole numbers in 64 bits, typed in hexadecimal, decimal, octal or binary, with the value shown in all four at once (tap a base to type in it; numbers already typed are converted). Keys for A–F, AND, OR, XOR, NOT, `<<`, `>>`, mod, brackets and ±. Arithmetic wraps around at 64 bits and negative numbers show as two's complement in hex, octal and binary, as on a programmer's calculator; division rounds towards zero; a shift must be 0–63 bits.
- **Tip and split** (Tools): a tip in percent on top of a bill, split between people. Each share is rounded up to the cent so together you always cover the bill, and it shows what that adds up to when it's a cent or two more.
- **Date difference** (Tools): pick two dates to see the days between them, the same in weeks and days, in years, months and days, and the number of workdays (Monday to Friday; public holidays aren't taken off). Counts from the first date up to, but not including, the second.
- **Loan table** (Tools): the fixed monthly payment for a loan at a yearly interest rate, the total and the interest, and a month-by-month table with the interest, the principal and what's left. It works in cents like a bank statement: the payment and every month's interest are rounded to the cent and the last payment is adjusted so the balance ends at exactly zero. The first 24 months show; **Show all** lists every month (up to 600).
- **Currency** (Tools): convert an amount with an exchange rate you type yourself (SumCtrl is offline and never looks rates up). Pick the two currencies, type how many of the second one 1 of the first is worth, and see the amount converted and the rate both ways. **Swap currencies** turns the rate round. The currencies and the rate are kept for next time.
- **Notes on history entries and CSV export.** In History, the note button on a sum lets you add a short note (what it was for); it shows under the result. The new export button saves the whole history as a CSV file (time, sum, result, note; oldest first; numbers with a decimal point and no grouping) in a place you pick, for a spreadsheet. That file is an ordinary file wherever you save it, not encrypted. Text in a cell that a spreadsheet would run as a formula gets a `'` in front.

- **Settings export and import.** **Settings → Export and import** saves your settings as a JSON file in a place you pick (angles, scientific keys, key vibration, save history, decimal separator, grouping, currency, the VAT rate to start with, the currency tool's currencies and rate, and Hide in screenshots / Hide in recent apps) and reads such a file back. Importing says what it replaces and asks first. The file never holds your history, memory, named values, the app lock, its PIN or **Lock again**, and importing can only turn Hide in screenshots and Hide in recent apps on, never off. Values that don't make sense are skipped and stay as they are.

- **SumCtrl in Dutch.** When your phone is set to Dutch, SumCtrl shows everything in Dutch (informal "je"), including the "Calculate" entry in other apps' text-selection menu; in any other language it stays English. There is no language setting in the app: it follows the phone. The app name, unit symbols and key labels (DEG, SCI, AC…) stay as they are.

### Changed

- The Numbers footer in Settings no longer says money is never converted: it is, in the new Currency tool, with a rate you type.

### Project

- **Unit tests.** SumCtrl now has tests (JUnit 4.13.2, test-only, not in the app) for the new logic: cursor editing, memory and named values in sums, the programmer engine, the tip, date, loan and currency maths, reading a selected sum and the CSV export. They were run on a computer with the Kotlin compiler; CI builds the app but doesn't run them yet.

## v0.2.1 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published. It is the first release built with the new build tools (Android Gradle Plugin 9, stricter code shrinking), so update over your installed version and check that your data and settings are still there.

### Changed

- **Newer build tools and libraries; nothing changes in how SumCtrl looks or works.** SumCtrl is now built with the current stable toolchain: Android Gradle Plugin 8.11.1 → 9.5.1, Gradle 8.14.3 → 9.9.2, Kotlin 2.2.0 → 2.4.21 (with its Compose and serialization compiler plugins) and Compose BOM 2025.07.00 → 2026.09.00 (Compose 1.12.1, Material 3 1.4.0). The libraries move to the versions the newest Ctrl apps use: AndroidX Activity 1.10.1 → 1.13.0, Core 1.16.0 → 1.18.0, Lifecycle 2.9.1 → 2.10.0, Navigation 2.9.1 → 2.9.8, DataStore 1.1.7 → 1.3.0, kotlinx.serialization 1.9.0 → 1.13.0 and kotlinx.coroutines 1.10.2 → 1.11.0. No library was added, and the app is still Google-free and offline (CI checks both).
- **Compiled against the Android 17 SDK.** Compose 1.12 needs `compileSdk` 37, so that goes from 36 to 37. The Android versions SumCtrl runs on and targets are unchanged (`minSdk` 35, `targetSdk` 36), so Android treats the app exactly as before.
- **The app is shrunk by a newer R8.** Android Gradle Plugin 9 compiles Kotlin itself (the separate Kotlin Android plugin is gone from the build files) and brings the R8 version that understands Kotlin 2.4; the release APK is about 4% smaller (3.42 MB → 3.29 MB). SumCtrl's own keep rules are unchanged.
- The checksums Gradle verifies every download against (`gradle/verification-metadata.xml`) were regenerated on CI for the new versions. Built and checked by CI only; not yet run on a phone.

## Project updates (2026-11-20)

Nothing in the app changed.

### Security

- **The signing key is no longer available to the build.** Until now the release key and its passwords were present in the same GitHub Actions job that runs Gradle, so any build plugin or library could in principle have read them. The build job now has no secrets at all and produces an unsigned APK. A separate job, which checks out no code and runs no Gradle, signs it with `apksigner`, and then refuses to hand it on unless it is signed by exactly the certificate published in SECURITY.md and README.md and its contents are identical to what the build job made. Releases are signed with the same key and the same signature scheme (v2) as before, so updates install as usual. No release has been published by the new workflow yet; the next version will be the first.
- **The build checks that the published fingerprint and the enforced one are the same.** If `build.yml`, SECURITY.md and README.md ever disagree about the signing certificate, the build fails.

### Project

- **Builds no longer use a saved Gradle cache.** Ctrl apps had builds fail with "Dependency verification failed ... from repository Gradle Central Plugin Repository", and keep failing until the caches were deleted by hand. The cause, reproduced on DateCtrl: with a restored cache Gradle still fetches the plugin files from Maven Central on every build; when Maven Central can't be reached for a moment, Gradle takes the Kotlin plugin "marker" file from the Gradle plugin portal instead, the portal serves a different file under the same name, verification rightly refuses it, and the failed run then saved that state as the cache for every later run. Now every build and the weekly code scan start from a clean Gradle home, and the plugin portal is no longer a plugin source at all (`settings.gradle.kts`): everything the build needs is on Maven Central and Google's Maven. Dependency verification itself is unchanged and as strict as before.

## v0.2.0 (2026-11-20)

**This release encrypts some of what the app stores on the phone. An older version cannot read it afterwards, so export or back up anything you need before you update (details below).** Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Security

- **Your history is now encrypted on the phone.** The file that holds your finished sums (`history.json` in SumCtrl's private storage) is sealed with AES-256-GCM under a random 256-bit data key. That key is stored next to it in `data.key`, itself encrypted by a key in the phone's secure hardware (Android Keystore, StrongBox when the phone has it) that can't be exported. This protects the history file when it is copied off the phone (a backup image, a forensic copy, a rooted file manager); it does not protect against something already running inside SumCtrl while the app is open. The key needs no fingerprint or PIN and is independent of the app lock.
- **The move is automatic and can't lose your history.** The first time this version starts, the existing history is read as before. Its encrypted copy is then written to a temporary file, synced to disk, read back, decrypted and compared byte for byte; only when it matches does it take the old file's place, in one step. If anything goes wrong the old file simply stays in use and SumCtrl tries again at the next start. Both kinds of file are read from now on.
- **If the encrypted history can't be opened** (the key is missing from the phone's secure storage, or the file is damaged), it is not deleted or overwritten: it is moved aside as `history.json.unreadable`, a new history starts, and the empty History sheet says so. If the key works again at a later start, the sums from the file kept aside are put back by themselves. **Clear history** (and "Forgot PIN?") deletes a history that was kept aside too. If the phone's secure hardware can't provide a key at all, SumCtrl keeps saving an ordinary, unencrypted history file exactly as before.
- **Not encrypted:** the settings (decimal separator, DEG/RAD, VAT rate and so on) hold nothing about what you calculated and stay an ordinary settings file. The app lock's own storage is unchanged.
- **No going back to an older version with your history.** SumCtrl 0.1.5 and older can't read the encrypted file: they show an empty history and replace the file the next time you finish a sum. SumCtrl has no export for its history, so stay on this version or newer if you want to keep it.

## v0.1.5 (2026-11-20)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Changed

- **Smoother keypad.** Pressing a key no longer rebuilds and redraws all the keys; only the sum and its result at the top are updated. The keys, the vibration setting and History behave exactly as before.
- **Opens with your settings already in place.** SumCtrl now waits a few milliseconds (half a second at most) for your saved settings before it draws its first screen, so the scientific keys, DEG/RAD and the decimal separator no longer appear in their default state for a moment and then change.
- **Found as the phone's calculator.** SumCtrl now tells Android it is a calculator app, so launchers and assistants that look for "the calculator" can find it. Nothing changes in the app itself.

### Privacy

- **"Hide in screenshots" is on from the very first frame.** The window now starts hidden from screenshots and screen recordings, and that is lifted only once your saved settings say the switch is off. Before, the protection was switched on a few frames after the app opened.

## v0.1.4 (2026-11-13)

A small security and privacy update. The calculator, converter and work tools work as before.

### Privacy

- **No debug logging in the app.** Release builds now strip debug, verbose and info log messages from the app and the libraries it uses, so nothing about what you calculate can end up in the phone's system log.

### Security

- **Checked build tools.** The build now checks the Gradle download against its published checksum, so a tampered build tool can't slip into a release.

## v0.1.3 (2026-11-12)

### Changed

- **Slightly smaller download.** The bundled JetBrains Mono font (numbers, readouts and labels) no longer carries the Cyrillic, Vietnamese and maths-alphabet letters SumCtrl never shows itself, which takes about 0.1 MB off the download and 0.2 MB off the installed app. Everything looks the same; if a name or a log line shown in that font uses one of those scripts, those letters are now drawn with your phone's own font.

## v0.1.2 (2026-11-05)

### Changed

- **Needs Android 15 or newer.** Android 13 no longer gets security updates and Android 14's updates end in early 2027, so SumCtrl now supports Android 15, 16 and 17 only. On older phones the version you have keeps working, but it won't get updates.

## v0.1.1 (2026-11-05)

### Security

- **No overlays over the lock screen.** While the lock screen, a PIN screen (turning the lock on or off, changing the PIN), "Forgot PIN?" or the clear-history confirmation is showing, other apps' floating windows (chat heads, screen overlays) are hidden over it, so no other app can cover it or put a fake screen on top. They come back as soon as that screen closes, and work as usual everywhere else. This uses a new install-time permission, "Hide other apps' floating windows"; there's no prompt.
- **PIN screens hidden from accessibility misuse.** On Android 14 and newer, the lock and PIN screens are marked as sensitive, so apps that misuse accessibility access can't read them. Real accessibility tools such as TalkBack keep working.
- **Stricter intents from other apps.** On Android 16 and newer, other apps can only open SumCtrl with an intent that really matches it. Opening SumCtrl from your launcher works as before.

### Project

- **Checked dependencies:** every library and build plugin download is now checked against sha256 checksums in `gradle/verification-metadata.xml`, and the build fails on any mismatch.
- **Verifiable releases:** this is the first release with a signed build provenance attestation and the APK's SHA-256 in the release notes. The README explains how to check a download with `gh attestation verify`.

## Project updates (2026-11-05, workflows)

No new app version: these changes are to the repo's build workflows only.

### Project

- **Signed build provenance:** every release from now on gets a GitHub build attestation, signed through Sigstore, which proves the APK was built by this repo's workflow from the tagged commit. You can check it with `gh attestation verify`.
- **Checksum in the release notes:** the release notes end with the APK's SHA-256, so you can check a download before installing it.
- **Least privilege:** the workflows get no permissions by default; each job gets only what it needs, and only the release job can write to the repo.
- **Workflow security check:** a new zizmor check runs on every change to the workflows, offline, with zizmor pinned by its checksum.
- **Code scanning:** CodeQL analyses the app's code every week and on every app change; results go to the repo's Security tab.
- **Dependency checksums:** a new manually started workflow writes `gradle/verification-metadata.xml`, the list of sha256 checksums every library and build plugin download is checked against.

## Project updates (2026-11-05)

No new app version: these changes are to the repo, its build checks and its documentation only.

### Project

- **README:** the permissions table also lists the app-internal permission AndroidX adds (it only lets SumCtrl talk to itself and isn't something you grant), and the Ctrl series list has all 28 apps, including CallCtrl, ClockCtrl, ContactCtrl, DateCtrl, LijnCtrl, MailCtrl, RecorderCtrl and TextCtrl.
- **Build:** every build now also checks that the Gradle wrapper in the repo hasn't been tampered with, before anything is compiled.
- **Repo:** `.gitignore` also keeps `google-services.json` out of the repo, next to the signing files it already covered.

## v0.1.0 (2026-11-03)

The first release of SumCtrl: an offline calculator, unit converter and work-tools app in the Ctrl series, made to replace your phone's calculator.

### New

- **Calculator with exact decimal maths.** Adding, subtracting, multiplying and dividing use exact decimals (34 significant digits), so 0.1 + 0.2 is 0.3 and money sums don't pick up rounding noise. Results show up to 12 significant digits, switching to scientific notation (like 1,5E20) for very big or very small numbers.
- **Live result while you type.** The answer shows under your sum as soon as it's complete. Multiplication and division go before addition and subtraction, −2² is −4 like on paper, and missing closing brackets at the end are added for you.
- **Shop-style percentages.** `200 + 21%` gives 242 and `200 − 10%` gives 180; `200 × 10%` gives 20 and `50%` on its own is 0,5.
- **Multiplication without a sign:** `2π`, `3(4+1)`, `(1+2)(3+4)` and `2sin(30)` work as you'd write them.
- **One bracket key** that opens or closes a bracket depending on what comes before it, a **±** key that flips the sign of the number you're typing, and backspace on the display (long-press it to clear everything). **AC** clears the line.
- **After =**, typing a number starts a new sum, an operator carries on from the result, and a function such as √ takes the result.
- **Scientific keys**, folded away until you tap **SCI**: sin, cos and tan and their inverses (**INV**), ln, log, eˣ, 10ˣ, √, x², x³, xʸ, factorial (!), π, e and Ans. **DEG/RAD** switches the angle unit; sin(180°) is exactly 0 and log(1000) exactly 3. In landscape the scientific keys sit next to the keypad.
- **Clear error messages** instead of a wrong answer: "Can't divide by zero", "No answer for that" (like √ of a negative number or tan 90°), "Number too big" and "Not a complete sum".
- **History.** Every sum you finish with = is saved on your phone, newest first, with the sum, its result and when you did it. Tap one to use its result again, remove one with ✕, or clear them all. History can be turned off in Settings.
- **Copy a result** by long-pressing it, with your decimal separator and without grouping, ready to paste.
- **Unit converter** with 13 categories: length, weight, volume (including US and UK gallons, cups and spoons), temperature, area, speed, data (kB/MB/GB/TB and KiB/MiB/GiB/TiB), time, pressure (bar, psi, kPa...), energy (kcal, kWh...), power (kW, PS, hp), fuel use (L/100 km, km/L, mpg US and UK) and angles. Factors are exact where a definition exists. Every unit of the category is listed under the result; tap one to convert to it, and use the swap button to turn the result into the new input.
- **VAT tool:** add VAT to a price or take it out of one, at 21 %, 12 %, 6 %, 0 % or your own rate, showing the price without VAT, the VAT and the price with VAT.
- **Discount tool:** the sale price and what you save, with an optional extra discount on top of the reduced price and the total discount it adds up to (20 % + 10 % is 28 %).
- **Margin and markup tool:** profit, margin and markup from cost and selling price, or the selling price you need for a target margin or markup.
- **Instalments tool:** the monthly payment, amount financed, total paid and interest for paying in parts, with an optional down payment and yearly interest rate (0 % splits the amount evenly).
- **Percentages tool:** X % of Y, X is what % of Y, and the percentage change from one number to another.
- **Settings** (tap your profile picture): save history on or off, clear history, vibrate on key press, degrees or radians, decimal comma or point (or follow the phone), thousands grouping, the currency shown in the work tools, and the VAT rate the VAT tool starts with.
- **App lock** with its own PIN and optional fingerprint unlock, the same lock as every Ctrl app, plus **Hide in screenshots** and **Hide in recent apps** (both off by default).
- **Ctrl / Signal look** in dark and light, following your phone's dark theme setting, with a new plus-and-equals keycap icon.

### Security

- **Offline and private.** SumCtrl has no internet permission and no other permissions apart from the optional fingerprint unlock. Your history and settings are left out of Android's cloud backup and phone-to-phone transfer. No telemetry, no Google libraries; every build checks both.
