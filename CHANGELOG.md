# Changelog

What changed in each SumCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/SumCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

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
