# SumCtrl

**An offline calculator, unit converter and work-tools app that replaces your phone's calculator.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/SumCtrl?label=download)](https://github.com/VoidedKN0X/SumCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) ![Offline](https://img.shields.io/badge/internet-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

SumCtrl is a calculator with exact decimal maths, a scientific keypad, history, a unit converter and tools for everyday shop sums: VAT, discounts, margins, instalments and percentages. It never goes online.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/SumCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Works fully offline.** SumCtrl doesn't have the internet permission and never goes online. Everything stays on your phone
- **Exact decimal maths:** 0.1 + 0.2 is 0.3, and money sums never pick up rounding noise. Results show 12 significant digits, with scientific notation for very big or very small numbers
- **Live result** under your sum while you type, proper order of operations, brackets that close themselves at the end, and multiplication without a sign (`2π`, `3(4+1)`)
- **Shop-style percentages:** `200 + 21%` is 242, `200 − 10%` is 180, `50%` is 0.5
- **Scientific keys** (folded away until you need them): sin, cos, tan and their inverses in degrees or radians, ln, log, eˣ, 10ˣ, √, x², x³, xʸ, factorial, π, e and Ans
- **History** of finished sums, kept only on your phone. Tap one to use its result again; remove one or clear them all, or turn history off
- **Copy a result** by long-pressing it. Backspace on the display; long-press it to clear everything
- **Unit converter** with 13 categories: length, weight, volume, temperature, area, speed, data (kB/MB/GB and KiB/MiB/GiB), time, pressure (bar, psi), energy, power (kW, PS, hp), fuel use (L/100 km, km/L, mpg) and angles, with every unit of the category listed at once
- **Work tools:** VAT (add or remove 21 %, 12 %, 6 % or your own rate), discounts with an extra discount on top, margin and markup, instalments with interest, and percentages (X % of Y, what percent, percentage change)
- **Numbers your way:** decimal comma or point (or follow your phone), thousands grouping, and your currency for the work tools. Number fields take a comma or a period either way
- **App lock** with its own PIN and optional fingerprint unlock, and **Hide in screenshots** / **Hide in recent apps** (both off until you turn them on)
- **Ctrl / Signal look**, shared by every Ctrl app: dark and light, following your phone's dark theme setting, high contrast, one violet accent, Space Grotesk and JetBrains Mono; landscape, tablet and foldable layouts

## Requirements

- Android 13 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- Permissions: biometrics, only for the optional fingerprint unlock. **No internet** and no other permissions
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google libraries, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Open the [latest release](https://github.com/VoidedKN0X/SumCtrl/releases/latest) on your phone, scroll down to **Assets** and tap the file that ends in `.apk` (for example `SumCtrl-v0.1.0.apk`). The "Source code" zip and tar.gz files are not the app; you don't need them.
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your history and settings are kept.

**Automatic updates:** on your phone, tap one of the buttons above.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds SumCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/SumCtrl` in Obtainium by hand.

**Make it your calculator:** SumCtrl can sit next to your phone's own calculator, or replace it on your home screen and in the app drawer. To hide the old one, long-press it and choose **Uninstall** or **Disable** (some phones only allow disabling).

## Getting started

- **Calculator:** type your sum; the result shows underneath as you go. Tap **=** to finish it and save it to **History** (the clock button at the top). Tap **SCI** for the scientific keys and **DEG/RAD** to switch angle units.
- **Convert:** pick a category, type a value and pick the two units. The swap button turns the result into the new input.
- **Tools:** pick a tool and fill in the fields; results update as you type.

Settings and About are behind your profile picture at the top right of every tab.

## Is it safe to install?

- **Open source.** All of SumCtrl's code is in this repo, under the GPL-3.0 licence. Anyone can read it.
- **Built by GitHub, not on a personal computer.** Every release on the Releases page is built by [the build workflow](.github/workflows/build.yml) on GitHub Actions, straight from the code in this repo. Each build also checks that the app has no network permission and no Google libraries.
- **Always signed with the same key.** Android refuses to install an update signed with a different key over SumCtrl, and the CtrlF-Droid repo only accepts APKs signed with this key. The SHA-256 fingerprint of SumCtrl's signing certificate is:

  ```
  A8:E6:37:6F:46:1C:99:29:FC:AF:A0:30:01:4A:A8:DE:63:36:02:36:B9:7E:55:05:DE:40:78:98:90:54:A8:09
  ```

  Advanced users can check it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).

**Permissions.** SumCtrl has **no internet permission**: it can't go online at all. It asks for only this:

| Permission | What SumCtrl uses it for |
| --- | --- |
| Use biometric hardware | Optional fingerprint unlock for the app lock. |

No camera, storage, location, contacts or notification permission.

See [Privacy](#privacy) for what happens to your data.

## App lock

Turn it on under **Settings > Privacy and security > App lock** (tap your profile picture at the top right, then **Settings**). It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, checked by the phone's secure hardware. The PIN is still needed after the phone restarts and at least every 72 hours.
- **The PIN is never stored.** It's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts.
- **Forgot your PIN?** It can't be recovered. The lock screen offers to delete your history and settings and turn the lock off.

**Hide in screenshots** and **Hide in recent apps** (both off by default): the first keeps SumCtrl out of screenshots and screen recordings, the second shows a blank card in the recent-apps view while screenshots still work. With the app lock on, the recent-apps view always shows a blank card.

## Privacy

**Telemetry-free.** SumCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It has **no internet permission**, works fully offline and never connects to the internet. Every build checks that the app has no network permission and no Google libraries.

- Your history and settings stay on your phone. SumCtrl is left out of Android's cloud backup and its phone-to-phone transfer, so nothing leaves the phone.
- Copying a result puts it on Android's clipboard, only when you long-press it.
- The Support, Report a bug, Source code and Licence links on the You page open in your browser, only when you tap them.

## How SumCtrl is made

SumCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/SumCtrl.git
   cd SumCtrl
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
3. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

## Support the project

SumCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, tap your profile picture: **About** at the bottom of the You page has **Support SumCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/SumCtrl/issues). Please report bugs and ideas with the [bug report and feature request forms](https://github.com/VoidedKN0X/SumCtrl/issues/new/choose). Starring the repo helps too.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [RideCtrl](https://github.com/VoidedKN0X/RideCtrl) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| **SumCtrl** (this app) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

SumCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

Fonts, bundled in the app (nothing is downloaded): [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), both under the SIL Open Font License 1.1 ([FONT-LICENSE-SpaceGrotesk.txt](FONT-LICENSE-SpaceGrotesk.txt), [FONT-LICENSE-JetBrainsMono.txt](FONT-LICENSE-JetBrainsMono.txt)).
