# Changelog

What changed in each SumCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/SumCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

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
